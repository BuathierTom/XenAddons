package xenus.xenaddons.render.music;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
//? if <26.2 {
import com.mojang.blaze3d.vertex.VertexFormatElement;
//?} else {
/*import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import net.minecraft.client.renderer.BindGroupLayouts;
*///?}
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import xenus.xenaddons.XenAddons;

public final class MusicRenderPipelines {
    //? if <26.2 {
    private static final int FIRST_CUSTOM_ELEMENT_ID = 7;
    private static final int CUSTOM_ELEMENT_COUNT = 3;

    public static VertexFormatElement CUSTOM_DATA;
    public static VertexFormatElement COLOR_2;
    public static VertexFormatElement SHADER_PROPERTIES;
    //?} else {
    /*public static final String CUSTOM_DATA = "CustomData";
    public static final String COLOR_2 = "Color2";
    public static final String SHADER_PROPERTIES = "ShadowProps";
    *///?}

    public static VertexFormat FORMAT;

    public static RenderPipeline ROUNDED_RECT;
    public static RenderPipeline LIQUID_GLASS;
    public static RenderPipeline IMAGE;
    public static RenderPipeline MSDF_FONT;

    private static volatile boolean initialized;
    private static volatile boolean available;

    private MusicRenderPipelines() {}

    private static RenderPipeline register(String name, boolean textured, VertexFormat format) {
        //? if <26.2 {
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
        //?} else {
        /*RenderPipeline.Builder builder = RenderPipeline.builder()
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withLocation(id("pipeline/" + name))
            .withVertexShader(id("core/" + name))
            .withFragmentShader(id("core/" + name))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexBinding(0, format)
            .withPrimitiveTopology(PrimitiveTopology.QUADS);
        if (textured) {
            builder.withBindGroupLayout(BindGroupLayouts.SAMPLER0);
        }
        *///?}
        return RenderPipelines.register(builder.build());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("xenaddons", path);
    }

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        try {
            //? if <26.2 {
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
            //?} else {
            /*VertexFormat format = VertexFormat.builder(0)
                .addAttribute("Position", GpuFormat.RGB32_FLOAT)
                .addAttribute("Color", GpuFormat.RGBA8_UNORM)
                .addAttribute("UV0", GpuFormat.RG32_FLOAT)
                .addAttribute(CUSTOM_DATA, GpuFormat.RGBA32_FLOAT)
                .addAttribute(COLOR_2, GpuFormat.RGBA8_UNORM)
                .addAttribute(SHADER_PROPERTIES, GpuFormat.RGBA32_FLOAT)
                .build();
            *///?}

            RenderPipeline roundedRect = register("rounded_rect", false, format);
            RenderPipeline liquidGlass = register("liquid_glass", true, format);
            RenderPipeline image = register("image", true, format);
            RenderPipeline msdfFont = register("msdf_font", true, format);

            //? if <26.2 {
            CUSTOM_DATA = customData;
            COLOR_2 = color2;
            SHADER_PROPERTIES = shaderProperties;
            //?}
            FORMAT = format;
            ROUNDED_RECT = roundedRect;
            LIQUID_GLASS = liquidGlass;
            IMAGE = image;
            MSDF_FONT = msdfFont;
            available = true;

            //? if <26.2 {
            XenAddons.LOGGER.info(
                "Music render pipelines initialized with vertex element IDs {}, {}, and {}",
                elementIds[0],
                elementIds[1],
                elementIds[2]
            );
            //?}
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

    //? if <26.2 {
    private static int[] findAvailableElementIds() {
        int[] result = new int[CUSTOM_ELEMENT_COUNT];
        int found = 0;

        // Custom vertex element IDs are global across every installed mod. Search
        // from the low end: other mods (e.g. SkyHanni) claim their own custom
        // elements from the high end, so scanning downward from MAX_COUNT races
        // them for the same slots and can leave them without the ID they expect,
        // crashing them at init (ExceptionInInitializerError/NoSuchElementException).
        for (int id = FIRST_CUSTOM_ELEMENT_ID; id < VertexFormatElement.MAX_COUNT; id++) {
            if (VertexFormatElement.byId(id) == null) {
                result[found++] = id;
                if (found == result.length) return result;
            }
        }

        throw new IllegalStateException(
            "Not enough free vertex format element IDs (needed " + CUSTOM_ELEMENT_COUNT + ", found " + found + ')'
        );
    }
    //?}
}
