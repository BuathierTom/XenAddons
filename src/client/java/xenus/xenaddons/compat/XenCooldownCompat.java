package xenus.xenaddons.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import xenus.xenaddons.features.impl.misc.CooldownItemsTracker;
import xenus.xenaddons.features.impl.misc.ExperimentationTableSolverTracker;

public final class XenCooldownCompat {
    private XenCooldownCompat() {}

    public static ItemStack replaceSuperpairsUnknownStack(Slot slot) {
        if (slot == null) return ItemStack.EMPTY;
        ItemStack replacement = ExperimentationTableSolverTracker.getSuperpairsVisualStack(slot);
        if (replacement != null && !replacement.isEmpty()) return replacement;
        return slot.getStack();
    }

    public static void renderInventorySlot(DrawContext context, Slot slot) {
        if (slot == null) return;
        ExperimentationTableSolverTracker.renderSlotHighlight(context, slot);
        renderSuperpairsOverlay(context, slot);

        ItemStack stack = slot.getStack();
        if (stack == null || stack.isEmpty()) return;
        CooldownItemsTracker.renderBackground(context, stack, slot.x, slot.y, true);
        renderOverlayTexts(context, MinecraftClient.getInstance().textRenderer, stack, slot.x, slot.y, true);
    }

    public static void renderDrawItemBackground(DrawContext context, ItemStack stack, int x, int y) {
        CooldownItemsTracker.renderBackground(context, stack, x, y, MinecraftClient.getInstance().currentScreen != null);
    }

    public static void renderDrawItemText(DrawContext context, ItemStack stack, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        CooldownItemsTracker.renderText(context, client.textRenderer, stack, x, y, client.currentScreen != null);
    }

    public static void renderStackOverlayBackground(DrawContext context, ItemStack stack, int x, int y) {
        CooldownItemsTracker.renderBackground(context, stack, x, y, MinecraftClient.getInstance().currentScreen != null);
    }

    public static void renderStackOverlayText(DrawContext context, TextRenderer textRenderer, ItemStack stack, int x, int y) {
        CooldownItemsTracker.renderText(context, textRenderer, stack, x, y, MinecraftClient.getInstance().currentScreen != null);
    }

    private static void renderSuperpairsOverlay(DrawContext context, Slot slot) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        String xpOverlay = ExperimentationTableSolverTracker.getSuperpairsOverlayText(slot);
        if (xpOverlay == null || xpOverlay.isBlank()) return;

        float scale = 0.6f;
        int textWidth = textRenderer.getWidth(xpOverlay);
        int xPos = slot.x + 2 + textWidth;
        int yPos = slot.y + 10;
        int drawX = (int) ((xPos - textWidth) / scale);
        int drawY = (int) (yPos / scale);
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);
        XenDrawContextCompat.drawTextWithShadow(context, textRenderer, xpOverlay, drawX, drawY, 0xFFFFFFFF);
        context.getMatrices().popMatrix();
    }

    private static void renderOverlayTexts(DrawContext context, TextRenderer textRenderer, ItemStack stack, int x, int y, boolean guiOpen) {
        for (CooldownItemsTracker.OverlayText overlay : CooldownItemsTracker.getOverlayTexts(stack, guiOpen)) {
            int ox = x + (overlay.alternativePosition() ? -8 : 0);
            int oy = y + (overlay.alternativePosition() ? -10 : 0);
            int width = textRenderer.getWidth(overlay.text());
            int drawX = ox + 19 - 2 - width;
            int drawY = oy + 9;
            XenDrawContextCompat.drawTextWithShadow(context, textRenderer, overlay.text(), drawX, drawY, 0xFF000000 | overlay.colorRgb());
        }
    }
}
