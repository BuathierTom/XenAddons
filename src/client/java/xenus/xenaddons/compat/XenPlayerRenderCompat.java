package xenus.xenaddons.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import xenus.xenaddons.config.CustomPlayerScaleManager;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.features.impl.misc.CustomSizeModule;

public final class XenPlayerRenderCompat {
    private XenPlayerRenderCompat() {}

    public static void applyCustomSize(AvatarRenderState state, PoseStack matrices) {
        CustomSizeModule module = ModuleManager.getModule(CustomSizeModule.class);
        if (module == null || !module.shouldApplyCustomSize()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        if (!(client.level.getEntity(state.id) instanceof Player player)) return;

        CustomPlayerScaleManager.PlayerScale scale = CustomPlayerScaleManager.get(player.getUUID());
        if (scale == null) return;

        if (scale.y() < 0.0F) {
            matrices.translate(0.0F, scale.y() * 2.0F, 0.0F);
        }

        matrices.scale(scale.x(), scale.y(), scale.z());

        Vec3 labelPos = state.nameTagAttachment;
        if (labelPos != null) {
            double adjustedY = (labelPos.y + 0.15D) * Math.abs(scale.y());
            state.nameTagAttachment = new Vec3(labelPos.x, adjustedY, labelPos.z);
        }
    }
}
