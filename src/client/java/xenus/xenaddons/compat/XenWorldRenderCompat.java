package xenus.xenaddons.compat;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;

public final class XenWorldRenderCompat {
    private static final RenderPipeline SEE_THROUGH_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(XenIdentifierCompat.xen("see_through_overlay"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .build()
    );
    private static final RenderLayer SEE_THROUGH_LAYER = RenderLayer.of(
        "secretroutes_see_through",
        1536,
        SEE_THROUGH_PIPELINE,
        RenderLayer.MultiPhaseParameters.builder()
            .target(RenderLayer.MAIN_TARGET)
            .build(false)
    );
    private static final RenderPipeline SEE_THROUGH_LINE_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(XenIdentifierCompat.xen("see_through_lines"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.LINES)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .build()
    );
    private static final RenderLayer SEE_THROUGH_LINE_LAYER = RenderLayer.of(
        "secretroutes_see_through_lines",
        1536,
        SEE_THROUGH_LINE_PIPELINE,
        RenderLayer.MultiPhaseParameters.builder()
            .target(RenderLayer.MAIN_TARGET)
            .build(false)
    );

    private XenWorldRenderCompat() {}

    public static RenderLayer seeThroughFilledLayer() {
        return SEE_THROUGH_LAYER;
    }

    public static RenderLayer seeThroughLineLayer() {
        return SEE_THROUGH_LINE_LAYER;
    }

    public static RenderLayer normalFilledBoxLayer() {
        return RenderLayer.getDebugFilledBox();
    }

    public static RenderLayer normalQuadLayer() {
        return RenderLayer.getDebugQuads();
    }

    public static RenderLayer normalLineLayer() {
        return RenderLayer.getLines();
    }

    public static void setLineWidth(float width) {
        RenderSystem.lineWidth(width);
    }
}
