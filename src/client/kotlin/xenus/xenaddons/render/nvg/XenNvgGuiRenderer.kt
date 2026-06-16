package xenus.xenaddons.render.nvg

import net.minecraft.client.gui.DrawContext
import xenus.xenaddons.mixin.client.AccessorDrawContext

object XenNvgGuiRenderer {
    fun draw(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        renderContent: () -> Unit
    ) {
        if (width <= 0 || height <= 0) return

        val state = XenNvgRenderState(
            x,
            y,
            x + width,
            y + height,
            1f,
            null,
            Runnable(renderContent)
        )

        (context as AccessorDrawContext).`xenaddons$getState`().addSpecialElement(state)
    }
}
