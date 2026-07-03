package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.compat.XenCooldownCompat;

@Mixin(AbstractContainerScreen.class)
public class MixinHandledScreenCooldownItems {

    @Redirect(
        method = "extractSlot",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;"),
        require = 0
    )
    private ItemStack xenaddons$replaceSuperpairsUnknownStack(Slot slot) {
        return XenCooldownCompat.replaceSuperpairsUnknownStack(slot);
    }

    @Inject(
        method = "extractSlot",
        at = @At("TAIL"),
        require = 0
    )
    private void xenaddons$renderCooldownInInventory(GuiGraphicsExtractor context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        XenCooldownCompat.renderInventorySlot(context, slot);
    }
}
