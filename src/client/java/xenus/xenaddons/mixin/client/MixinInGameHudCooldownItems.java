package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.compat.XenHudCompat;

@Mixin(InGameHud.class)
public class MixinInGameHudCooldownItems {

    @Inject(
        method = "renderHotbarItem",
        at = @At("TAIL"),
        require = 0
    )
    private void xenaddons$renderCooldownOnHotbar(DrawContext context, int x, int y, RenderTickCounter tickCounter, PlayerEntity player, ItemStack stack, int seed, CallbackInfo ci) {
        XenHudCompat.renderHotbarCooldown(context, x, y, player, stack);
    }
}
