package xenus.xenaddons.render.vanilla

import net.minecraft.client.render.RenderLayer
import xenus.xenaddons.compat.XenWorldRenderCompat

object XenRenderLayers {
    val filled: RenderLayer
        get() = XenWorldRenderCompat.normalFilledBoxLayer()

    val quads: RenderLayer
        get() = XenWorldRenderCompat.normalQuadLayer()

    val lines: RenderLayer
        get() = XenWorldRenderCompat.normalLineLayer()

    val seeThroughFilled: RenderLayer
        get() = XenWorldRenderCompat.seeThroughFilledLayer()

    val seeThroughLines: RenderLayer
        get() = XenWorldRenderCompat.seeThroughLineLayer()
}
