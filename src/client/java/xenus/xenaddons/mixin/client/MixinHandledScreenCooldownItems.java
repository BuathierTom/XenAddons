package xenus.xenaddons.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.features.impl.misc.CooldownItemsTracker;

@Mixin(HandledScreen.class)
public class MixinHandledScreenCooldownItems {

    @Inject(
        method = "drawSlot",
        at = @At("TAIL"),
        require = 0
    )
    private void xenaddons$renderCooldownInInventory(DrawContext context, Slot slot, CallbackInfo ci) {
        if (slot == null) return;
        ItemStack stack = slot.getStack();
        if (stack == null || stack.isEmpty()) return;
        int x = slot.x;
        int y = slot.y;
        CooldownItemsTracker.renderBackground(context, stack, x, y, true);
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        for (xenus.xenaddons.features.impl.misc.CooldownItemsTracker.OverlayText overlay : CooldownItemsTracker.getOverlayTexts(stack, true)) {
            int ox = x + (overlay.alternativePosition() ? -8 : 0);
            int oy = y + (overlay.alternativePosition() ? -10 : 0);
            int w = textRenderer.getWidth(overlay.text());
            int drawX = ox + 19 - 2 - w;
            int drawY = oy + 9;
            context.drawTextWithShadow(textRenderer, overlay.text(), drawX, drawY, 0xFF000000 | overlay.colorRgb());
        }
    }
}
