package xenus.xenaddons.render.vanilla

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.Camera
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack

data class XenRenderContext(
    val matrices: MatrixStack,
    val consumers: VertexConsumerProvider,
    val camera: Camera
) {
    companion object {
        fun from(context: WorldRenderContext): XenRenderContext {
            return XenRenderContext(
                context.matrices(),
                context.consumers() ?: MinecraftClient.getInstance().bufferBuilders.entityVertexConsumers,
                MinecraftClient.getInstance().gameRenderer.camera
            )
        }
    }
}
