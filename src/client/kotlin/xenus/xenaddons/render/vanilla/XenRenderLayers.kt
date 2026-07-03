package xenus.xenaddons.render.vanilla

import net.minecraft.client.renderer.rendertype.RenderType
import xenus.xenaddons.compat.XenWorldRenderCompat

object XenRenderLayers {
    val filled: RenderType
        get() = XenWorldRenderCompat.normalFilledBoxLayer()

    val quads: RenderType
        get() = XenWorldRenderCompat.normalQuadLayer()

    val lines: RenderType
        get() = XenWorldRenderCompat.normalLineLayer()

    val seeThroughFilled: RenderType
        get() = XenWorldRenderCompat.seeThroughFilledLayer()

    val seeThroughLines: RenderType
        get() = XenWorldRenderCompat.seeThroughLineLayer()
}
