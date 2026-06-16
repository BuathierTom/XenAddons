package xenus.xenaddons.render.vanilla

import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier
import xenus.xenaddons.compat.XenDrawContextCompat
import kotlin.math.max

object XenRender2D {
    private val mc: MinecraftClient
        get() = MinecraftClient.getInstance()

    fun rect(context: DrawContext, x: Number, y: Number, width: Number, height: Number, color: Int) {
        val ix = x.toInt()
        val iy = y.toInt()
        context.fill(ix, iy, ix + width.toInt(), iy + height.toInt(), color)
    }

    fun border(
        context: DrawContext,
        x: Number,
        y: Number,
        width: Number,
        height: Number,
        color: Int,
        thickness: Number = 1
    ) {
        val t = max(1, thickness.toInt())
        rect(context, x, y, width, t, color)
        rect(context, x, y.toInt() + height.toInt() - t, width, t, color)
        rect(context, x, y, t, height, color)
        rect(context, x.toInt() + width.toInt() - t, y, t, height, color)
    }

    fun text(
        context: DrawContext,
        text: String,
        x: Number,
        y: Number,
        color: Int = 0xFFFFFFFF.toInt(),
        shadow: Boolean = true,
        textRenderer: TextRenderer = mc.textRenderer
    ) {
        if (shadow) {
            context.drawTextWithShadow(textRenderer, text, x.toInt(), y.toInt(), color)
        } else {
            context.drawText(textRenderer, text, x.toInt(), y.toInt(), color, false)
        }
    }

    fun centeredText(
        context: DrawContext,
        text: String,
        centerX: Number,
        y: Number,
        color: Int = 0xFFFFFFFF.toInt(),
        shadow: Boolean = true,
        textRenderer: TextRenderer = mc.textRenderer
    ) {
        text(context, text, centerX.toInt() - textRenderer.getWidth(text) / 2, y, color, shadow, textRenderer)
    }

    fun texture(
        context: DrawContext,
        texture: Identifier,
        x: Number,
        y: Number,
        width: Number,
        height: Number,
        textureWidth: Number = width,
        textureHeight: Number = height
    ) {
        XenDrawContextCompat.drawTexture(
            context,
            texture,
            x.toInt(),
            y.toInt(),
            0f,
            0f,
            width.toInt(),
            height.toInt(),
            textureWidth.toInt(),
            textureHeight.toInt()
        )
    }

    fun item(context: DrawContext, item: ItemStack, x: Number, y: Number, scale: Float = 1f) {
        if (item.isEmpty) return

        val matrices = context.matrices
        matrices.pushMatrix()
        matrices.translate(x.toFloat(), y.toFloat())
        if (scale != 1f) matrices.scale(scale, scale)
        context.drawItem(item, 0, 0)
        matrices.popMatrix()
    }

    fun verticalGradient(
        context: DrawContext,
        x: Number,
        y: Number,
        width: Number,
        height: Number,
        topColor: Int,
        bottomColor: Int
    ) {
        val ix = x.toInt()
        val iy = y.toInt()
        context.fillGradient(ix, iy, ix + width.toInt(), iy + height.toInt(), topColor, bottomColor)
    }

    fun width(text: String, textRenderer: TextRenderer = mc.textRenderer): Int =
        text.lines().maxOfOrNull(textRenderer::getWidth) ?: 0

    fun height(text: String, textRenderer: TextRenderer = mc.textRenderer): Int =
        max(1, text.count { it == '\n' } + 1) * textRenderer.fontHeight
}
