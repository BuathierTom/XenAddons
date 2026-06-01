package xenus.xenaddons.compat;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.OutputTarget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexFormats;

public final class XenWorldRenderCompat {
    private static final RenderPipeline SEE_THROUGH_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(XenIdentifierCompat.xen("see_through_overlay"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .build()
    );
    private static final RenderLayer SEE_THROUGH_LAYER = RenderLayer.of(
        "secretroutes_see_through",
        RenderSetup.builder(SEE_THROUGH_PIPELINE)
            .outputTarget(OutputTarget.MAIN_TARGET)
            .expectedBufferSize(1536)
            .build()
    );
    private static final RenderPipeline SEE_THROUGH_LINE_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
            .withLocation(XenIdentifierCompat.xen("see_through_lines"))
            .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH, VertexFormat.DrawMode.LINES)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .build()
    );
    private static final RenderLayer SEE_THROUGH_LINE_LAYER = RenderLayer.of(
        "secretroutes_see_through_lines",
        RenderSetup.builder(SEE_THROUGH_LINE_PIPELINE)
            .outputTarget(OutputTarget.MAIN_TARGET)
            .expectedBufferSize(1536)
            .build()
    );

    private XenWorldRenderCompat() {}

    public static RenderLayer seeThroughFilledLayer() {
        return SEE_THROUGH_LAYER;
    }

    public static RenderLayer seeThroughLineLayer() {
        return SEE_THROUGH_LINE_LAYER;
    }

    public static RenderLayer normalFilledBoxLayer() {
        return RenderLayers.debugFilledBox();
    }

    public static RenderLayer normalQuadLayer() {
        return RenderLayers.debugQuads();
    }

    public static RenderLayer normalLineLayer() {
        return RenderLayers.lines();
    }

    public static void setLineWidth(float width) {
        // Line width is written per vertex via VertexConsumer#lineWidth in 1.21.11.
    }
}
