package xenus.xenaddons.mixin.client;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.compat.XenPlayerRenderCompat;

@Mixin(AvatarRenderer.class)
public class MixinPlayerEntityRendererCustomSize {

    @Inject(method = "scale(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("TAIL"))
    private void xenaddons$applyCustomSize(AvatarRenderState state, PoseStack matrices, CallbackInfo ci) {
        XenPlayerRenderCompat.applyCustomSize(state, matrices);
    }
}
