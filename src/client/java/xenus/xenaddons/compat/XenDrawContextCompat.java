package xenus.xenaddons.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class XenDrawContextCompat {
    private XenDrawContextCompat() {}

    public static int scaledWidth(GuiGraphicsExtractor context) {
        return context.guiWidth();
    }

    public static int scaledHeight(GuiGraphicsExtractor context) {
        return context.guiHeight();
    }

    public static void fill(GuiGraphicsExtractor context, int x1, int y1, int x2, int y2, int color) {
        context.fill(x1, y1, x2, y2, color);
    }

    public static void drawTextWithShadow(GuiGraphicsExtractor context, Font textRenderer, String text, int x, int y, int color) {
        context.text(textRenderer, text, x, y, color, true);
    }

    public static void drawTextWithShadow(GuiGraphicsExtractor context, Font textRenderer, Component text, int x, int y, int color) {
        context.text(textRenderer, text, x, y, color, true);
    }

    public static void text(GuiGraphicsExtractor context, Font textRenderer, String text, int x, int y, int color) {
        drawTextWithShadow(context, textRenderer, text, x, y, color);
    }

    public static void text(GuiGraphicsExtractor context, Font textRenderer, Component text, int x, int y, int color) {
        drawTextWithShadow(context, textRenderer, text, x, y, color);
    }

    public static void drawTexture(GuiGraphicsExtractor context, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        context.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public static void drawTexture(GuiGraphicsExtractor context, Identifier texture, int x, int y, float u, float v, int width, int height, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        context.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, regionWidth, regionHeight, textureWidth, textureHeight);
    }

    public static void item(GuiGraphicsExtractor context, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        context.item(stack, x, y);
    }
}
