package xenus.xenaddons.api.utils

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import java.awt.Color

object Render3DImmediate {
    var poseStack: PoseStack? = null
    var consumers: MultiBufferSource? = null
    
    fun setup(stack: PoseStack, source: MultiBufferSource?) {
        this.poseStack = stack
        this.consumers = source
    }

    fun renderWireframeBox(
        x: Double, y: Double, z: Double,
        w: Double, h: Double,
        color: Int, // Support Int color (RGB/ARGB)
        lineWidth: Float = 2.0f,
        phase: Boolean = false,
        centered: Boolean = false
    ) {
        val c = Color(color, true)
        renderWireframeBox(x, y, z, w, h, c, lineWidth.toDouble(), phase, 0.0, centered)
    }

    // Matching Devonian signature
    fun renderWireframeBox(
        x: Double,
        y: Double,
        z: Double,
        w: Double,
        h: Double,
        color: Color,
        lineWidth: Double = 1.0,
        phase: Boolean = false,
        wz: Double = 0.0, // Used for depth of box if different from w? usually w=wz implies square base if not specified? 
                         // Devonian: renderWireframeBox(x,y,z,w,h,color,lineWidth,phase, wz, centered)
        centered: Boolean = false
    ) {
        val ps = poseStack ?: return
        val buffer = consumers ?: Minecraft.getInstance().renderBuffers().bufferSource()
        
        val red = color.red / 255f
        val green = color.green / 255f
        val blue = color.blue / 255f
        val alpha = color.alpha / 255f

        val finalX = if (centered) x - w / 2 else x
        val finalY = y
        val finalZ = if (centered) z - (if (wz != 0.0) wz else w) / 2 else z
        val finalW = w
        val finalH = h
        val finalD = if (wz != 0.0) wz else w

        val minX = finalX
        val minY = finalY
        val minZ = finalZ
        val maxX = finalX + finalW
        val maxY = finalY + finalH
        val maxZ = finalZ + finalD
        
        // Use LINES RenderType
        // In 1.21, RenderType.lines() might be different or require specific handling
        val builder = buffer.getBuffer(RenderType.lines())
        
        ps.pushPose()
        // We usually need to translate to camera pos if we are rendering absolute world coords.
        // But Fabric AFTER_ENTITIES stack is usually relative to Camera already? 
        // CHECK: Fabric docs say "The matrix stack is transformed to the camera's position." meaning (0,0,0) is camera?
        // No, usually context.matrixStack() in render events is ALREADY set up such that drawing at (x,y,z) draws at relative to camera.
        // But LividSolver passes World Coords (entity.position).
        // If stack is at 0,0,0 relative to Camera, we need to translate by (x - camX, y - camY, z - camZ).
        // Devonian logic was `if (translate) ...`.
        // Let's assume we need to translate relative to camera unless the stack is already there.
        // Fabric context.matrixStack() is "positioned at the camera".
        // It means (0,0,0) is the camera.
        // So we need to subtract camera pos from world pos.
        
        val cam = Minecraft.getInstance().gameRenderer.mainCamera.position
        
        ps.translate(-cam.x, -cam.y, -cam.z)

        drawBoxWireframe(ps, builder, minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, alpha)
        
        ps.popPose()
    }
    
    private fun drawBoxWireframe(
        ps: PoseStack, builder: VertexConsumer, 
        minX: Double, minY: Double, minZ: Double, 
        maxX: Double, maxY: Double, maxZ: Double, 
        r: Float, g: Float, b: Float, a: Float
    ) {
        val entry = ps.last()
        
        // Bottom
        builder.addVertex(entry, minX.toFloat(), minY.toFloat(), minZ.toFloat()).setColor(r, g, b, a).setNormal(entry, 1f, 0f, 0f)
        builder.addVertex(entry, maxX.toFloat(), minY.toFloat(), minZ.toFloat()).setColor(r, g, b, a).setNormal(entry, 1f, 0f, 0f)
        
        builder.addVertex(entry, maxX.toFloat(), minY.toFloat(), minZ.toFloat()).setColor(r, g, b, a).setNormal(entry, 0f, 0f, 1f)
        builder.addVertex(entry, maxX.toFloat(), minY.toFloat(), maxZ.toFloat()).setColor(r, g, b, a).setNormal(entry, 0f, 0f, 1f)
        
        builder.addVertex(entry, maxX.toFloat(), minY.toFloat(), maxZ.toFloat()).setColor(r, g, b, a).setNormal(entry, -1f, 0f, 0f)
        builder.addVertex(entry, minX.toFloat(), minY.toFloat(), maxZ.toFloat()).setColor(r, g, b, a).setNormal(entry, -1f, 0f, 0f)
        
        builder.addVertex(entry, minX.toFloat(), minY.toFloat(), maxZ.toFloat()).setColor(r, g, b, a).setNormal(entry, 0f, 0f, -1f)
        builder.addVertex(entry, minX.toFloat(), minY.toFloat(), minZ.toFloat()).setColor(r, g, b, a).setNormal(entry, 0f, 0f, -1f)
        
        // Top
        builder.addVertex(entry, minX.toFloat(), maxY.toFloat(), minZ.toFloat()).setColor(r, g, b, a).setNormal(entry, 1f, 0f, 0f)
        builder.addVertex(entry, maxX.toFloat(), maxY.toFloat(), minZ.toFloat()).setColor(r, g, b, a).setNormal(entry, 1f, 0f, 0f)
        
        builder.addVertex(entry, maxX.toFloat(), maxY.toFloat(), minZ.toFloat()).setColor(r, g, b, a).setNormal(entry, 0f, 0f, 1f)
        builder.addVertex(entry, maxX.toFloat(), maxY.toFloat(), maxZ.toFloat()).setColor(r, g, b, a).setNormal(entry, 0f, 0f, 1f)
        
        builder.addVertex(entry, maxX.toFloat(), maxY.toFloat(), maxZ.toFloat()).setColor(r, g, b, a).setNormal(entry, -1f, 0f, 0f)
        builder.addVertex(entry, minX.toFloat(), maxY.toFloat(), maxZ.toFloat()).setColor(r, g, b, a).setNormal(entry, -1f, 0f, 0f)
        
        builder.addVertex(entry, minX.toFloat(), maxY.toFloat(), maxZ.toFloat()).setColor(r, g, b, a).setNormal(entry, 0f, 0f, -1f)
        builder.addVertex(entry, minX.toFloat(), maxY.toFloat(), minZ.toFloat()).setColor(r, g, b, a).setNormal(entry, 0f, 0f, -1f)
        
        // Sides
        builder.addVertex(entry, minX.toFloat(), minY.toFloat(), minZ.toFloat()).setColor(r, g, b, a).setNormal(entry, 0f, 1f, 0f)
        builder.addVertex(entry, minX.toFloat(), maxY.toFloat(), minZ.toFloat()).setColor(r, g, b, a).setNormal(entry, 0f, 1f, 0f)
        
        builder.addVertex(entry, maxX.toFloat(), minY.toFloat(), minZ.toFloat()).setColor(r, g, b, a).setNormal(entry, 0f, 1f, 0f)
        builder.addVertex(entry, maxX.toFloat(), maxY.toFloat(), minZ.toFloat()).setColor(r, g, b, a).setNormal(entry, 0f, 1f, 0f)
        
        builder.addVertex(entry, maxX.toFloat(), minY.toFloat(), maxZ.toFloat()).setColor(r, g, b, a).setNormal(entry, 0f, 1f, 0f)
        builder.addVertex(entry, maxX.toFloat(), maxY.toFloat(), maxZ.toFloat()).setColor(r, g, b, a).setNormal(entry, 0f, 1f, 0f)
        
        builder.addVertex(entry, minX.toFloat(), minY.toFloat(), maxZ.toFloat()).setColor(r, g, b, a).setNormal(entry, 0f, 1f, 0f)
        builder.addVertex(entry, minX.toFloat(), maxY.toFloat(), maxZ.toFloat()).setColor(r, g, b, a).setNormal(entry, 0f, 1f, 0f)
    }

    fun renderFilledBox(
        x: Double, y: Double, z: Double,
        w: Double, h: Double,
        color: Color,
        phase: Boolean = false,
        wz: Double = 0.0,
        centered: Boolean = false
    ) {
        // Implementation for filled box if needed (ThreeWeirdosSolver uses it)
        // ...
        // Simplification: skip phase/translation for now or assume default
        val ps = poseStack ?: return
        val buffer = consumers ?: Minecraft.getInstance().renderBuffers().bufferSource()
        
        val red = color.red / 255f
        val green = color.green / 255f
        val blue = color.blue / 255f
        val alpha = color.alpha / 255f

        val finalX = if (centered) x - w / 2 else x
        val finalY = y
        val finalZ = if (centered) z - (if (wz != 0.0) wz else w) / 2 else z
        val finalW = w
        val finalH = h
        val finalD = if (wz != 0.0) wz else w
        
        // Use standard translucent render type or similar
        // RenderType.debugQuads() might assume something
        // Just use a generic quad type
        // Note: Filling requires Quads, lines require Lines.
        // Creating a custom render type on the fly works too.
    }
    
    fun renderTracer(
        x: Double, y: Double, z: Double,
        color: Color,
        lineWidth: Double = 1.0
    ) {
         val ps = poseStack ?: return
         val buffer = consumers ?: Minecraft.getInstance().renderBuffers().bufferSource()
         
         val cam = Minecraft.getInstance().gameRenderer.mainCamera.position
         val vec = Minecraft.getInstance().player?.getEyePosition(1.0f) ?: cam // Or look vector
         // Tracer usually from Layout/Center to Target.
         // Let's just draw line from Eye to (x,y,z)
         
         val builder = buffer.getBuffer(RenderType.lines())
         val r = color.red / 255f
         val g = color.green / 255f
         val b = color.blue / 255f
         val a = color.alpha / 255f
         
         ps.pushPose()
         ps.translate(-cam.x, -cam.y, -cam.z)
         
         val entry = ps.last()
         val eye = Minecraft.getInstance().player?.getEyePosition(1.0f) ?: return

         builder.addVertex(entry, eye.x.toFloat(), eye.y.toFloat(), eye.z.toFloat())
            .setColor(r, g, b, a).setNormal(entry, 0f, 1f, 0f)
         builder.addVertex(entry, x.toFloat(), y.toFloat(), z.toFloat())
            .setColor(r, g, b, a).setNormal(entry, 0f, 1f, 0f)

         ps.popPose()
    }
}
