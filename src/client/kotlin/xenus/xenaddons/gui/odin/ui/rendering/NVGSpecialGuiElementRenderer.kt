package xenus.xenaddons.gui.odin.ui.rendering

import com.mojang.blaze3d.opengl.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.GlBackend
import net.minecraft.client.gui.render.SpecialGuiElementRenderer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.texture.GlTextureView
import net.minecraft.client.util.math.MatrixStack
import org.lwjgl.opengl.GL33C
import kotlin.math.min

class NVGSpecialGuiElementRenderer(vertexConsumers: VertexConsumerProvider.Immediate) :
    SpecialGuiElementRenderer<NanoVgGuiElementRenderState>(vertexConsumers) {

    override fun getElementClass(): Class<NanoVgGuiElementRenderState> = NanoVgGuiElementRenderState::class.java

    override fun render(state: NanoVgGuiElementRenderState, matrices: MatrixStack) {
        val colorView = RenderSystem.outputColorTextureOverride as? GlTextureView ?: return
        val depthView = RenderSystem.outputDepthTextureOverride as? GlTextureView ?: return
        val device = RenderSystem.getDevice() as? GlBackend ?: return

        val colorTexture = colorView.texture()
        val depthTexture = depthView.texture()
        val pixelWidth = colorTexture.getWidth(0)
        val pixelHeight = colorTexture.getHeight(0)
        if (pixelWidth <= 0 || pixelHeight <= 0) return

        val framebufferId = colorTexture.getOrCreateFramebuffer(device.bufferManager, depthTexture)
        val previousFramebuffer = GlStateManager.getFrameBuffer(GL33C.GL_FRAMEBUFFER)
        val client = MinecraftClient.getInstance()
        val logicalWidth = (state.x2() - state.x1()).coerceAtLeast(1)
        val logicalHeight = (state.y2() - state.y1()).coerceAtLeast(1)
        val devicePixelRatio = min(
            pixelWidth.toFloat() / logicalWidth.toFloat(),
            pixelHeight.toFloat() / logicalHeight.toFloat()
        ).coerceAtLeast(1f)

        var frameStarted = false

        try {
            GlStateManager._glBindFramebuffer(GL33C.GL_FRAMEBUFFER, framebufferId)
            GlStateManager._viewport(0, 0, pixelWidth, pixelHeight)
            GlStateManager._disableScissorTest()
            GL33C.glBindSampler(0, 0)

            NVGRenderer.beginFrame(pixelWidth.toFloat(), pixelHeight.toFloat(), devicePixelRatio)
            frameStarted = true
            state.renderContent().run()
        } finally {
            if (frameStarted) {
                NVGRenderer.endFrame()
            }

            RenderSystem.setupDefaultState()
            GlStateManager._glBindFramebuffer(GL33C.GL_FRAMEBUFFER, previousFramebuffer)
            GlStateManager._viewport(0, 0, client.window.framebufferWidth, client.window.framebufferHeight)
        }
    }

    override fun getName(): String = "xenaddons_nvg"
}
