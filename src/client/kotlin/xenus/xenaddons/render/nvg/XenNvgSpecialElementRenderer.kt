package xenus.xenaddons.render.nvg

import com.mojang.blaze3d.opengl.GlConst
import com.mojang.blaze3d.opengl.DirectStateAccess
import com.mojang.blaze3d.opengl.GlStateManager
import com.mojang.blaze3d.opengl.GlTexture
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.renderer.MultiBufferSource
import org.lwjgl.opengl.GL33C

class XenNvgSpecialElementRenderer(vertexConsumers: MultiBufferSource.BufferSource) :
    PictureInPictureRenderer<XenNvgRenderState>(vertexConsumers) {

    override fun getRenderStateClass(): Class<XenNvgRenderState> = XenNvgRenderState::class.java

    override fun renderToTexture(state: XenNvgRenderState, poseStack: PoseStack) {
        val colorTextureView = RenderSystem.outputColorTextureOverride ?: return
        val bufferManager = directStateAccess() ?: return
        val depthTexture = (RenderSystem.outputDepthTextureOverride?.texture() as? GlTexture) ?: return

        val pixelWidth = colorTextureView.getWidth(0)
        val pixelHeight = colorTextureView.getHeight(0)
        if (pixelWidth <= 0 || pixelHeight <= 0) return

        (colorTextureView.texture() as? GlTexture)?.getFbo(bufferManager, depthTexture)?.let { framebufferId ->
            GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, framebufferId)
            GlStateManager._viewport(0, 0, pixelWidth, pixelHeight)
        }

        GL33C.glBindSampler(0, 0)
        XenNvgRenderer.beginFrame(pixelWidth.toFloat(), pixelHeight.toFloat())
        state.renderContent().run()
        XenNvgRenderer.endFrame()

        GlStateManager._disableDepthTest()
        GlStateManager._disableCull()
        GlStateManager._enableBlend()
        GlStateManager._blendFuncSeparate(770, 771, 1, 0)
    }

    override fun getTranslateY(height: Int, windowScaleFactor: Int): Float = height / 2f

    override fun getTextureLabel(): String = "xenaddons_nvg"

    companion object {
        private fun directStateAccess(): DirectStateAccess? {
            return runCatching {
                val device = RenderSystem.getDevice()
                val backendField = device.javaClass.getDeclaredField("backend")
                backendField.isAccessible = true
                val backend = backendField.get(device)
                val method = backend.javaClass.getDeclaredMethod("directStateAccess")
                method.isAccessible = true
                method.invoke(backend) as? DirectStateAccess
            }.getOrNull()
        }
    }
}
