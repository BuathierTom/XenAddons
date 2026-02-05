package xenus.xenaddons.utils;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.StyleSpriteSource;

public class RenderUtils {
    public static final net.minecraft.util.Identifier XEN_FONT = net.minecraft.util.Identifier.of("xenaddons", "xen");

    public static void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y + 1, x + 1, y + height - 1, color);
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }
    
    public static void drawText(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, String text, int x, int y, int color, boolean shadow) {
        net.minecraft.text.MutableText mutableText = net.minecraft.text.Text.literal(text);
        if (shadow) context.drawTextWithShadow(textRenderer, mutableText, x, y, color);
        else context.drawText(textRenderer, mutableText, x, y, color, false);
    }

    public static void drawCenteredText(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, String text, int centerX, int y, int color, boolean shadow) {
        net.minecraft.text.MutableText mutableText = net.minecraft.text.Text.literal(text);
        int width = textRenderer.getWidth(mutableText);
        if (shadow) context.drawTextWithShadow(textRenderer, mutableText, centerX - width / 2, y, color);
        else context.drawText(textRenderer, mutableText, centerX - width / 2, y, color, false);
    }
    public static int darken(int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (a << 24) | ((int)(r * 0.7) << 16) | ((int)(g * 0.7) << 8) | (int)(b * 0.7);
    }
}
