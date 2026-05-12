package xenus.xenaddons.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.nanovg.NSVGImage;
import org.lwjgl.system.MemoryUtil;
import xenus.xenaddons.XenAddons;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.nanovg.NanoSVG.nsvgCreateRasterizer;
import static org.lwjgl.nanovg.NanoSVG.nsvgDelete;
import static org.lwjgl.nanovg.NanoSVG.nsvgDeleteRasterizer;
import static org.lwjgl.nanovg.NanoSVG.nsvgParse;
import static org.lwjgl.nanovg.NanoSVG.nsvgRasterize;

public final class SvgIconTextures {
    private static final Map<String, Identifier> TEXTURES = new ConcurrentHashMap<>();

    private SvgIconTextures() {}

    public static Identifier get(String resourcePath, int size, int argb) {
        String key = resourcePath + "|" + size + "|" + Integer.toHexString(argb);
        Identifier cached = TEXTURES.get(key);
        if (cached != null) return cached;

        Identifier created = create(resourcePath, size, argb);
        if (created != null) TEXTURES.put(key, created);
        return created;
    }

    private static Identifier create(String resourcePath, int size, int argb) {
        NativeImage nativeImage = null;
        try {
            String source = readResource(resourcePath);
            NSVGImage svg = nsvgParse(source, "px", 96f);
            if (svg == null) throw new IllegalStateException("Failed to parse " + resourcePath);

            ByteBuffer buffer = MemoryUtil.memAlloc(size * size * 4);
            long rasterizer = nsvgCreateRasterizer();
            try {
                float scale = size / Math.max(svg.width(), svg.height());
                nsvgRasterize(rasterizer, svg, 0f, 0f, scale, buffer, size, size, size * 4);
                nativeImage = new NativeImage(size, size, false);
                tint(buffer, nativeImage, size, argb);
            } finally {
                nsvgDeleteRasterizer(rasterizer);
                nsvgDelete(svg);
                MemoryUtil.memFree(buffer);
            }

            Identifier id = Identifier.of(
                "xenaddons",
                "spotify/icon_" + Integer.toUnsignedString(keyHash(resourcePath, size, argb), 16)
            );
            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "XenAddons Spotify icon", nativeImage);
            nativeImage = null;
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
            return id;
        } catch (Exception e) {
            if (nativeImage != null) nativeImage.close();
            XenAddons.LOGGER.debug("Failed to create SVG icon texture {}", resourcePath, e);
            return null;
        }
    }

    private static void tint(ByteBuffer buffer, NativeImage nativeImage, int size, int argb) {
        int rgb = argb & 0x00FFFFFF;
        int colorAlpha = (argb >>> 24) & 0xFF;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int i = ((y * size) + x) * 4;
                int alpha = buffer.get(i + 3) & 0xFF;
                alpha = (alpha * colorAlpha) / 255;
                nativeImage.setColorArgb(x, y, (alpha << 24) | rgb);
            }
        }
    }

    private static String readResource(String resourcePath) throws Exception {
        try (InputStream stream = SvgIconTextures.class.getResourceAsStream(resourcePath)) {
            if (stream == null) throw new IllegalArgumentException("Missing resource " + resourcePath);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static int keyHash(String resourcePath, int size, int argb) {
        int result = resourcePath.hashCode();
        result = 31 * result + size;
        result = 31 * result + argb;
        return result;
    }
}
