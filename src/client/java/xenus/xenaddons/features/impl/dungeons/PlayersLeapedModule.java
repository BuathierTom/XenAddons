package xenus.xenaddons.features.impl.dungeons;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.NumberSetting;
import xenus.xenaddons.utils.DungeonUtils;

public class PlayersLeapedModule extends Module {
    private static final Box[] REGIONS = {
            new Box(90, 106, 50, 111, 145, 123),
            new Box(17, 106, 121, 108, 145, 143),
            new Box(-2, 106, 51, 20, 145, 142),
            new Box(-1, 26, 29, 191, 145, 58),
            new Box(3, 5, 0, 128, 48, 140)
    };

    private static final Box EE2_BOX = new Box(57, 108, 130, 59, 110, 132);
    private static final Box HEE2_BOX = new Box(59, 132, 138, 62, 133, 140);
    private static final Box EE3_BOX = new Box(1, 108, 103, 3, 110, 105);
    private static final Box CORE_BOX = new Box(53.5, 114, 49.5, 55.5, 116, 51.5);
    private static final Box RELIC_BOX = new Box(51.5, 3, 73.5, 57.5, 8, 79.5);

    private final NumberSetting hudOffsetX = new NumberSetting("HUD Offset X", 0, -1000, 1000, 1);
    private final NumberSetting hudY = new NumberSetting("HUD Y", 68, -1000, 1000, 1);
    private final NumberSetting hudScale = new NumberSetting("HUD Scale", 1.0, 0.5, 6.0, 0.05);

    private int leapedCount = 0;
    private int currentSpot = 0;
    private boolean inBounds = false;

    public PlayersLeapedModule() {
        super("Players Leaped", "Shows how many players leaped in F7 terms", Category.DUNGEON);

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

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (!this.isEnabled() || !shouldDisplay()) return;
            renderHud(drawContext);
        });
    }

    private void tick() {
        if (!DungeonUtils.inDungeon() || DungeonUtils.floorNumber != 7 || DungeonUtils.currentPhase <= 0) {
            inBounds = false;
            return;
        }

        ClientPlayerEntity player = mc.player;
        ClientWorld world = mc.world;
        if (player == null || world == null) return;

        currentSpot = getSpot(player);
        if (currentSpot < 2) {
            leapedCount = 0;
            inBounds = false;
            return;
        }
        inBounds = true;

        Box box = REGIONS[currentSpot - 1];
        int currentCount = 0;
        for (Entity entity : world.getOtherEntities(player, box)) {
            if (!(entity instanceof PlayerEntity)) continue;
            if (entity.isInvisible()) continue;
            if (entity.isSpectator()) continue;
            currentCount++;
        }
        leapedCount = currentCount;
    }

    private int getSpot(ClientPlayerEntity player) {
        Vec3d pos = player.getEntityPos();
        if (HEE2_BOX.contains(pos)) return 2;
        if (EE2_BOX.contains(pos)) return 2;
        if (EE3_BOX.contains(pos)) return 3;
        if (CORE_BOX.contains(pos)) return 4;
        if (RELIC_BOX.contains(pos) && !DungeonUtils.inPhase(5)) return 5;
        return 0;
    }

    private boolean shouldDisplay() {
        return inBounds && DungeonUtils.inDungeon() && DungeonUtils.floorNumber == 7;
    }

    private void renderHud(DrawContext drawContext) {
        String startColor;
        if (currentSpot == 3) {
            startColor = leapedCount > 2 ? "§9" : "§4";
        } else {
            startColor = leapedCount > 3 ? "§9" : "§4";
        }

        String text = startColor + leapedCount + "§9/4 Players Leaped";
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();
        int textWidth = mc.textRenderer.getWidth(text);
        int textHeight = mc.textRenderer.fontHeight;
        float scale = (float) hudScale.getValue().doubleValue();
        float inv = 1.0f / scale;
        int centerXRaw = (width / 2) + hudOffsetX.getValue().intValue();
        int yRaw = hudY.getValue().intValue();
        int minCenterX = (int) Math.ceil((textWidth * scale) / 2.0f) + 2;
        int maxCenterX = width - minCenterX;
        int centerX = MathHelper.clamp(centerXRaw, minCenterX, Math.max(minCenterX, maxCenterX));
        int maxY = height - (int) Math.ceil(textHeight * scale) - 2;
        int y = MathHelper.clamp(yRaw, 2, Math.max(2, maxY));

        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().scale(scale, scale);
        int x = (int) ((centerX * inv) - (textWidth / 2f));
        int drawY = (int) (y * inv);
        drawContext.drawTextWithShadow(mc.textRenderer, text, x, drawY, 0xFFFFFFFF);
        drawContext.getMatrices().popMatrix();
    }

    @Override
    public void onDisable() {
        leapedCount = 0;
        currentSpot = 0;
        inBounds = false;
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
