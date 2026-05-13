package xenus.xenaddons.mixin.client;

import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.compat.XenPlayerRenderCompat;

@Mixin(PlayerEntityRenderer.class)
public class MixinPlayerEntityRendererCustomSize {

    @Inject(method = "scale(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;)V", at = @At("TAIL"))
    private void xenaddons$applyCustomSize(PlayerEntityRenderState state, MatrixStack matrices, CallbackInfo ci) {
        XenPlayerRenderCompat.applyCustomSize(state, matrices);
    }
}
