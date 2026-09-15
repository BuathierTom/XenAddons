package xenus.xenaddons.cosmetics.wings;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public class DragonWingsModel extends EntityModel<AvatarRenderState> {

    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;
    private static final float FLAP_CYCLE_TICKS = 30.0F;
    private static final float BASE_X_ROT = (float) Math.toRadians(-80.0D);
    private static final float BASE_Y_ROT = (float) Math.toRadians(20.0D);
    private static final float BASE_Z_ROT = (float) Math.toRadians(20.0D);

    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart rightTip;
    private final ModelPart leftTip;

    public DragonWingsModel(ModelPart root) {
        super(root);
        this.rightWing = root.getChild("right_wing");
        this.leftWing = root.getChild("left_wing");
        this.rightTip = rightWing.getChild("tip");
        this.leftTip = leftWing.getChild("tip");
    }

    public static DragonWingsModel create() {
        MeshDefinition mesh = new MeshDefinition();

        for (boolean left : new boolean[] {false, true}) {
            float boxX = left ? 0.0F : -56.0F;
            PartDefinition wing = mesh.getRoot().addOrReplaceChild(
                left ? "left_wing" : "right_wing",
                CubeListBuilder.create().mirror(left)
                    .texOffs(112, 88).addBox(boxX, -4.0F, -4.0F, 56, 8, 8)
                    .texOffs(-56, 88).addBox(boxX, 0.0F, 2.0F, 56, 0, 56),
                PartPose.offset(left ? 12.0F : -12.0F, 5.0F, 2.0F)
            );

            wing.addOrReplaceChild(
                "tip",
                CubeListBuilder.create().mirror(left)
                    .texOffs(112, 136).addBox(boxX, -2.0F, -2.0F, 56, 4, 4)
                    .texOffs(-56, 144).addBox(boxX, 0.0F, 2.0F, 56, 0, 56),
                PartPose.offset(left ? 56.0F : -56.0F, 0.0F, 0.0F)
            );
        }

        return new DragonWingsModel(LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT).bakeRoot());
    }

    @Override
    public void setupAnim(AvatarRenderState state) {
        super.setupAnim(state);

        float phase = (state.ageInTicks % FLAP_CYCLE_TICKS) / FLAP_CYCLE_TICKS * ((float) Math.PI * 2.0F);

        rightWing.xRot = BASE_X_ROT - (float) Math.cos(phase) * 0.2F;
        rightWing.yRot = BASE_Y_ROT + (float) Math.sin(phase) * 0.4F;
        rightWing.zRot = BASE_Z_ROT;
        rightTip.zRot = -((float) Math.sin(phase + 2.0F) + 0.5F) * 0.75F;

        leftWing.xRot = rightWing.xRot;
        leftWing.yRot = -rightWing.yRot;
        leftWing.zRot = -rightWing.zRot;
        leftTip.zRot = -rightTip.zRot;
    }

    public ModelPart rightWing() {
        return rightWing;
    }

    public ModelPart leftWing() {
        return leftWing;
    }
}
