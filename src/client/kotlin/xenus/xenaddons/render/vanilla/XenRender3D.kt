package xenus.xenaddons.render.vanilla

import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import xenus.xenaddons.compat.XenCameraCompat
import xenus.xenaddons.utils.RenderUtils

object XenRender3D {
    fun line(
        matrices: MatrixStack,
        consumers: VertexConsumerProvider,
        cameraPos: Vec3d,
        start: Vec3d,
        end: Vec3d,
        color: Int,
        width: Float = 1f
    ) {
        RenderUtils.drawLine(matrices, consumers, cameraPos, start, end, color, width)
    }

    fun line(context: XenRenderContext, start: Vec3d, end: Vec3d, color: Int, width: Float = 1f) {
        line(context.matrices, context.consumers, XenCameraCompat.getPos(context.camera), start, end, color, width)
    }

    fun box(
        matrices: MatrixStack,
        consumers: VertexConsumerProvider,
        cameraPos: Vec3d,
        box: Box,
        color: Int,
        filled: Boolean = false
    ) {
        RenderUtils.drawBox(matrices, consumers, cameraPos, box, color, filled)
    }

    fun box(context: XenRenderContext, box: Box, color: Int, filled: Boolean = false) {
        box(context.matrices, context.consumers, XenCameraCompat.getPos(context.camera), box, color, filled)
    }
}
