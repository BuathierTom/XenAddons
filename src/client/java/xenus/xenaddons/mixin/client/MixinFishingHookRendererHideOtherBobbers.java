package xenus.xenaddons.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xenus.xenaddons.features.impl.fishing.HideOtherBobbersModule;

@Mixin(FishingHookRenderer.class)
public class MixinFishingHookRendererHideOtherBobbers {

    @Inject(
            method = "shouldRender(Lnet/minecraft/world/entity/projectile/FishingHook;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void xenaddons$hideOtherBobbers(FishingHook fishingHook, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (HideOtherBobbersModule.isActive() && !(fishingHook.getPlayerOwner() instanceof LocalPlayer)) {
            cir.setReturnValue(false);
        }
    }
}
