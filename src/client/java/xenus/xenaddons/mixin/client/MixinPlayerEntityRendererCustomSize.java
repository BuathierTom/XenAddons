package xenus.xenaddons.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.config.CustomPlayerScaleManager;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.features.impl.misc.CustomSizeModule;

@Mixin(PlayerEntityRenderer.class)
public class MixinPlayerEntityRendererCustomSize {

    @Inject(method = "scale(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;)V", at = @At("TAIL"))
    private void xenaddons$applyCustomSize(PlayerEntityRenderState state, MatrixStack matrices, CallbackInfo ci) {
        CustomSizeModule module = ModuleManager.getModule(CustomSizeModule.class);
        if (module == null || !module.isEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        if (!(mc.world.getEntityById(state.id) instanceof PlayerEntity player)) return;

        CustomPlayerScaleManager.PlayerScale scale = CustomPlayerScaleManager.get(player.getUuid());
        if (scale == null) return;

        if (scale.y() < 0.0F) {
            matrices.translate(0.0F, scale.y() * 2.0F, 0.0F);
        }

        matrices.scale(scale.x(), scale.y(), scale.z());

        Vec3d labelPos = state.nameLabelPos;
        if (labelPos != null) {
            double adjustedY = (labelPos.y + 0.15D) * Math.abs(scale.y());
            state.nameLabelPos = new Vec3d(labelPos.x, adjustedY, labelPos.z);
        }
    }
}
