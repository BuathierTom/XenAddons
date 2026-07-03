package xenus.xenaddons.render.nvg

import net.minecraft.client.gui.GuiGraphicsExtractor
import org.joml.Matrix3x2f

object XenNvgGuiRenderer {
    fun draw(
        context: GuiGraphicsExtractor,
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
            width,
            height,
            1f,
            Matrix3x2f(context.pose()),
            context.scissorStack.peek(),
            Runnable(renderContent)
        )

        context.guiRenderState.addPicturesInPictureState(state)
    }
}
