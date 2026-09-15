package xenus.xenaddons.cosmetics.wings;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import xenus.xenaddons.config.CustomWingsManager;
import xenus.xenaddons.cosmetics.CosmeticsSnapshot.PlayerWings;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.features.impl.misc.CustomSizeModule;

public class DragonWingsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final Identifier DRAGON_TEXTURE =
        Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon.png");

    private static final float BASE_SCALE = 0.2F;
    private static final float OFFSET_Y = 0.09F;
    private static final float OFFSET_Z = 0.15F;
    private static final int SUBMIT_ORDER = 0;

    private final DragonWingsModel wingsModel = DragonWingsModel.create();

    public DragonWingsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible || state.isSpectator) return;

        CustomSizeModule cosmetics = ModuleManager.getModule(CustomSizeModule.class);
        if (cosmetics == null || !cosmetics.shouldApplyCustomWings()) return;

        PlayerWings wings = resolveWings(state);
        if (wings == null) return;

        float scale = BASE_SCALE * wings.size();
        if (scale <= 0.0F) return;

        wingsModel.setupAnim(state);

        poseStack.pushPose();
        getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(0.0F, OFFSET_Y, OFFSET_Z);
        poseStack.scale(scale, scale, scale);
        renderColoredCutoutModel(wingsModel, DRAGON_TEXTURE, poseStack, collector, light, state, wings.color(), SUBMIT_ORDER);
        poseStack.popPose();
    }

    private static PlayerWings resolveWings(AvatarRenderState state) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return null;
        if (!(client.level.getEntity(state.id) instanceof Player player)) return null;

        return CustomWingsManager.get(player.getUUID());
    }
}
