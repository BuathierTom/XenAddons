package xenus.xenaddons.compat;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public final class XenWorldRenderCompat {
    private XenWorldRenderCompat() {}

    public static RenderType seeThroughFilledLayer() {
        return RenderTypes.debugFilledBox();
    }

    public static RenderType seeThroughLineLayer() {
        return RenderTypes.lines();
    }

    public static RenderType normalFilledBoxLayer() {
        return RenderTypes.debugFilledBox();
    }

    public static RenderType normalQuadLayer() {
        return RenderTypes.debugQuads();
    }

    public static RenderType normalLineLayer() {
        return RenderTypes.lines();
    }

    public static void setLineWidth(float width) {
        // Line width is written per vertex via VertexConsumer#lineWidth in 1.21.11.
    }
}
