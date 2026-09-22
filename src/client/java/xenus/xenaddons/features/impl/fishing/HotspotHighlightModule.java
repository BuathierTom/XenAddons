package xenus.xenaddons.features.impl.fishing;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import xenus.xenaddons.compat.XenCameraCompat;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.settings.NumberSetting;
import xenus.xenaddons.utils.RenderUtils;

import java.util.List;

public class HotspotHighlightModule extends Module {
    private static final int CIRCLE_SEGMENTS = 32;

    private final BooleanSetting showOutline = new BooleanSetting("Show Outline", true);
    private final BooleanSetting showSurface = new BooleanSetting("Show Surface", true);
    private final NumberSetting surfaceOpacity = new NumberSetting("Surface Opacity", 40, 0, 100, 1);
    private final NumberSetting outlineOpacity = new NumberSetting("Outline Opacity", 90, 0, 100, 1);

    private int scanCooldown = 0;

    public HotspotHighlightModule() {
        super("Hotspot Highlight", "Highlights fishing hotspots with a color matching their buff type", Category.FISHING);

        this.registerSetting(showOutline);
        this.registerSetting(showSurface);
        this.registerSetting(surfaceOpacity);
        this.registerSetting(outlineOpacity);

        ClientTickEvents.END_CLIENT_TICK.register(client -> onTick());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> HotspotTracker.reset());

        LevelRenderEvents.END_MAIN.register(this::onWorldRender);
    }

    @Override
    public void onDisable() {
        HotspotTracker.reset();
    }

    private void onTick() {
        if (!this.isEnabled() || mc.level == null) return;
        if (scanCooldown-- > 0) return;
        scanCooldown = 10;
        HotspotTracker.scan(mc.level);
    }

    private void onWorldRender(LevelRenderContext context) {
        if (!this.isEnabled()) return;
        if (!showOutline.getValue() && !showSurface.getValue()) return;
        if (mc.level == null || mc.gameRenderer == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        if (camera == null) return;
        Vec3 cameraPos = XenCameraCompat.getPos(camera);

        List<HotspotTracker.Hotspot> hotspots = HotspotTracker.getHotspots();
        if (hotspots.isEmpty()) return;

        try (ByteBufferBuilder allocator = new ByteBufferBuilder(131072)) {
            MultiBufferSource.BufferSource consumers = MultiBufferSource.immediate(allocator);

            for (HotspotTracker.Hotspot hotspot : hotspots) {
                if (hotspot.pos == null || hotspot.radius == null) continue;
                float radius = hotspot.radius.floatValue();
                int rgb = hotspot.type.color & 0xFFFFFF;

                if (showSurface.getValue()) {
                    int color = (alphaFromPercent(surfaceOpacity.getValue()) << 24) | rgb;
                    RenderUtils.drawCircleFilled(context.poseStack(), consumers, cameraPos, hotspot.pos, radius, color, CIRCLE_SEGMENTS);
                }
                if (showOutline.getValue()) {
                    int color = (alphaFromPercent(outlineOpacity.getValue()) << 24) | rgb;
                    RenderUtils.drawCircleOutline(context.poseStack(), consumers, cameraPos, hotspot.pos, radius, color, CIRCLE_SEGMENTS);
                }
            }

            consumers.endBatch();
        }
    }

    private static int alphaFromPercent(double percent) {
        return (int) Math.round(percent * 2.55);
    }

    public static boolean shouldHideParticles() {
        HotspotHighlightModule module = ModuleManager.getModule(HotspotHighlightModule.class);
        return module != null && module.isEnabled();
    }
}
