package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.compat.XenCooldownCompat;

@Mixin(HandledScreen.class)
public class MixinHandledScreenCooldownItems {

    @Redirect(
        method = "drawSlot",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;getStack()Lnet/minecraft/item/ItemStack;"),
        require = 0
    )
    private ItemStack xenaddons$replaceSuperpairsUnknownStack(Slot slot) {
        return XenCooldownCompat.replaceSuperpairsUnknownStack(slot);
    }

    @Inject(
        method = "drawSlot",
        at = @At("TAIL"),
        require = 0
    )
    private void xenaddons$renderCooldownInInventory(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        XenCooldownCompat.renderInventorySlot(context, slot);
    }
}
