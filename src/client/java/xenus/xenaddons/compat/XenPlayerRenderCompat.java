package xenus.xenaddons.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import xenus.xenaddons.config.CustomPlayerScaleManager;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.features.impl.misc.CustomSizeModule;

public final class XenPlayerRenderCompat {
    private XenPlayerRenderCompat() {}

    public static void applyCustomSize(PlayerEntityRenderState state, MatrixStack matrices) {
        CustomSizeModule module = ModuleManager.getModule(CustomSizeModule.class);
        if (module == null || !module.shouldApplyCustomSize()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        if (!(client.world.getEntityById(state.id) instanceof PlayerEntity player)) return;

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
