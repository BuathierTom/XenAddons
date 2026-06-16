package xenus.xenaddons.render

import xenus.xenaddons.render.nvg.XenNvgRenderer

object XenRenderer {
    val defaultFont: XenFont
        get() = XenNvgRenderer.defaultFont

    fun devicePixelRatio(): Float = XenNvgRenderer.devicePixelRatio()

    fun beginFrame(width: Float, height: Float) = XenNvgRenderer.beginFrame(width, height)

    fun beginFrame(width: Float, height: Float, devicePixelRatio: Float) =
        XenNvgRenderer.beginFrame(width, height, devicePixelRatio)

    fun endFrame() = XenNvgRenderer.endFrame()

    fun push() = XenNvgRenderer.push()

    fun pop() = XenNvgRenderer.pop()

    fun scale(x: Float, y: Float) = XenNvgRenderer.scale(x, y)

    fun translate(x: Float, y: Float) = XenNvgRenderer.translate(x, y)

    fun rotate(amount: Float) = XenNvgRenderer.rotate(amount)

    fun globalAlpha(amount: Float) = XenNvgRenderer.globalAlpha(amount)

    fun pushScissor(x: Float, y: Float, width: Float, height: Float) =
        XenNvgRenderer.pushScissor(x, y, width, height)

    fun popScissor() = XenNvgRenderer.popScissor()

    fun line(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color: Int) =
        XenNvgRenderer.line(x1, y1, x2, y2, thickness, color)

    fun drawHalfRoundedRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Int,
        radius: Float,
        roundTop: Boolean
    ) = XenNvgRenderer.drawHalfRoundedRect(x, y, width, height, color, radius, roundTop)

    fun rect(x: Float, y: Float, width: Float, height: Float, color: Int, radius: Float) =
        XenNvgRenderer.rect(x, y, width, height, color, radius)

    fun rect(x: Float, y: Float, width: Float, height: Float, color: Int) =
        XenNvgRenderer.rect(x, y, width, height, color)

    fun hollowRect(x: Float, y: Float, width: Float, height: Float, thickness: Float, color: Int, radius: Float) =
        XenNvgRenderer.hollowRect(x, y, width, height, thickness, color, radius)

    fun gradientRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color1: Int,
        color2: Int,
        gradient: XenGradient,
        radius: Float = 0f
    ) = XenNvgRenderer.gradientRect(x, y, width, height, color1, color2, gradient, radius)

    fun dropShadow(x: Float, y: Float, width: Float, height: Float, blur: Float, spread: Float, radius: Float) =
        XenNvgRenderer.dropShadow(x, y, width, height, blur, spread, radius)

    fun circle(x: Float, y: Float, radius: Float, color: Int) =
        XenNvgRenderer.circle(x, y, radius, color)

    fun text(text: String, x: Float, y: Float, size: Float, color: Int, font: XenFont = defaultFont) =
        XenNvgRenderer.text(text, x, y, size, color, font)

    fun textShadow(text: String, x: Float, y: Float, size: Float, color: Int, font: XenFont = defaultFont) =
        XenNvgRenderer.textShadow(text, x, y, size, color, font)

    fun textWidth(text: String, size: Float, font: XenFont = defaultFont): Float =
        XenNvgRenderer.textWidth(text, size, font)

    fun drawWrappedString(
        text: String,
        x: Float,
        y: Float,
        width: Float,
        size: Float,
        color: Int,
        font: XenFont = defaultFont,
        lineHeight: Float = 1f
    ) = XenNvgRenderer.drawWrappedString(text, x, y, width, size, color, font, lineHeight)

    fun wrappedTextBounds(
        text: String,
        width: Float,
        size: Float,
        font: XenFont = defaultFont,
        lineHeight: Float = 1f
    ): FloatArray = XenNvgRenderer.wrappedTextBounds(text, width, size, font, lineHeight)

    fun createNvgImage(textureId: Int, textureWidth: Int, textureHeight: Int): Int =
        XenNvgRenderer.createNVGImage(textureId, textureWidth, textureHeight)

    fun image(
        image: Int,
        textureWidth: Int,
        textureHeight: Int,
        subX: Int,
        subY: Int,
        subW: Int,
        subH: Int,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float = 0f
    ) = XenNvgRenderer.image(image, textureWidth, textureHeight, subX, subY, subW, subH, x, y, width, height, radius)

    fun image(image: XenImage, x: Float, y: Float, width: Float, height: Float, radius: Float = 0f) =
        XenNvgRenderer.image(image, x, y, width, height, radius)

    fun createImage(resourcePath: String): XenImage = XenNvgRenderer.createImage(resourcePath)

    fun deleteImage(image: XenImage) = XenNvgRenderer.deleteImage(image)
}
