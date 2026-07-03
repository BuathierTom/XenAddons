package xenus.xenaddons.mixin.client;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.impl.client.rendering.fluid.FluidRenderingRegistryImpl;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xenus.xenaddons.features.impl.misc.LavaReplacementModule;

import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
@Mixin(value = FluidRenderingRegistryImpl.class, remap = false)
public class MixinFluidRenderHandlerRegistry {

    @Shadow
    @Final
    private static Map<Fluid, FluidRenderHandler> HANDLERS;

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private static void xenaddons$replaceLavaWithWaterHandler(Fluid fluid, CallbackInfoReturnable<FluidRenderHandler> cir) {
        if (!LavaReplacementModule.isActive()) return;
        if (fluid == Fluids.LAVA) {
            cir.setReturnValue(HANDLERS.get(Fluids.WATER));
        } else if (fluid == Fluids.FLOWING_LAVA) {
            cir.setReturnValue(HANDLERS.get(Fluids.FLOWING_WATER));
        }
    }
}
