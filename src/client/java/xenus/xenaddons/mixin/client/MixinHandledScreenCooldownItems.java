package xenus.xenaddons.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.features.impl.misc.CooldownItemsTracker;
import xenus.xenaddons.features.impl.misc.ExperimentationTableSolverTracker;

@Mixin(HandledScreen.class)
public class MixinHandledScreenCooldownItems {

    @Redirect(
        method = "drawSlot",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;getStack()Lnet/minecraft/item/ItemStack;"),
        require = 0
    )
    private ItemStack xenaddons$replaceSuperpairsUnknownStack(Slot slot) {
        if (slot == null) return ItemStack.EMPTY;
        ItemStack replacement = ExperimentationTableSolverTracker.getSuperpairsVisualStack(slot);
        if (replacement != null && !replacement.isEmpty()) return replacement;
        return slot.getStack();
    }

    @Inject(
        method = "drawSlot",
        at = @At("TAIL"),
        require = 0
    )
    private void xenaddons$renderCooldownInInventory(DrawContext context, Slot slot, CallbackInfo ci) {
        if (slot == null) return;
        ExperimentationTableSolverTracker.renderSlotHighlight(context, slot);

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        String xpOverlay = ExperimentationTableSolverTracker.getSuperpairsOverlayText(slot);
        if (xpOverlay != null && !xpOverlay.isBlank()) {
            float scale = 0.6f;
            int textWidth = textRenderer.getWidth(xpOverlay);
            int xPos = slot.x + 2 + textWidth;
            int yPos = slot.y + 10;
            int drawX = (int) ((xPos - textWidth) / scale);
            int drawY = (int) (yPos / scale);
            context.getMatrices().pushMatrix();
            context.getMatrices().scale(scale, scale);
            context.drawTextWithShadow(textRenderer, xpOverlay, drawX, drawY, 0xFFFFFFFF);
            context.getMatrices().popMatrix();
        }

        ItemStack stack = slot.getStack();
        if (stack == null || stack.isEmpty()) return;
        int x = slot.x;
        int y = slot.y;
        CooldownItemsTracker.renderBackground(context, stack, x, y, true);
        for (CooldownItemsTracker.OverlayText overlay : CooldownItemsTracker.getOverlayTexts(stack, true)) {
            int ox = x + (overlay.alternativePosition() ? -8 : 0);
            int oy = y + (overlay.alternativePosition() ? -10 : 0);
            int w = textRenderer.getWidth(overlay.text());
            int drawX = ox + 19 - 2 - w;
            int drawY = oy + 9;
            context.drawTextWithShadow(textRenderer, overlay.text(), drawX, drawY, 0xFF000000 | overlay.colorRgb());
        }
    }
}
