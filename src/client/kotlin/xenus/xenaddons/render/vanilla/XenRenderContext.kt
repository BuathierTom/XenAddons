package xenus.xenaddons.render.vanilla

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.Minecraft
import net.minecraft.client.Camera
import net.minecraft.client.renderer.MultiBufferSource
import com.mojang.blaze3d.vertex.PoseStack

data class XenRenderContext(
    val matrices: PoseStack,
    val consumers: MultiBufferSource,
    val camera: Camera
) {
    companion object {
        fun from(context: LevelRenderContext): XenRenderContext {
            return XenRenderContext(
                context.poseStack(),
                context.bufferSource(),
                Minecraft.getInstance().gameRenderer.mainCamera
            )
        }
    }
}
