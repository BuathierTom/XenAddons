package xenus.xenaddons.features.impl.dungeons;

import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.hud.HudElement;
import xenus.xenaddons.hud.HudRender;
import xenus.xenaddons.settings.ActionSetting;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.settings.ModeSetting;
import xenus.xenaddons.settings.NumberSetting;
import xenus.xenaddons.settings.StringSetting;
import xenus.xenaddons.utils.AlertSounds;
import xenus.xenaddons.utils.DungeonUtils;

public class PlayersLeapedModule extends Module {
    private static final String PREVIEW_TEXT = "§42§9/4 Players Leaped";
    private static final String DEFAULT_COMPLETE_TEXT = "&aEveryone Leaped!";
    private static final int COMPLETE_DELAY_TICKS = 20;
    private static final int TITLE_TICKS = 40;
    private static final AABB[] P3_SECTIONS = {
        new AABB(90, 158, 123, 111, 105, 32),
        new AABB(16, 158, 122, 111, 105, 143),
        new AABB(19, 158, 48, -3, 106, 142),
        new AABB(91, 158, 50, -3, 106, 30)
    };
    private static final AABB CORE_AREA = new AABB(68, 106, 54, 42, 155, 119);

    private final BooleanSetting alertComplete = new BooleanSetting("Alert Complete", true);
    private final StringSetting completeText = new StringSetting("Complete Text", DEFAULT_COMPLETE_TEXT, 64);
    private final ModeSetting completeSound = new ModeSetting("Complete Sound", AlertSounds.DEFAULT, AlertSounds.NAMES);
    private final NumberSetting soundVolume = new NumberSetting("Sound Volume", 1.0, 0.0, 5.0, 0.1);
    private final NumberSetting soundPitch = new NumberSetting("Sound Pitch", 1.0, 0.0, 2.0, 0.1);
    private final ActionSetting playSound = new ActionSetting("Play Sound", this::playCompleteSound);

    private ClientLevel lastLevel;
    private Region currentRegion;
    private int currentMax;
    private int tickCount;

    public PlayersLeapedModule() {
        super("Players Leaped", "Shows how many players leaped in F7/M7 terms", Category.DUNGEON);

        completeText.setVisibility(alertComplete::getValue);
        completeSound.setVisibility(alertComplete::getValue);
        soundVolume.setVisibility(alertComplete::getValue);
        soundPitch.setVisibility(alertComplete::getValue);
        playSound.setVisibility(alertComplete::getValue);
        this.registerSetting(alertComplete);
        this.registerSetting(completeText);
        this.registerSetting(completeSound);
        this.registerSetting(soundVolume);
        this.registerSetting(soundPitch);
        this.registerSetting(playSound);

        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());

        hudElement("Players Leaped", (context, example) -> {
            String text = example ? PREVIEW_TEXT : getHudText();
            return text == null ? HudElement.Size.NONE : HudRender.centeredText(context, text, 0xFFFFFFFF);
        })
            .visibleWhen(() -> currentRegion != null)
            .centered()
            .defaults(HudElement.CENTER, 68.0f, 1.0f);
    }

    private void tick() {
        ClientLevel level = mc.level;
        if (level != lastLevel) {
            lastLevel = level;
            resetRegions();
        }

        tickCount++;
        for (Region region : Region.values()) {
            if (region.completeAtTick >= 0 && tickCount >= region.completeAtTick) {
                region.completed = true;
                region.completeAtTick = -1;
            }
        }

        currentRegion = null;
        LocalPlayer player = mc.player;
        if (!this.isEnabled() || level == null || player == null || !inFloor7Boss()) return;

        Region region = findRegion(player);
        if (region == null) return;

        currentRegion = region;
        currentMax = Math.min(Math.max(0, DungeonUtils.getDungeonPartyCountFromTab() - 1), region.maxTeammates);
        if (currentMax <= 0) return;

        for (Player other : level.players()) {
            if (other == player || other.isSpectator()) continue;
            if (DungeonUtils.getPlayerClass(other) == null) continue;
            if (region.leaped.contains(other.getId())) continue;
            if (!region.check.test(other.getX(), other.getY(), other.getZ())) continue;

            region.leaped.add(other.getId());
            if (region.leaped.size() == currentMax && alertComplete.getValue()) {
                showComplete();
            }
            if (region.leaped.size() >= currentMax && region.completeAtTick < 0) {
                region.completeAtTick = tickCount + COMPLETE_DELAY_TICKS;
            }
        }
    }

    private static boolean inFloor7Boss() {
        return DungeonUtils.floorNumber == 7 && DungeonUtils.inBoss();
    }

    private static Region findRegion(LocalPlayer player) {
        for (Region region : Region.values()) {
            if (!region.completed && region.box.contains(player.getX(), player.getY(), player.getZ())) {
                return region;
            }
        }
        return null;
    }

    private String getHudText() {
        Region region = currentRegion;
        if (region == null || currentMax <= 0) return null;

        int count = region.leaped.size();
        String startColor = currentMax - count <= 1 ? "§9" : "§4";
        return startColor + count + "§9/" + currentMax + " Players Leaped";
    }

    private void showComplete() {
        String text = completeText.getValue();
        if (text == null || text.isBlank()) text = DEFAULT_COMPLETE_TEXT;
        mc.gui.setTitle(Component.literal(text.replace('&', '§')));
        mc.gui.setTimes(0, TITLE_TICKS, 0);
        playCompleteSound();
    }

    private void playCompleteSound() {
        AlertSounds.play(completeSound.getValue(), soundVolume.getValue(), soundPitch.getValue());
    }

    private void resetRegions() {
        for (Region region : Region.values()) {
            region.leaped.clear();
            region.completed = false;
            region.completeAtTick = -1;
        }
        currentRegion = null;
        currentMax = 0;
    }

    private static int phase(double y) {
        if (y > 210) return 1;
        if (y > 155) return 2;
        if (y > 100) return 3;
        if (y > 45) return 4;
        return 5;
    }

    private static int p3Section(double x, double y, double z) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || phase(player.getY()) != 3) return 0;

        for (int i = 0; i < P3_SECTIONS.length; i++) {
            if (P3_SECTIONS[i].contains(x, y, z)) return i + 1;
        }
        return 0;
    }

    @Override
    public void onDisable() {
        currentRegion = null;
        currentMax = 0;
    }

    @FunctionalInterface
    private interface PositionCheck {
        boolean test(double x, double y, double z);
    }

    private enum Region {
        SS(new AABB(106, 119, 92, 109, 121, 96), 3, (x, y, z) -> p3Section(x, y, z) == 1),
        EE2(new AABB(57, 108, 130, 59, 110, 132), 4, (x, y, z) -> p3Section(x, y, z) == 2),
        HEE2(new AABB(57, 132, 138, 62, 133, 140), 4, (x, y, z) -> p3Section(x, y, z) == 2),
        EE3(new AABB(1, 108, 101, 3, 110, 107), 3, (x, y, z) -> p3Section(x, y, z) == 3),
        CORE(new AABB(51, 114, 54, 58, 117, 49), 4, (x, y, z) -> p3Section(x, y, z) == 4),
        IN_CORE(new AABB(51, 114, 54, 58, 117, 49).move(0, 0, 6), 4, (x, y, z) -> CORE_AREA.contains(x, y, z)),
        RELIC(new AABB(46.5, 3, 68.5, 62.5, 8, 84.5), 4, (x, y, z) -> phase(y) == 5);

        private final AABB box;
        private final int maxTeammates;
        private final PositionCheck check;
        private final Set<Integer> leaped = new HashSet<>();
        private boolean completed;
        private int completeAtTick = -1;

        Region(AABB box, int maxTeammates, PositionCheck check) {
            this.box = box;
            this.maxTeammates = maxTeammates;
            this.check = check;
        }
    }
}
