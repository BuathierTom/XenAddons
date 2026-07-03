package xenus.xenaddons.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import xenus.xenaddons.features.impl.misc.CooldownItemsTracker;
import xenus.xenaddons.features.impl.misc.ExperimentationTableSolverTracker;

public final class XenCooldownCompat {
    private XenCooldownCompat() {}

    public static ItemStack replaceSuperpairsUnknownStack(Slot slot) {
        if (slot == null) return ItemStack.EMPTY;
        ItemStack replacement = ExperimentationTableSolverTracker.getSuperpairsVisualStack(slot);
        if (replacement != null && !replacement.isEmpty()) return replacement;
        return slot.getItem();
    }

    public static void renderInventorySlot(GuiGraphicsExtractor context, Slot slot) {
        if (slot == null) return;
        renderSuperpairsOverlay(context, slot);

        ItemStack stack = slot.getItem();
        if (stack == null || stack.isEmpty()) return;
        CooldownItemsTracker.renderBackground(context, stack, slot.x, slot.y, true);
        renderOverlayTexts(context, Minecraft.getInstance().font, stack, slot.x, slot.y, true);
    }

    public static void renderDrawItemBackground(GuiGraphicsExtractor context, ItemStack stack, int x, int y) {
        CooldownItemsTracker.renderBackground(context, stack, x, y, Minecraft.getInstance().screen != null);
    }

    public static void renderDrawItemText(GuiGraphicsExtractor context, ItemStack stack, int x, int y) {
        Minecraft client = Minecraft.getInstance();
        CooldownItemsTracker.renderText(context, client.font, stack, x, y, client.screen != null);
    }

    public static void renderStackOverlayBackground(GuiGraphicsExtractor context, ItemStack stack, int x, int y) {
        CooldownItemsTracker.renderBackground(context, stack, x, y, Minecraft.getInstance().screen != null);
    }

    public static void renderStackOverlayText(GuiGraphicsExtractor context, Font textRenderer, ItemStack stack, int x, int y) {
        CooldownItemsTracker.renderText(context, textRenderer, stack, x, y, Minecraft.getInstance().screen != null);
    }

    private static void renderSuperpairsOverlay(GuiGraphicsExtractor context, Slot slot) {
        Font textRenderer = Minecraft.getInstance().font;
        String xpOverlay = ExperimentationTableSolverTracker.getSuperpairsOverlayText(slot);
        if (xpOverlay == null || xpOverlay.isBlank()) return;

        float scale = 0.6f;
        int textWidth = textRenderer.width(xpOverlay);
        int xPos = slot.x + 2 + textWidth;
        int yPos = slot.y + 10;
        int drawX = (int) ((xPos - textWidth) / scale);
        int drawY = (int) (yPos / scale);
        context.pose().pushMatrix();
        context.pose().scale(scale, scale);
        XenDrawContextCompat.text(context, textRenderer, xpOverlay, drawX, drawY, 0xFFFFFFFF);
        context.pose().popMatrix();
    }

    private static void renderOverlayTexts(GuiGraphicsExtractor context, Font textRenderer, ItemStack stack, int x, int y, boolean guiOpen) {
        for (CooldownItemsTracker.OverlayText overlay : CooldownItemsTracker.getOverlayTexts(stack, guiOpen)) {
            int ox = x + (overlay.alternativePosition() ? -8 : 0);
            int oy = y + (overlay.alternativePosition() ? -10 : 0);
            int width = textRenderer.width(overlay.text());
            int drawX = ox + 19 - 2 - width;
            int drawY = oy + 9;
            XenDrawContextCompat.text(context, textRenderer, overlay.text(), drawX, drawY, 0xFF000000 | overlay.colorRgb());
        }
    }
}
