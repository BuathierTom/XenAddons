package xenus.xenaddons.features.impl.slayer;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.settings.NumberSetting;
import xenus.xenaddons.utils.slayer.SlayerTracker;
import xenus.xenaddons.utils.slayer.SlayerTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the nametag of the slayer boss you spawned on your HUD, so you can read its health without
 * looking at it. Modelled on Athen's Slayer display.
 */
public class SlayerBossHudModule extends Module {

    private static final List<Component> PREVIEW_LINES = List.of(
            Component.literal("§c02:46"),
            Component.literal("§c☠ §bRevenant Horror I §a500§c❤")
    );

    private final BooleanSetting showTimer = new BooleanSetting("Show timer", true);
    private final NumberSetting hudOffsetX = new NumberSetting("HUD Offset X", 0, -1000, 1000, 1);
    private final NumberSetting hudY = new NumberSetting("HUD Y", 60, -1000, 1000, 1);
    private final NumberSetting hudScale = new NumberSetting("HUD Scale", 1.0, 0.5, 6.0, 0.05);

    public SlayerBossHudModule() {
        super("Slayer Boss HUD", "Shows your slayer boss's nametag and health on screen", Category.SLAYER);

        this.registerSetting(showTimer);
        hudOffsetX.setVisibility(() -> false);
        hudY.setVisibility(() -> false);
        hudScale.setVisibility(() -> false);
        this.registerSetting(hudOffsetX);
        this.registerSetting(hudY);
        this.registerSetting(hudScale);

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("xenaddons", "slayer_boss_hud"), (drawContext, tickDelta) -> {
            if (!this.isEnabled()) return;
            renderLines(drawContext, getDisplayLines());
        });
    }

    public static boolean isActive() {
        SlayerBossHudModule module = ModuleManager.getModule(SlayerBossHudModule.class);
        return module != null && module.isEnabled();
    }

    private List<Component> getDisplayLines() {
        List<Component> lines = SlayerTracker.getDisplayLines();
        if (showTimer.getValue()) return lines;

        // Without the timer only the "☠ Name Tier 500❤" line is worth keeping.
        List<Component> filtered = new ArrayList<>(1);
        for (Component line : lines) {
            if (SlayerTypes.containsBossName(SlayerTypes.stripFormatting(line.getString()))) filtered.add(line);
        }
        return filtered;
    }

    private void renderLines(GuiGraphicsExtractor drawContext, List<Component> lines) {
        if (lines.isEmpty()) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        float scale = (float) hudScale.getValue().doubleValue();
        float inv = 1.0f / scale;
        int lineHeight = mc.font.lineHeight + 1;

        int widest = 0;
        for (Component line : lines) widest = Math.max(widest, mc.font.width(line));

        int minCenterX = (int) Math.ceil((widest * scale) / 2.0f) + 2;
        int maxCenterX = screenWidth - minCenterX;
        int centerX = Mth.clamp((screenWidth / 2) + hudOffsetX.getValue().intValue(), minCenterX, Math.max(minCenterX, maxCenterX));

        int blockHeight = (int) Math.ceil(lineHeight * lines.size() * scale);
        int y = Mth.clamp(hudY.getValue().intValue(), 2, Math.max(2, screenHeight - blockHeight - 2));

        drawContext.pose().pushMatrix();
        drawContext.pose().scale(scale, scale);
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            int x = (int) ((centerX * inv) - (mc.font.width(line) / 2f));
            int drawY = (int) (y * inv) + (i * lineHeight);
            drawContext.text(mc.font, line, x, drawY, 0xFFFFFFFF, true);
        }
        drawContext.pose().popMatrix();
    }

    public void renderPreview(GuiGraphicsExtractor drawContext) {
        renderLines(drawContext, getPreviewLines());
    }

    public List<Component> getPreviewLines() {
        if (showTimer.getValue()) return PREVIEW_LINES;
        return List.of(PREVIEW_LINES.get(1));
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
