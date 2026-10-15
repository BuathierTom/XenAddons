package xenus.xenaddons.features.impl.dungeons;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.ActionSetting;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.settings.ModeSetting;
import xenus.xenaddons.settings.StringSetting;
import xenus.xenaddons.hud.HudElement;
import xenus.xenaddons.hud.HudRender;
import xenus.xenaddons.utils.DungeonUtils;

import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

public class ReminderGateModule extends Module {
    private static final Pattern COLOR_PATTERN = Pattern.compile("§.");
    private static final Pattern LEVER_PATTERN = Pattern.compile("^.+ (?:activated|completed) a lever! \\(\\d+/\\d+\\)$");
    private static final int NOTIFICATION_DURATION_TICKS = 20 * 5;
    private static final double P3_MIN_Y = 100.0;
    private static final double P3_MAX_Y = 155.0;
    private static final AABB[] P3_SECTIONS = {
            new AABB(90, 158, 123, 111, 105, 32),
            new AABB(16, 158, 122, 111, 105, 143),
            new AABB(19, 158, 48, -3, 106, 142),
            new AABB(91, 158, 50, -3, 106, 30)
    };
    private static final BlockPos[] GATES = {
            new BlockPos(103, 134, 123),
            new BlockPos(17, 134, 135),
            new BlockPos(5, 134, 49)
    };
    private static final Identifier[] RANDOM_SOUND_POOL = new Identifier[]{
            Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.fah"),
            Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.nuke_bomb"),
            Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.android_meme"),
            Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.pump_shotgun"),
            Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.knocked"),
            Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.hog_rider"),
            Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.airhorn")
    };

    private final StringSetting displayText = new StringSetting("Display Component", "GATEEEEEE !!!!!!!", 64);
    private final BooleanSetting enableSound = new BooleanSetting("Enable Sound", true);
    private final ModeSetting sound = new ModeSetting(
            "Sound",
            "FAHHHHHHHHHHHHHH",
            "Random",
            "FAHHHHHHHHHHHHHH",
            "Nuke Bomb",
            "Android Meme",
            "Pump Shotgun",
            "Knocked",
            "Hog Rider",
            "Airhorn"
    );
    private final ActionSetting playSound = new ActionSetting("Play Sound", this::playSelectedSoundPreview);

    private ClientLevel lastLevel;
    private int remindedSection = 0;
    private int notificationSection = 0;
    private int notificationTicks = 0;
    private int debugDisplayTicks = 0;

    public ReminderGateModule() {
        super("Reminder Gate", "Shows a gate reminder when lever phases start", Category.DUNGEON);

        this.registerSetting(displayText);
        this.registerSetting(enableSound);
        this.registerSetting(sound);
        this.registerSetting(playSound);

        ClientReceiveMessageEvents.GAME.register(this::onGameMessage);
        ClientReceiveMessageEvents.GAME_CANCELED.register(this::onGameMessage);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!this.isEnabled()) return;
            if (debugDisplayTicks > 0) debugDisplayTicks--;
            tick();
        });

        hudElement("Reminder Gate", (context, example) -> HudRender.centeredText(context, getDisplayText(), 0xFFFF5555))
            .visibleWhen(() -> debugDisplayTicks > 0 || notificationTicks > 0)
            .centered()
            .defaults(HudElement.CENTER, 84.0f, 5.0f);
    }

    private void onGameMessage(Component message, boolean overlay) {
        if (overlay || !this.isEnabled()) return;

        String clean = COLOR_PATTERN.matcher(message.getString()).replaceAll("").strip();
        if (!LEVER_PATTERN.matcher(clean).matches()) return;

        int section = currentSection();
        if (section == 0 || section == remindedSection || !isGateStanding(section)) return;

        remindedSection = section;
        notificationSection = section;
        notificationTicks = NOTIFICATION_DURATION_TICKS;
        playSelectedSound();
    }

    private void tick() {
        if (mc.level != lastLevel) {
            lastLevel = mc.level;
            clearDungeonRuntime();
        }

        if (notificationTicks <= 0) return;
        if (currentSection() != notificationSection || !isGateStanding(notificationSection)) {
            notificationTicks = 0;
            return;
        }
        notificationTicks--;
    }

    private int currentSection() {
        LocalPlayer player = mc.player;
        if (player == null || !DungeonUtils.inDungeon() || DungeonUtils.floorNumber != 7 || !DungeonUtils.inBoss()) return 0;

        double y = player.getY();
        if (y <= P3_MIN_Y || y > P3_MAX_Y) return 0;

        for (int i = 0; i < P3_SECTIONS.length; i++) {
            if (P3_SECTIONS[i].contains(player.getX(), y, player.getZ())) return i + 1;
        }
        return 0;
    }

    private boolean isGateStanding(int section) {
        if (mc.level == null || section < 1 || section > GATES.length) return false;

        Block block = mc.level.getBlockState(GATES[section - 1]).getBlock();
        return block == Blocks.CRACKED_STONE_BRICKS || block == Blocks.INFESTED_STONE_BRICKS;
    }

    private String getDisplayText() {
        String text = displayText.getValue();
        return (text == null || text.isBlank()) ? "GATEEEEEE !!!!!!!" : text;
    }

    private void playSelectedSoundPreview() {
        playSelectedSound();
    }

    public void debugTrigger() {
        debugDisplayTicks = 80;
        playSelectedSound();
    }

    private void playSelectedSound() {
        if (!enableSound.getValue()) return;
        if (mc.player == null || mc.level == null) return;
        Identifier id = resolveSoundId(sound.getValue());
        SoundEvent event = SoundEvent.createVariableRangeEvent(id);
        float volume = 8.0f;
        float pitch = 1.0f;
        mc.player.playSound(event, volume, pitch);
    }

    private Identifier resolveSoundId(String selected) {
        return switch (selected) {
            case "Random" -> {
                int idx = ThreadLocalRandom.current().nextInt(RANDOM_SOUND_POOL.length);
                yield RANDOM_SOUND_POOL[idx];
            }
            case "Nuke Bomb" -> Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.nuke_bomb");
            case "Android Meme" -> Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.android_meme");
            case "Pump Shotgun" -> Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.pump_shotgun");
            case "Knocked" -> Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.knocked");
            case "Hog Rider" -> Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.hog_rider");
            case "Airhorn" -> Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.airhorn");
            case "FAHHHHHHHHHHHHHH" -> Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.fah");
            default -> {
                Identifier parsed = Identifier.tryParse(selected);
                yield parsed != null ? parsed : Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.fah");
            }
        };
    }

    private void clearDungeonRuntime() {
        remindedSection = 0;
        notificationSection = 0;
        notificationTicks = 0;
    }

    @Override
    public void onDisable() {
        clearDungeonRuntime();
        debugDisplayTicks = 0;
    }
}
