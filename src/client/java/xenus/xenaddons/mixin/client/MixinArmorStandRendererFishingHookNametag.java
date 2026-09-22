package xenus.xenaddons.mixin.client;

import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xenus.xenaddons.features.impl.fishing.FishingHookDisplayModule;

@Mixin(ArmorStandRenderer.class)
public class MixinArmorStandRendererFishingHookNametag {

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/decoration/ArmorStand;D)Z", at = @At("HEAD"), cancellable = true)
    private void xenaddons$fishingHook$shouldShowName(ArmorStand entity, double distanceSq, CallbackInfoReturnable<Boolean> cir) {
        if (FishingHookDisplayModule.shouldHideVanillaIndicator(entity)) {
            cir.setReturnValue(false);
        }
    }
}
