package xenus.xenaddons.render.music.font;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
//? if <26.2 {
import com.mojang.blaze3d.textures.TextureFormat;
//?} else {
/*import com.mojang.blaze3d.GpuFormat;
*///?}
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.render.TextureSetup;
import org.joml.Matrix3x2fc;
import xenus.xenaddons.render.music.MusicVertexConsumer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

public final class MusicMsdfFont {
    private final GpuTexture texture;
    private final GpuTextureView view;
    private final TextureSetup textureSetup;
    private final int atlasWidth;
    private final int atlasHeight;
    private final float distanceRange;
    private final float nominalSize;
    private final float lineHeight;
    private final float ascender;
    private final Map<Integer, Glyph> glyphs = new HashMap<>();

    public MusicMsdfFont(InputStream imageStream, InputStream jsonStream, float nominalSize) throws Exception {
        JsonObject root = JsonParser.parseReader(
            new InputStreamReader(jsonStream, StandardCharsets.UTF_8)
        ).getAsJsonObject();
        JsonObject atlas = root.getAsJsonObject("atlas");
        atlasWidth = atlas.get("width").getAsInt();
        atlasHeight = atlas.get("height").getAsInt();
        distanceRange = atlas.get("distanceRange").getAsFloat();
        this.nominalSize = nominalSize;

        JsonObject metrics = root.getAsJsonObject("metrics");
        float emSize = metrics.has("emSize") ? metrics.get("emSize").getAsFloat() : 1.0f;
        lineHeight = metrics.has("lineHeight") ? metrics.get("lineHeight").getAsFloat() / emSize : 1.2f;
        ascender = metrics.has("ascender") ? metrics.get("ascender").getAsFloat() / emSize : 0.8f;

        JsonArray glyphArray = root.getAsJsonArray("glyphs");
        for (JsonElement element : glyphArray) {
            JsonObject glyph = element.getAsJsonObject();
            if (!glyph.has("unicode") || glyph.get("unicode").isJsonNull()) continue;

            int unicode = glyph.get("unicode").getAsInt();
            float advance = glyph.get("advance").getAsFloat() / emSize;
            float planeLeft = 0.0f;
            float planeBottom = 0.0f;
            float planeRight = 0.0f;
            float planeTop = 0.0f;
            float atlasLeft = 0.0f;
            float atlasBottom = 0.0f;
            float atlasRight = 0.0f;
            float atlasTop = 0.0f;

            if (glyph.has("planeBounds")) {
                JsonObject bounds = glyph.getAsJsonObject("planeBounds");
                planeLeft = bounds.get("left").getAsFloat() / emSize;
                planeBottom = bounds.get("bottom").getAsFloat() / emSize;
                planeRight = bounds.get("right").getAsFloat() / emSize;
                planeTop = bounds.get("top").getAsFloat() / emSize;
            }
            if (glyph.has("atlasBounds")) {
                JsonObject bounds = glyph.getAsJsonObject("atlasBounds");
                atlasLeft = bounds.get("left").getAsFloat();
                atlasBottom = bounds.get("bottom").getAsFloat();
                atlasRight = bounds.get("right").getAsFloat();
                atlasTop = bounds.get("top").getAsFloat();
            }

            glyphs.put(
                unicode,
                new Glyph(
                    advance,
                    planeLeft,
                    planeBottom,
                    planeRight,
                    planeTop,
                    atlasLeft,
                    atlasBottom,
                    atlasRight,
                    atlasTop
                )
            );
        }

        NativeImage image = NativeImage.read(imageStream);
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        texture = RenderSystem.getDevice().createTexture(
            "XenAddons Music MSDF Font",
            GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING,
            //? if <26.2 {
            TextureFormat.RGBA8,
            //?} else {
            /*GpuFormat.RGBA8_UNORM,
            *///?}
            imageWidth,
            imageHeight,
            1,
            1
        );
        view = RenderSystem.getDevice().createTextureView(texture);

        ByteBuffer pixels = ByteBuffer.allocateDirect(imageWidth * imageHeight * 4);
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                int rgba = image.getPixel(x, y);
                pixels.put((byte) (rgba & 0xFF));
                pixels.put((byte) ((rgba >> 8) & 0xFF));
                pixels.put((byte) ((rgba >> 16) & 0xFF));
                pixels.put((byte) ((rgba >> 24) & 0xFF));
            }
        }
        pixels.flip();
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(
            texture,
            pixels,
            //? if <26.2 {
            NativeImage.Format.RGBA,
            //?}
            0,
            0,
            0,
            0,
            imageWidth,
            imageHeight
        );
        image.close();

        GpuSampler sampler = RenderSystem.getDevice().createSampler(
            AddressMode.CLAMP_TO_EDGE,
            AddressMode.CLAMP_TO_EDGE,
            FilterMode.LINEAR,
            FilterMode.LINEAR,
            1,
            OptionalDouble.empty()
        );
        textureSetup = TextureSetup.singleTexture(view, sampler);
    }

    public void draw(
        VertexConsumer consumer,
        MusicVertexConsumer musicConsumer,
        Matrix3x2fc pose,
        String text,
        float x,
        float y,
        float size,
        int color
    ) {
        float cursorX = x;
        float baseline = y + ascender * size;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);
            Glyph glyph = glyphs.get(codePoint);
            if (glyph == null) {
                Glyph space = glyphs.get(32);
                if (space != null) cursorX += space.advance * size;
                continue;
            }

            if (glyph.atlasRight > glyph.atlasLeft && glyph.atlasTop > glyph.atlasBottom) {
                float x0 = cursorX + glyph.planeLeft * size;
                float y0 = baseline - glyph.planeTop * size;
                float x1 = cursorX + glyph.planeRight * size;
                float y1 = baseline - glyph.planeBottom * size;
                float u0 = glyph.atlasLeft / atlasWidth;
                float v0 = 1.0f - (glyph.atlasTop / atlasHeight);
                float u1 = glyph.atlasRight / atlasWidth;
                float v1 = 1.0f - (glyph.atlasBottom / atlasHeight);

                addVertex(consumer, musicConsumer, pose, x0, y0, u0, v0, color);
                addVertex(consumer, musicConsumer, pose, x0, y1, u0, v1, color);
                addVertex(consumer, musicConsumer, pose, x1, y1, u1, v1, color);
                addVertex(consumer, musicConsumer, pose, x1, y0, u1, v0, color);
            }
            cursorX += glyph.advance * size;
        }
    }

    private void addVertex(
        VertexConsumer consumer,
        MusicVertexConsumer musicConsumer,
        Matrix3x2fc pose,
        float x,
        float y,
        float u,
        float v,
        int color
    ) {
        consumer.addVertexWith2DPose(pose, x, y).setColor(color).setUv(u, v);
        musicConsumer.setMusicCustomData(distanceRange, 0.0f, 0.0f, 0.0f);
        musicConsumer.setMusicColor2(0);
        musicConsumer.setMusicShaderProperties(0.0f, 0.0f, 0.0f, 0.0f);
    }

    public float width(String text, float size) {
        float width = 0.0f;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);
            Glyph glyph = glyphs.get(codePoint);
            if (glyph != null) {
                width += glyph.advance * size;
            } else {
                Glyph space = glyphs.get(32);
                if (space != null) width += space.advance * size;
            }
        }
        return width;
    }

    public float height(float size) {
        return lineHeight * size;
    }

    public TextureSetup textureSetup() {
        return textureSetup;
    }

    public void close() {
        view.close();
        texture.close();
    }

    private record Glyph(
        float advance,
        float planeLeft,
        float planeBottom,
        float planeRight,
        float planeTop,
        float atlasLeft,
        float atlasBottom,
        float atlasRight,
        float atlasTop
    ) {}
}
