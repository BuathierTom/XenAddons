package xenus.xenaddons.render.vanilla

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.Minecraft
import net.minecraft.client.Camera
import net.minecraft.client.renderer.OrderedSubmitNodeCollector
import com.mojang.blaze3d.vertex.PoseStack

data class XenRenderContext(
    val matrices: PoseStack,
    val collector: OrderedSubmitNodeCollector,
    val camera: Camera
) {
    companion object {
        fun from(context: LevelRenderContext): XenRenderContext {
            return XenRenderContext(
                context.poseStack(),
                context.submitNodeCollector(),
                Minecraft.getInstance().gameRenderer.getMainCamera()
            )
        }
    }
}
