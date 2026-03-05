package xenus.xenaddons.mixin.client;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.impl.client.rendering.fluid.FluidRenderHandlerRegistryImpl;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xenus.xenaddons.features.impl.misc.LavaReplacementModule;

import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
@Mixin(value = FluidRenderHandlerRegistryImpl.class, remap = false)
public class MixinFluidRenderHandlerRegistry {

    @Shadow
    @Final
    private Map<Fluid, FluidRenderHandler> handlers;

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void xenaddons$replaceLavaWithWaterHandler(Fluid fluid, CallbackInfoReturnable<FluidRenderHandler> cir) {
        if (!LavaReplacementModule.isActive()) return;
        if (fluid == Fluids.LAVA) {
            cir.setReturnValue(handlers.get(Fluids.WATER));
        } else if (fluid == Fluids.FLOWING_LAVA) {
            cir.setReturnValue(handlers.get(Fluids.FLOWING_WATER));
        }
    }
}
