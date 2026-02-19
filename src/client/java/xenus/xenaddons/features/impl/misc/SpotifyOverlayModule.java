package xenus.xenaddons.features.impl.misc;

import net.minecraft.client.gui.DrawContext;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.NumberSetting;
import xenus.xenaddons.utils.SpotifyNowPlaying;

public class SpotifyOverlayModule extends Module {
    private final NumberSetting hudOffsetX = new NumberSetting("HUD Offset X", 0, -1000, 1000, 1);
    private final NumberSetting hudY = new NumberSetting("HUD Y", 84, -1000, 1000, 1);
    private final NumberSetting hudScale = new NumberSetting("HUD Scale", 1.0, 0.5, 6.0, 0.05);

    public SpotifyOverlayModule() {
        super("Spotify Overlay", "Displays currently playing Spotify track", Category.MISC);
        hudOffsetX.setVisibility(() -> false);
        hudY.setVisibility(() -> false);
        hudScale.setVisibility(() -> false);
        this.registerSetting(hudOffsetX);
        this.registerSetting(hudY);
        this.registerSetting(hudScale);
    }

    public void renderInGameHud(DrawContext drawContext) {
        if (!this.isEnabled()) return;
        SpotifyNowPlaying.requestRefreshIfNeeded();
        String line = SpotifyNowPlaying.getFormattedLine();
        if (line == null || line.isBlank()) return;
        renderLine(drawContext, line);
    }

    public void renderPreview(DrawContext drawContext) {
        renderLine(drawContext, "§6Spotify §7> §aAuteur §7- §bNom musique");
    }

    private void renderLine(DrawContext drawContext, String line) {
        int width = mc.getWindow().getScaledWidth();
        float scale = (float) hudScale.getValue().doubleValue();
        float inv = 1.0f / scale;
        int leftX = (width / 2) + hudOffsetX.getValue().intValue();
        int y = hudY.getValue().intValue();

        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().scale(scale, scale);
        int x = (int) (leftX * inv);
        int drawY = (int) (y * inv);
        drawContext.drawTextWithShadow(mc.textRenderer, line, x, drawY, 0xFFFFFFFF);
        drawContext.getMatrices().popMatrix();
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
