package xenus.xenaddons.render.music;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class MusicRenderPipelines {
    public static final VertexFormatElement CUSTOM_DATA =
        VertexFormatElement.register(7, 0, VertexFormatElement.Type.FLOAT, false, 4);
    public static final VertexFormatElement COLOR_2 =
        VertexFormatElement.register(8, 0, VertexFormatElement.Type.UBYTE, true, 4);
    public static final VertexFormatElement SHADER_PROPERTIES =
        VertexFormatElement.register(9, 0, VertexFormatElement.Type.FLOAT, false, 4);

    public static final VertexFormat FORMAT = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("Color", VertexFormatElement.COLOR)
        .add("UV0", VertexFormatElement.UV0)
        .add("CustomData", CUSTOM_DATA)
        .add("Color2", COLOR_2)
        .add("ShadowProps", SHADER_PROPERTIES)
        .build();

    public static final RenderPipeline ROUNDED_RECT = register("rounded_rect", false);
    public static final RenderPipeline LIQUID_GLASS = register("liquid_glass", true);
    public static final RenderPipeline IMAGE = register("image", true);
    public static final RenderPipeline MSDF_FONT = register("msdf_font", true);

    private MusicRenderPipelines() {}

    private static RenderPipeline register(String name, boolean textured) {
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(id("pipeline/" + name))
            .withVertexShader(id("core/" + name))
            .withFragmentShader(id("core/" + name))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexFormat(FORMAT, VertexFormat.Mode.QUADS);
        if (textured) {
            builder.withSampler("Sampler0");
        }
        return RenderPipelines.register(builder.build());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("xenaddons", path);
    }

    public static void init() {
        // Forces static pipeline registration during client initialization.
    }
}
