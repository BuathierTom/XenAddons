package xenus.xenaddons.render.music;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;

import java.util.OptionalDouble;

public final class MusicCaptureManager {
    private static final long CAPTURE_INTERVAL_NANOS = 33_333_333L;

    private static GpuTexture captureTexture;
    private static GpuTextureView captureView;
    private static TextureSetup textureSetup;
    private static long lastCaptureNanos;

    private MusicCaptureManager() {}

    public static TextureSetup textureSetup() {
        return textureSetup != null ? textureSetup : TextureSetup.noTexture();
    }

    public static void update(Minecraft minecraft) {
        if (!RenderSystem.isOnRenderThread() || minecraft == null || minecraft.getMainRenderTarget() == null) return;

        GpuTexture screenTexture = minecraft.getMainRenderTarget().getColorTexture();
        if (screenTexture == null || screenTexture.isClosed()) return;

        int width = screenTexture.getWidth(0);
        int height = screenTexture.getHeight(0);
        if (width <= 0 || height <= 0) return;

        boolean recreateCapture = captureTexture == null
            || captureTexture.getWidth(0) != width
            || captureTexture.getHeight(0) != height;
        long now = System.nanoTime();
        if (!recreateCapture
            && lastCaptureNanos != 0L
            && now - lastCaptureNanos < CAPTURE_INTERVAL_NANOS) {
            return;
        }

        if (recreateCapture) {
            cleanup();
            captureTexture = RenderSystem.getDevice().createTexture(
                "XenAddons Music Liquid Glass Capture",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING
                    | GpuTexture.USAGE_RENDER_ATTACHMENT,
                screenTexture.getFormat(),
                width,
                height,
                1,
                1
            );
            captureView = RenderSystem.getDevice().createTextureView(captureTexture);
            textureSetup = TextureSetup.singleTexture(
                captureView,
                RenderSystem.getDevice().createSampler(
                    AddressMode.CLAMP_TO_EDGE,
                    AddressMode.CLAMP_TO_EDGE,
                    FilterMode.LINEAR,
                    FilterMode.LINEAR,
                    1,
                    OptionalDouble.empty()
                )
            );
        }

        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
            screenTexture,
            captureTexture,
            0,
            0,
            0,
            0,
            0,
            width,
            height
        );
        lastCaptureNanos = now;
    }

    public static void cleanup() {
        lastCaptureNanos = 0L;
        textureSetup = null;
        if (captureView != null) {
            captureView.close();
            captureView = null;
        }
        if (captureTexture != null) {
            captureTexture.close();
            captureTexture = null;
        }
    }
}
