package xenus.xenaddons.features.impl.dungeons;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeightedPressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xenus.xenaddons.compat.XenCameraCompat;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.utils.DungeonUtils;
import xenus.xenaddons.utils.RenderUtils;

public class HighlightPressurePlateModule extends Module {
    private static final BlockPos I4_PLATE_POS = new BlockPos(63, 127, 35);
    private static final int HIGHLIGHT_COLOR = 0xA000FF00;

    public HighlightPressurePlateModule() {
        super("Highlight Pressure Plate", "Highlights the I4 Light Weighted Pressure Plate in green while pressed", Category.DUNGEON);

        LevelRenderEvents.COLLECT_SUBMITS.register(this::onWorldRender);
    }

    private void onWorldRender(LevelRenderContext context) {
        if (!this.isEnabled() || !DungeonUtils.inDungeon()) return;
        if (mc.level == null || mc.gameRenderer == null) return;

        BlockState state = mc.level.getBlockState(I4_PLATE_POS);
        if (!state.is(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE)) return;
        if (state.getValue(WeightedPressurePlateBlock.POWER) <= 0) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        if (camera == null) return;
        Vec3 cameraPos = XenCameraCompat.getPos(camera);

        AABB box = state.getShape(mc.level, I4_PLATE_POS).bounds().move(I4_PLATE_POS).inflate(0.002);

        RenderUtils.drawBox(context.poseStack(), context.submitNodeCollector(), cameraPos, box, HIGHLIGHT_COLOR, true);
    }
}
