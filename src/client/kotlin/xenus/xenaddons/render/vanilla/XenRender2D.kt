package xenus.xenaddons.render.vanilla

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import xenus.xenaddons.compat.XenDrawContextCompat
import kotlin.math.max

object XenRender2D {
    private val mc: Minecraft
        get() = Minecraft.getInstance()

    fun rect(context: GuiGraphicsExtractor, x: Number, y: Number, width: Number, height: Number, color: Int) {
        val ix = x.toInt()
        val iy = y.toInt()
        context.fill(ix, iy, ix + width.toInt(), iy + height.toInt(), color)
    }

    fun border(
        context: GuiGraphicsExtractor,
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
        context: GuiGraphicsExtractor,
        text: String,
        x: Number,
        y: Number,
        color: Int = 0xFFFFFFFF.toInt(),
        shadow: Boolean = true,
        textRenderer: Font = mc.font
    ) {
        context.text(textRenderer, text, x.toInt(), y.toInt(), color, shadow)
    }

    fun centeredText(
        context: GuiGraphicsExtractor,
        text: String,
        centerX: Number,
        y: Number,
        color: Int = 0xFFFFFFFF.toInt(),
        shadow: Boolean = true,
        textRenderer: Font = mc.font
    ) {
        text(context, text, centerX.toInt() - textRenderer.width(text) / 2, y, color, shadow, textRenderer)
    }

    fun texture(
        context: GuiGraphicsExtractor,
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

    fun item(context: GuiGraphicsExtractor, item: ItemStack, x: Number, y: Number, scale: Float = 1f) {
        if (item.isEmpty) return

        val matrices = context.pose()
        matrices.pushMatrix()
        matrices.translate(x.toFloat(), y.toFloat())
        if (scale != 1f) matrices.scale(scale, scale)
        context.item(item, 0, 0)
        matrices.popMatrix()
    }

    fun verticalGradient(
        context: GuiGraphicsExtractor,
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

    fun width(text: String, textRenderer: Font = mc.font): Int =
        text.lines().maxOfOrNull(textRenderer::width) ?: 0

    fun height(text: String, textRenderer: Font = mc.font): Int =
        max(1, text.count { it == '\n' } + 1) * textRenderer.lineHeight
}
