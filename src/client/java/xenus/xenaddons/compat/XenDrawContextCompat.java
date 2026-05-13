package xenus.xenaddons.compat;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class XenDrawContextCompat {
    private XenDrawContextCompat() {}

    public static int scaledWidth(DrawContext context) {
        return context.getScaledWindowWidth();
    }

    public static int scaledHeight(DrawContext context) {
        return context.getScaledWindowHeight();
    }

    public static void fill(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        context.fill(x1, y1, x2, y2, color);
    }

    public static void drawTextWithShadow(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color) {
        context.drawTextWithShadow(textRenderer, text, x, y, color);
    }

    public static void drawTextWithShadow(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int color) {
        context.drawTextWithShadow(textRenderer, text, x, y, color);
    }

    public static void drawTexture(DrawContext context, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public static void drawTexture(DrawContext context, Identifier texture, int x, int y, float u, float v, int width, int height, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, regionWidth, regionHeight, textureWidth, textureHeight);
    }
}
