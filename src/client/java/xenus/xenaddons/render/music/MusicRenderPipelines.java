package xenus.xenaddons.render.music;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import xenus.xenaddons.XenAddons;

public final class MusicRenderPipelines {
    private static final int FIRST_CUSTOM_ELEMENT_ID = 7;
    private static final int CUSTOM_ELEMENT_COUNT = 3;

    public static VertexFormatElement CUSTOM_DATA;
    public static VertexFormatElement COLOR_2;
    public static VertexFormatElement SHADER_PROPERTIES;

    public static VertexFormat FORMAT;

    public static RenderPipeline ROUNDED_RECT;
    public static RenderPipeline LIQUID_GLASS;
    public static RenderPipeline IMAGE;
    public static RenderPipeline MSDF_FONT;

    private static volatile boolean initialized;
    private static volatile boolean available;

    private MusicRenderPipelines() {}

    private static RenderPipeline register(String name, boolean textured, VertexFormat format) {
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(id("pipeline/" + name))
            .withVertexShader(id("core/" + name))
            .withFragmentShader(id("core/" + name))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexFormat(format, VertexFormat.Mode.QUADS);
        if (textured) {
            builder.withSampler("Sampler0");
        }
        return RenderPipelines.register(builder.build());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("xenaddons", path);
    }

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        try {
            int[] elementIds = findAvailableElementIds();
            VertexFormatElement customData = VertexFormatElement.register(
                elementIds[0],
                0,
                VertexFormatElement.Type.FLOAT,
                false,
                4
            );
            VertexFormatElement color2 = VertexFormatElement.register(
                elementIds[1],
                0,
                VertexFormatElement.Type.UBYTE,
                true,
                4
            );
            VertexFormatElement shaderProperties = VertexFormatElement.register(
                elementIds[2],
                0,
                VertexFormatElement.Type.FLOAT,
                false,
                4
            );

            VertexFormat format = VertexFormat.builder()
                .add("Position", VertexFormatElement.POSITION)
                .add("Color", VertexFormatElement.COLOR)
                .add("UV0", VertexFormatElement.UV0)
                .add("CustomData", customData)
                .add("Color2", color2)
                .add("ShadowProps", shaderProperties)
                .build();

            RenderPipeline roundedRect = register("rounded_rect", false, format);
            RenderPipeline liquidGlass = register("liquid_glass", true, format);
            RenderPipeline image = register("image", true, format);
            RenderPipeline msdfFont = register("msdf_font", true, format);

            CUSTOM_DATA = customData;
            COLOR_2 = color2;
            SHADER_PROPERTIES = shaderProperties;
            FORMAT = format;
            ROUNDED_RECT = roundedRect;
            LIQUID_GLASS = liquidGlass;
            IMAGE = image;
            MSDF_FONT = msdfFont;
            available = true;

            XenAddons.LOGGER.info(
                "Music render pipelines initialized with vertex element IDs {}, {}, and {}",
                elementIds[0],
                elementIds[1],
                elementIds[2]
            );
        } catch (RuntimeException exception) {
            XenAddons.LOGGER.error(
                "Music render pipelines could not be initialized; using the standard Spotify display instead",
                exception
            );
        }
    }

    public static boolean isAvailable() {
        init();
        return available;
    }

    private static int[] findAvailableElementIds() {
        int[] result = new int[CUSTOM_ELEMENT_COUNT];
        int found = 0;

        // Custom vertex element IDs are global across every installed mod. Search
        // from the high end to avoid the low IDs most mods allocate first.
        for (int id = VertexFormatElement.MAX_COUNT - 1; id >= FIRST_CUSTOM_ELEMENT_ID; id--) {
            if (VertexFormatElement.byId(id) == null) {
                result[found++] = id;
                if (found == result.length) return result;
            }
        }

        throw new IllegalStateException(
            "Not enough free vertex format element IDs (needed " + CUSTOM_ELEMENT_COUNT + ", found " + found + ')'
        );
    }
}
