package xenus.xenaddons.render.nvg

//? if <26.2 {
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
//?} else {
/*import com.mojang.blaze3d.GpuFormat
import com.mojang.blaze3d.opengl.GlStateManager
import com.mojang.blaze3d.opengl.GlTexture
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
import net.minecraft.client.renderer.state.gui.BlitRenderState
import net.minecraft.client.renderer.state.gui.GuiRenderState
import org.lwjgl.opengl.GL33C

class XenNvgSpecialElementRenderer : PictureInPictureRenderer<XenNvgRenderState>() {
    private var texture: GpuTexture? = null
    private var textureView: GpuTextureView? = null
    private var framebuffer = 0
    private var framebufferTexture = 0

    override fun getRenderStateClass(): Class<XenNvgRenderState> = XenNvgRenderState::class.java

    override fun prepare(
        state: XenNvgRenderState,
        guiRenderState: GuiRenderState,
        featureRenderDispatcher: FeatureRenderDispatcher,
        guiScale: Int
    ) {
        val pixelWidth = (state.x1() - state.x0()) * guiScale
        val pixelHeight = (state.y1() - state.y0()) * guiScale
        if (pixelWidth <= 0 || pixelHeight <= 0) return

        val view = prepareTexture(pixelWidth, pixelHeight)
        val glTexture = view.texture() as? GlTexture ?: return

        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(view.texture(), GuiRenderer.CLEAR_COLOR)
        GlStateManager._glBindFramebuffer(GL33C.GL_FRAMEBUFFER, framebufferFor(glTexture.glId()))
        GlStateManager._viewport(0, 0, pixelWidth, pixelHeight)

        GL33C.glBindSampler(0, 0)
        XenNvgRenderer.beginFrame(pixelWidth.toFloat(), pixelHeight.toFloat())
        state.renderContent().run()
        XenNvgRenderer.endFrame()

        GlStateManager._glBindFramebuffer(GL33C.GL_FRAMEBUFFER, 0)
        GlStateManager._disableDepthTest()
        GlStateManager._disableCull()
        GlStateManager._disableScissorTest()
        GlStateManager._enableBlend(0)
        GlStateManager._blendFuncSeparate(1, 771, 1, 771)
        GlStateManager._colorMask(15)
        GlStateManager._activeTexture(GL33C.GL_TEXTURE0)
        GlStateManager._bindTexture(0)

        guiRenderState.addBlitToCurrentLayer(
            BlitRenderState(
                RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                TextureSetup.singleTexture(view, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                state.pose(),
                state.x0(),
                state.y0(),
                state.x1(),
                state.y1(),
                0f,
                1f,
                1f,
                0f,
                -1,
                state.scissorArea(),
                null
            )
        )
    }

    override fun renderToTexture(state: XenNvgRenderState, poseStack: PoseStack, submitNodeCollector: SubmitNodeCollector) {}

    override fun getTextureLabel(): String = "xenaddons_nvg"

    override fun close() {
        releaseTexture()
        if (framebuffer != 0) {
            GL33C.glDeleteFramebuffers(framebuffer)
            framebuffer = 0
            framebufferTexture = 0
        }
        super.close()
    }

    private fun prepareTexture(width: Int, height: Int): GpuTextureView {
        val current = texture
        val currentView = textureView
        if (current != null && currentView != null && current.getWidth(0) == width && current.getHeight(0) == height) {
            return currentView
        }

        releaseTexture()
        val device = RenderSystem.getDevice()
        val created = device.createTexture(
            "UI xenaddons_nvg texture",
            GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING or GpuTexture.USAGE_RENDER_ATTACHMENT,
            GpuFormat.RGBA8_UNORM,
            width,
            height,
            1,
            1
        )
        val createdView = device.createTextureView(created)
        texture = created
        textureView = createdView
        return createdView
    }

    private fun releaseTexture() {
        textureView?.close()
        textureView = null
        texture?.close()
        texture = null
    }

    private fun framebufferFor(textureId: Int): Int {
        if (framebuffer == 0) framebuffer = GL33C.glGenFramebuffers()
        if (framebufferTexture != textureId) {
            GlStateManager._glBindFramebuffer(GL33C.GL_FRAMEBUFFER, framebuffer)
            GL33C.glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0, GL33C.GL_TEXTURE_2D, textureId, 0)
            framebufferTexture = textureId
        }
        return framebuffer
    }
}
*///?}
