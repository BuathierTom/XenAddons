package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.compat.XenHudCompat;

@Mixin(Gui.class)
public class MixinInGameHudCooldownItems {

    @Inject(
        method = "extractSlot",
        at = @At("TAIL"),
        require = 0
    )
    private void xenaddons$renderCooldownOnHotbar(GuiGraphicsExtractor context, int x, int y, DeltaTracker tickCounter, Player player, ItemStack stack, int seed, CallbackInfo ci) {
        XenHudCompat.renderHotbarCooldown(context, x, y, player, stack);
    }
}
