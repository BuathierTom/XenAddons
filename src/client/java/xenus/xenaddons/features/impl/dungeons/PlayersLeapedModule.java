package xenus.xenaddons.features.impl.dungeons;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.NumberSetting;
import xenus.xenaddons.utils.DungeonUtils;

public class PlayersLeapedModule extends Module {
    private static final AABB[] REGIONS = {
            new AABB(90, 106, 50, 111, 145, 123),
            new AABB(17, 106, 121, 108, 145, 143),
            new AABB(-2, 106, 51, 20, 145, 142),
            new AABB(-1, 26, 29, 191, 145, 58),
            new AABB(3, 5, 0, 128, 48, 140)
    };

    // Match the same coordinate ranges used by DungeonUtils position definitions.
    private static final AABB EE2_BOX = new AABB(49, 108, 129, 58, 115, 133);
    private static final AABB HEE2_BOX = new AABB(59, 132, 138, 62, 135, 143);
    private static final AABB EE3_BOX = new AABB(0, 108, 98, 4, 115, 107);
    private static final AABB CORE_BOX = new AABB(52, 113, 49, 57, 117, 53);
    private static final AABB GOLDOR_TUNNEL_BOX = new AABB(41, 110, 59, 68, 150, 117);
    private static final AABB P5_BOX = new AABB(3, 5, 0, 128, 48, 140);

    private final NumberSetting hudOffsetX = new NumberSetting("HUD Offset X", 0, -1000, 1000, 1);
    private final NumberSetting hudY = new NumberSetting("HUD Y", 68, -1000, 1000, 1);
    private final NumberSetting hudScale = new NumberSetting("HUD Scale", 1.0, 0.5, 6.0, 0.05);

    private int leapedCount = 0;
    private int currentSpot = 0;
    private boolean inBounds = false;
    private boolean mageSeenAtCoreThisRun = false;
    private boolean p5HiddenAfterFullCount = false;

    public PlayersLeapedModule() {
        super("Players Leaped", "Shows how many players leaped in F7/M7 terms", Category.DUNGEON);

        hudOffsetX.setVisibility(() -> false);
        hudY.setVisibility(() -> false);
        hudScale.setVisibility(() -> false);
        this.registerSetting(hudOffsetX);
        this.registerSetting(hudY);
        this.registerSetting(hudScale);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!this.isEnabled()) {
                inBounds = false;
                return;
            }
            tick();
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("xenaddons", "players_leaped_hud"), (drawContext, tickDelta) -> {
            if (!this.isEnabled() || !shouldDisplay()) return;
            renderHud(drawContext);
        });
    }

    private void tick() {
        if (!DungeonUtils.inDungeon()) {
            inBounds = false;
            mageSeenAtCoreThisRun = false;
            p5HiddenAfterFullCount = false;
            return;
        }

        LocalPlayer player = mc.player;
        ClientLevel world = mc.level;
        if (player == null || world == null) return;

        currentSpot = getSpot(player);
        if (currentSpot < 2) {
            leapedCount = 0;
            inBounds = false;
            return;
        }
        inBounds = true;

        AABB box = getCountRegion(currentSpot);
        if (box == null) {
            leapedCount = 0;
            inBounds = false;
            return;
        }
        int currentCount = 0;
        for (Player other : world.players()) {
            if (other == null || other == player) continue;
            if (other.isSpectator()) continue;
            if (!isInBox(box, other.getBoundingBox())) continue;
            currentCount++;
        }
        leapedCount = currentCount;
        if (currentSpot == 6 && leapedCount >= 4) {
            p5HiddenAfterFullCount = true;
        }
    }

    private int getSpot(LocalPlayer player) {
        AABB playerBox = player.getBoundingBox();
        String playerClass = DungeonUtils.getPlayerClass(player);
        if (playerClass == null && player.getGameProfile() != null) {
            playerClass = DungeonUtils.getPlayerClass(player.getGameProfile().name());
        }
        if (playerClass == null) return 0;

        if ((isInBox(HEE2_BOX, playerBox) || isInBox(EE2_BOX, playerBox))
                && ("Archer".equals(playerClass) || "Mage".equals(playerClass))) return 2;
        if (isInBox(EE3_BOX, playerBox) && "Healer".equals(playerClass)) return 3;
        if (isInBox(CORE_BOX, playerBox) && "Mage".equals(playerClass)) {
            mageSeenAtCoreThisRun = true;
            return 4;
        }
        if (isInBox(GOLDOR_TUNNEL_BOX, playerBox) && !DungeonUtils.inPhase(5) && "Mage".equals(playerClass) && mageSeenAtCoreThisRun) return 5;
        if (isInBox(P5_BOX, playerBox) && "Healer".equals(playerClass)) return 6;
        return 0;
    }

    private AABB getCountRegion(int spot) {
        return switch (spot) {
            case 2 -> REGIONS[1];
            case 3 -> REGIONS[2];
            case 4 -> REGIONS[3];
            case 5, 6 -> REGIONS[4];
            default -> null;
        };
    }

    private static boolean isInBox(AABB box, AABB target) {
        return target.maxX >= Math.min(box.minX, box.maxX) && target.minX <= Math.max(box.minX, box.maxX)
            && target.maxY >= Math.min(box.minY, box.maxY) && target.minY <= Math.max(box.minY, box.maxY)
            && target.maxZ >= Math.min(box.minZ, box.maxZ) && target.minZ <= Math.max(box.minZ, box.maxZ);
    }

    private boolean shouldDisplay() {
        if (currentSpot == 6 && p5HiddenAfterFullCount) {
            return false;
        }
        return inBounds && DungeonUtils.inDungeon();
    }

    private void renderHud(GuiGraphicsExtractor drawContext) {
        String startColor;
        if (currentSpot == 3) {
            startColor = leapedCount > 2 ? "§9" : "§4";
        } else {
            startColor = leapedCount > 3 ? "§9" : "§4";
        }

        String text = startColor + leapedCount + "§9/4 Players Leaped";
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int textWidth = mc.font.width(text);
        int textHeight = mc.font.lineHeight;
        float scale = (float) hudScale.getValue().doubleValue();
        float inv = 1.0f / scale;
        int centerXRaw = (width / 2) + hudOffsetX.getValue().intValue();
        int yRaw = hudY.getValue().intValue();
        int minCenterX = (int) Math.ceil((textWidth * scale) / 2.0f) + 2;
        int maxCenterX = width - minCenterX;
        int centerX = Mth.clamp(centerXRaw, minCenterX, Math.max(minCenterX, maxCenterX));
        int maxY = height - (int) Math.ceil(textHeight * scale) - 2;
        int y = Mth.clamp(yRaw, 2, Math.max(2, maxY));

        drawContext.pose().pushMatrix();
        drawContext.pose().scale(scale, scale);
        int x = (int) ((centerX * inv) - (textWidth / 2f));
        int drawY = (int) (y * inv);
        drawContext.text(mc.font, text, x, drawY, 0xFFFFFFFF);
        drawContext.pose().popMatrix();
    }

    @Override
    public void onDisable() {
        leapedCount = 0;
        currentSpot = 0;
        inBounds = false;
        mageSeenAtCoreThisRun = false;
        p5HiddenAfterFullCount = false;
    }

    public int getHudOffsetX() {
        return hudOffsetX.getValue().intValue();
    }

    public int getHudY() {
        return hudY.getValue().intValue();
    }

    public double getHudScale() {
        return hudScale.getValue();
    }

    public void setHudOffsetX(int value) {
        hudOffsetX.setValue((double) value);
    }

    public void setHudY(int value) {
        hudY.setValue((double) value);
    }

    public void setHudScale(double value) {
        hudScale.setValue(value);
    }
}
