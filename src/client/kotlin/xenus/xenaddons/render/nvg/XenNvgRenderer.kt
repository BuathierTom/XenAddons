package xenus.xenaddons.render.nvg

import xenus.xenaddons.render.XenFont
import xenus.xenaddons.render.XenGradient
import xenus.xenaddons.render.XenImage

import net.minecraft.client.Minecraft
import org.lwjgl.nanovg.NVGColor
import org.lwjgl.nanovg.NVGPaint
import org.lwjgl.nanovg.NanoSVG.nsvgCreateRasterizer
import org.lwjgl.nanovg.NanoSVG.nsvgDelete
import org.lwjgl.nanovg.NanoSVG.nsvgDeleteRasterizer
import org.lwjgl.nanovg.NanoSVG.nsvgParse
import org.lwjgl.nanovg.NanoSVG.nsvgRasterize
import org.lwjgl.nanovg.NanoVG.NVG_ALIGN_LEFT
import org.lwjgl.nanovg.NanoVG.NVG_ALIGN_TOP
import org.lwjgl.nanovg.NanoVG.NVG_HOLE
import org.lwjgl.nanovg.NanoVG.NVG_IMAGE_NEAREST
import org.lwjgl.nanovg.NanoVG.nvgArcTo
import org.lwjgl.nanovg.NanoVG.nvgBeginFrame
import org.lwjgl.nanovg.NanoVG.nvgBeginPath
import org.lwjgl.nanovg.NanoVG.nvgBoxGradient
import org.lwjgl.nanovg.NanoVG.nvgCircle
import org.lwjgl.nanovg.NanoVG.nvgClosePath
import org.lwjgl.nanovg.NanoVG.nvgCreateFontMem
import org.lwjgl.nanovg.NanoVG.nvgCreateImageRGBA
import org.lwjgl.nanovg.NanoVG.nvgDeleteImage
import org.lwjgl.nanovg.NanoVG.nvgEndFrame
import org.lwjgl.nanovg.NanoVG.nvgFill
import org.lwjgl.nanovg.NanoVG.nvgFillColor
import org.lwjgl.nanovg.NanoVG.nvgFillPaint
import org.lwjgl.nanovg.NanoVG.nvgFontFaceId
import org.lwjgl.nanovg.NanoVG.nvgFontSize
import org.lwjgl.nanovg.NanoVG.nvgGlobalAlpha
import org.lwjgl.nanovg.NanoVG.nvgImagePattern
import org.lwjgl.nanovg.NanoVG.nvgLineTo
import org.lwjgl.nanovg.NanoVG.nvgLinearGradient
import org.lwjgl.nanovg.NanoVG.nvgMoveTo
import org.lwjgl.nanovg.NanoVG.nvgPathWinding
import org.lwjgl.nanovg.NanoVG.nvgRGBA
import org.lwjgl.nanovg.NanoVG.nvgRect
import org.lwjgl.nanovg.NanoVG.nvgResetScissor
import org.lwjgl.nanovg.NanoVG.nvgRestore
import org.lwjgl.nanovg.NanoVG.nvgRotate
import org.lwjgl.nanovg.NanoVG.nvgRoundedRect
import org.lwjgl.nanovg.NanoVG.nvgSave
import org.lwjgl.nanovg.NanoVG.nvgScale
import org.lwjgl.nanovg.NanoVG.nvgScissor
import org.lwjgl.nanovg.NanoVG.nvgStroke
import org.lwjgl.nanovg.NanoVG.nvgStrokeColor
import org.lwjgl.nanovg.NanoVG.nvgStrokeWidth
import org.lwjgl.nanovg.NanoVG.nvgText
import org.lwjgl.nanovg.NanoVG.nvgTextAlign
import org.lwjgl.nanovg.NanoVG.nvgTextBounds
import org.lwjgl.nanovg.NanoVG.nvgTextBox
import org.lwjgl.nanovg.NanoVG.nvgTextBoxBounds
import org.lwjgl.nanovg.NanoVG.nvgTextLineHeight
import org.lwjgl.nanovg.NanoVG.nvgTranslate
import org.lwjgl.nanovg.NanoVGGL3.NVG_ANTIALIAS
import org.lwjgl.nanovg.NanoVGGL3.NVG_IMAGE_NODELETE
import org.lwjgl.nanovg.NanoVGGL3.NVG_STENCIL_STROKES
import org.lwjgl.nanovg.NanoVGGL3.nvgCreate
import org.lwjgl.nanovg.NanoVGGL3.nvglCreateImageFromHandle
import org.lwjgl.opengl.GL33C
import org.lwjgl.stb.STBImage.stbi_load_from_memory
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

object XenNvgRenderer {
    private val nvgPaint = NVGPaint.malloc()
    private val nvgColor = NVGColor.malloc()
    private val nvgColor2 = NVGColor.malloc()

    val defaultFont = XenFont(
        "Default",
        requireNotNull(XenNvgRenderer::class.java.getResourceAsStream("/assets/xenaddons/render/font.ttf")) {
            "Missing XenAddons render font resource"
        }
    )

    private val fontMap = HashMap<XenFont, NVGFont>()
    private val fontBounds = FloatArray(4)
    private val images = HashMap<XenImage, NVGImage>()

    private var scissor: Scissor? = null
    private var drawing = false
    private val vg: Long

    init {
        vg = nvgCreate(NVG_ANTIALIAS or NVG_STENCIL_STROKES)
        require(vg != -1L) { "Failed to initialize NanoVG" }
    }

    fun devicePixelRatio(): Float {
        val window = Minecraft.getInstance().window
        return if (window.screenWidth == 0) 1f else window.width.toFloat() / window.screenWidth.toFloat()
    }

    fun beginFrame(width: Float, height: Float) {
        beginFrame(width, height, devicePixelRatio())
    }

    fun beginFrame(width: Float, height: Float, devicePixelRatio: Float) {
        if (drawing) throw IllegalStateException("NanoVG frame already started")
        val dpr = devicePixelRatio.coerceAtLeast(1f)
        nvgBeginFrame(vg, width / dpr, height / dpr, dpr)
        nvgTextAlign(vg, NVG_ALIGN_LEFT or NVG_ALIGN_TOP)
        drawing = true
    }

    fun endFrame() {
        if (!drawing) throw IllegalStateException("NanoVG frame not started")
        nvgEndFrame(vg)
        drawing = false
    }

    fun push() = nvgSave(vg)

    fun pop() = nvgRestore(vg)

    fun scale(x: Float, y: Float) = nvgScale(vg, x, y)

    fun translate(x: Float, y: Float) = nvgTranslate(vg, x, y)

    fun rotate(amount: Float) = nvgRotate(vg, amount)

    fun globalAlpha(amount: Float) = nvgGlobalAlpha(vg, amount.coerceIn(0f, 1f))

    fun pushScissor(x: Float, y: Float, width: Float, height: Float) {
        scissor = Scissor(scissor, x, y, x + width, y + height)
        scissor?.applyScissor()
    }

    fun popScissor() {
        nvgResetScissor(vg)
        scissor = scissor?.previous
        scissor?.applyScissor()
    }

    fun line(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color: Int) {
        nvgBeginPath(vg)
        nvgMoveTo(vg, x1, y1)
        nvgLineTo(vg, x2, y2)
        nvgStrokeWidth(vg, thickness)
        color(color)
        nvgStrokeColor(vg, nvgColor)
        nvgStroke(vg)
    }

    fun drawHalfRoundedRect(x: Float, y: Float, width: Float, height: Float, color: Int, radius: Float, roundTop: Boolean) {
        nvgBeginPath(vg)
        if (roundTop) {
            nvgMoveTo(vg, x, y + height)
            nvgLineTo(vg, x + width, y + height)
            nvgLineTo(vg, x + width, y + radius)
            nvgArcTo(vg, x + width, y, x + width - radius, y, radius)
            nvgLineTo(vg, x + radius, y)
            nvgArcTo(vg, x, y, x, y + radius, radius)
            nvgLineTo(vg, x, y + height)
        } else {
            nvgMoveTo(vg, x, y)
            nvgLineTo(vg, x + width, y)
            nvgLineTo(vg, x + width, y + height - radius)
            nvgArcTo(vg, x + width, y + height, x + width - radius, y + height, radius)
            nvgLineTo(vg, x + radius, y + height)
            nvgArcTo(vg, x, y + height, x, y + height - radius, radius)
            nvgLineTo(vg, x, y)
        }
        nvgClosePath(vg)
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgFill(vg)
    }

    fun rect(x: Float, y: Float, width: Float, height: Float, color: Int, radius: Float) {
        nvgBeginPath(vg)
        nvgRoundedRect(vg, x, y, width, height + 0.5f, radius)
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgFill(vg)
    }

    fun rect(x: Float, y: Float, width: Float, height: Float, color: Int) {
        nvgBeginPath(vg)
        nvgRect(vg, x, y, width, height + 0.5f)
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgFill(vg)
    }

    fun beveledRect(x: Float, y: Float, width: Float, height: Float, color: Int, cut: Float) {
        beveledPath(x, y, width, height, cut)
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgFill(vg)
    }

    fun hollowRect(x: Float, y: Float, width: Float, height: Float, thickness: Float, color: Int, radius: Float) {
        nvgBeginPath(vg)
        nvgRoundedRect(vg, x, y, width, height, radius)
        nvgStrokeWidth(vg, thickness)
        nvgPathWinding(vg, NVG_HOLE)
        color(color)
        nvgStrokeColor(vg, nvgColor)
        nvgStroke(vg)
    }

    fun hollowBeveledRect(x: Float, y: Float, width: Float, height: Float, thickness: Float, color: Int, cut: Float) {
        beveledPath(x, y, width, height, cut)
        nvgStrokeWidth(vg, thickness)
        color(color)
        nvgStrokeColor(vg, nvgColor)
        nvgStroke(vg)
    }

    fun gradientRect(x: Float, y: Float, width: Float, height: Float, color1: Int, color2: Int, gradient: XenGradient, radius: Float = 0f) {
        nvgBeginPath(vg)
        if (radius > 0f) {
            nvgRoundedRect(vg, x, y, width, height, radius)
        } else {
            nvgRect(vg, x, y, width, height)
        }
        gradient(color1, color2, x, y, width, height, gradient)
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    fun dropShadow(x: Float, y: Float, width: Float, height: Float, blur: Float, spread: Float, radius: Float) {
        nvgRGBA(0, 0, 0, 125.toByte(), nvgColor)
        nvgRGBA(0, 0, 0, 0, nvgColor2)
        nvgBoxGradient(
            vg,
            x - spread,
            y - spread,
            width + 2f * spread,
            height + 2f * spread,
            radius + spread,
            blur,
            nvgColor,
            nvgColor2,
            nvgPaint
        )
        nvgBeginPath(vg)
        nvgRoundedRect(vg, x - spread - blur, y - spread - blur, width + 2f * spread + 2f * blur, height + 2f * spread + 2f * blur, radius + spread)
        nvgRoundedRect(vg, x, y, width, height, radius)
        nvgPathWinding(vg, NVG_HOLE)
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    fun circle(x: Float, y: Float, radius: Float, color: Int) {
        nvgBeginPath(vg)
        nvgCircle(vg, x, y, radius)
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgFill(vg)
    }

    fun text(text: String, x: Float, y: Float, size: Float, color: Int, font: XenFont = defaultFont) {
        nvgFontSize(vg, size)
        nvgFontFaceId(vg, getFontId(font))
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgText(vg, x, y + 0.5f, text)
    }

    fun textShadow(text: String, x: Float, y: Float, size: Float, color: Int, font: XenFont = defaultFont) {
        nvgFontFaceId(vg, getFontId(font))
        nvgFontSize(vg, size)
        color(0xFF000000.toInt())
        nvgFillColor(vg, nvgColor)
        nvgText(vg, round(x + 2f), round(y + 2f), text)

        color(color)
        nvgFillColor(vg, nvgColor)
        nvgText(vg, round(x), round(y), text)
    }

    fun textWidth(text: String, size: Float, font: XenFont = defaultFont): Float {
        nvgFontSize(vg, size)
        nvgFontFaceId(vg, getFontId(font))
        return nvgTextBounds(vg, 0f, 0f, text, fontBounds)
    }

    fun drawWrappedString(text: String, x: Float, y: Float, width: Float, size: Float, color: Int, font: XenFont = defaultFont, lineHeight: Float = 1f) {
        nvgFontSize(vg, size)
        nvgFontFaceId(vg, getFontId(font))
        nvgTextLineHeight(vg, lineHeight)
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgTextBox(vg, x, y, width, text)
    }

    fun wrappedTextBounds(text: String, width: Float, size: Float, font: XenFont = defaultFont, lineHeight: Float = 1f): FloatArray {
        val bounds = FloatArray(4)
        nvgFontSize(vg, size)
        nvgFontFaceId(vg, getFontId(font))
        nvgTextLineHeight(vg, lineHeight)
        nvgTextBoxBounds(vg, 0f, 0f, width, text, bounds)
        return bounds
    }

    fun createNVGImage(textureId: Int, textureWidth: Int, textureHeight: Int): Int {
        GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, textureId)
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_NEAREST)
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_NEAREST)
        return nvglCreateImageFromHandle(vg, textureId, textureWidth, textureHeight, NVG_IMAGE_NEAREST or NVG_IMAGE_NODELETE)
    }

    fun image(image: Int, textureWidth: Int, textureHeight: Int, subX: Int, subY: Int, subW: Int, subH: Int, x: Float, y: Float, width: Float, height: Float, radius: Float = 0f) {
        if (image == -1) return

        val sx = subX.toFloat() / textureWidth
        val sy = subY.toFloat() / textureHeight
        val sw = subW.toFloat() / textureWidth
        val sh = subH.toFloat() / textureHeight

        val imageWidth = width / sw
        val imageHeight = height / sh
        val imageX = x - imageWidth * sx
        val imageY = y - imageHeight * sy

        nvgImagePattern(vg, imageX, imageY, imageWidth, imageHeight, 0f, image, 1f, nvgPaint)
        nvgBeginPath(vg)
        if (radius > 0f) {
            nvgRoundedRect(vg, x, y, width, height + 0.5f, radius)
        } else {
            nvgRect(vg, x, y, width, height + 0.5f)
        }
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    fun image(image: XenImage, x: Float, y: Float, width: Float, height: Float, radius: Float = 0f) {
        nvgImagePattern(vg, x, y, width, height, 0f, getImage(image), 1f, nvgPaint)
        nvgBeginPath(vg)
        if (radius > 0f) {
            nvgRoundedRect(vg, x, y, width, height + 0.5f, radius)
        } else {
            nvgRect(vg, x, y, width, height + 0.5f)
        }
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    fun createImage(resourcePath: String): XenImage {
        val image = images.keys.find { it.identifier == resourcePath } ?: XenImage(resourcePath)
        if (image.isSvg) {
            images.getOrPut(image) { NVGImage(0, loadSvg(image)) }.count++
        } else {
            images.getOrPut(image) { NVGImage(0, loadImage(image)) }.count++
        }
        return image
    }

    fun deleteImage(image: XenImage) {
        val nvgImage = images[image] ?: return
        nvgImage.count--
        if (nvgImage.count == 0) {
            nvgDeleteImage(vg, nvgImage.nvg)
            images.remove(image)
        }
    }

    private fun getImage(image: XenImage): Int =
        images[image]?.nvg ?: throw IllegalStateException("XenImage (${image.identifier}) is not registered")

    private fun loadImage(image: XenImage): Int {
        val width = IntArray(1)
        val height = IntArray(1)
        val channels = IntArray(1)
        val buffer = stbi_load_from_memory(image.buffer(), width, height, channels, 4)
            ?: throw IllegalStateException("Failed to load image ${image.identifier}")
        return nvgCreateImageRGBA(vg, width[0], height[0], 0, buffer)
    }

    private fun loadSvg(image: XenImage): Int {
        val source = image.openFreshStream().use { it.bufferedReader().readText() }
        val svg = nsvgParse(source, "px", 96f) ?: throw IllegalStateException("Failed to parse ${image.identifier}")
        val width = svg.width().toInt()
        val height = svg.height().toInt()
        val buffer = memAlloc(width * height * 4)

        try {
            val rasterizer = nsvgCreateRasterizer()
            nsvgRasterize(rasterizer, svg, 0f, 0f, 1f, buffer, width, height, width * 4)
            val created = nvgCreateImageRGBA(vg, width, height, 0, buffer)
            nsvgDeleteRasterizer(rasterizer)
            return created
        } finally {
            nsvgDelete(svg)
            memFree(buffer)
        }
    }

    private fun color(color: Int) {
        nvgRGBA(color.red.toByte(), color.green.toByte(), color.blue.toByte(), color.alpha.toByte(), nvgColor)
    }

    private fun color(color1: Int, color2: Int) {
        nvgRGBA(color1.red.toByte(), color1.green.toByte(), color1.blue.toByte(), color1.alpha.toByte(), nvgColor)
        nvgRGBA(color2.red.toByte(), color2.green.toByte(), color2.blue.toByte(), color2.alpha.toByte(), nvgColor2)
    }

    private fun gradient(color1: Int, color2: Int, x: Float, y: Float, width: Float, height: Float, direction: XenGradient) {
        color(color1, color2)
        when (direction) {
            XenGradient.LeftToRight -> nvgLinearGradient(vg, x, y, x + width, y, nvgColor, nvgColor2, nvgPaint)
            XenGradient.TopToBottom -> nvgLinearGradient(vg, x, y, x, y + height, nvgColor, nvgColor2, nvgPaint)
        }
    }

    private fun beveledPath(x: Float, y: Float, width: Float, height: Float, cut: Float) {
        val c = cut.coerceIn(0f, min(width, height) / 2f)
        nvgBeginPath(vg)
        nvgMoveTo(vg, x + c, y)
        nvgLineTo(vg, x + width - c, y)
        nvgLineTo(vg, x + width, y + c)
        nvgLineTo(vg, x + width, y + height - c)
        nvgLineTo(vg, x + width - c, y + height)
        nvgLineTo(vg, x + c, y + height)
        nvgLineTo(vg, x, y + height - c)
        nvgLineTo(vg, x, y + c)
        nvgClosePath(vg)
    }

    private fun getFontId(font: XenFont): Int {
        return fontMap.getOrPut(font) {
            val buffer = font.buffer()
            NVGFont(nvgCreateFontMem(vg, font.name, buffer, false), buffer)
        }.id
    }

    private val Int.alpha: Int get() = (this ushr 24) and 0xFF
    private val Int.red: Int get() = (this ushr 16) and 0xFF
    private val Int.green: Int get() = (this ushr 8) and 0xFF
    private val Int.blue: Int get() = this and 0xFF

    private class Scissor(
        val previous: Scissor?,
        val x: Float,
        val y: Float,
        val maxX: Float,
        val maxY: Float
    ) {
        fun applyScissor() {
            if (previous == null) {
                nvgScissor(vg, x, y, maxX - x, maxY - y)
                return
            }

            val clippedX = max(x, previous.x)
            val clippedY = max(y, previous.y)
            val clippedWidth = max(0f, min(maxX, previous.maxX) - clippedX)
            val clippedHeight = max(0f, min(maxY, previous.maxY) - clippedY)
            nvgScissor(vg, clippedX, clippedY, clippedWidth, clippedHeight)
        }
    }

    private data class NVGImage(var count: Int, val nvg: Int)
    private data class NVGFont(val id: Int, val buffer: ByteBuffer)
}
