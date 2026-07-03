package xenus.xenaddons.render.vanilla

import net.minecraft.client.renderer.MultiBufferSource
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import xenus.xenaddons.compat.XenCameraCompat
import xenus.xenaddons.utils.RenderUtils

object XenRender3D {
    fun line(
        matrices: PoseStack,
        consumers: MultiBufferSource,
        cameraPos: Vec3,
        start: Vec3,
        end: Vec3,
        color: Int,
        width: Float = 1f
    ) {
        RenderUtils.drawLine(matrices, consumers, cameraPos, start, end, color, width)
    }

    fun line(context: XenRenderContext, start: Vec3, end: Vec3, color: Int, width: Float = 1f) {
        line(context.matrices, context.consumers, XenCameraCompat.getPos(context.camera), start, end, color, width)
    }

    fun box(
        matrices: PoseStack,
        consumers: MultiBufferSource,
        cameraPos: Vec3,
        box: AABB,
        color: Int,
        filled: Boolean = false
    ) {
        RenderUtils.drawBox(matrices, consumers, cameraPos, box, color, filled)
    }

    fun box(context: XenRenderContext, box: AABB, color: Int, filled: Boolean = false) {
        box(context.matrices, context.consumers, XenCameraCompat.getPos(context.camera), box, color, filled)
    }
}
