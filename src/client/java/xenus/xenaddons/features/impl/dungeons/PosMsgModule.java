package xenus.xenaddons.features.impl.dungeons;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.utils.DungeonUtils;
import xenus.xenaddons.utils.RenderUtils;

import java.util.HashMap;
import java.util.Map;

public class PosMsgModule extends Module {
    private static final int ZONE_COLOR_IDLE = 0xAAFF4444;
    private static final int ZONE_COLOR_ACTIVE = 0xAA00FF66;

    private final Map<String, Boolean> lastLocation = new HashMap<>();
    private final BooleanSetting showZones = new BooleanSetting("Show Zones", false);
    private long lastSendTime = 0;

    public PosMsgModule() {
        super("Pos Message", "Sends a chat message when you reach specific points in F7/M7", Category.DUNGEON);
        this.registerSetting(showZones);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (this.isEnabled()) onTick();
        });

        WorldRenderEvents.END_MAIN.register(this::onWorldRender);
    }

    private void onTick() {
        if (!DungeonUtils.inDungeon()) {
            if (!lastLocation.isEmpty()) lastLocation.clear();
            return;
        }
        
        ClientPlayerEntity player = mc.player;
        if (player == null || player.isInvisible()) return;

        String playerClass = DungeonUtils.getPlayerClass(player);
        if (playerClass == null) playerClass = "";

        for (DungeonUtils.PositionDefinition pos : DungeonUtils.positionDefinitions) {
            if (lastLocation.getOrDefault(pos.id, false)) continue;
            if ("AtP5".equals(pos.id) && !DungeonUtils.hasHealerBeenAtMidThisRun()) continue;
            
            if (pos.checkCondition.apply(playerClass) && pos.checkPosition.apply(player)) {
                
                // Anti-spam
                if (System.currentTimeMillis() - lastSendTime < 500) continue;
                
                lastLocation.put(pos.id, true);
                lastSendTime = System.currentTimeMillis();
                
                // Send Party Chat
                String cmd = "pc " + pos.messageText;
                if (mc.getNetworkHandler() != null) {
                    mc.getNetworkHandler().sendChatCommand(cmd);
                }
                
                // Client side confirmation
                xenus.xenaddons.utils.ChatUtils.info("Announcing -> " + pos.messageText);
            }
        }
    }
    
    @Override
    public void onDisable() {
        lastLocation.clear();
    }

    private void onWorldRender(WorldRenderContext context) {
        if (!this.isEnabled() || !showZones.getValue()) return;
        if (!DungeonUtils.inDungeon()) return;
        if (mc.player == null || mc.world == null || mc.gameRenderer == null) return;

        Camera camera = mc.gameRenderer.getCamera();
        if (camera == null) return;
        Vec3d cameraPos = camera.getPos();

        try (BufferAllocator allocator = new BufferAllocator(131072)) {
            VertexConsumerProvider.Immediate consumers = VertexConsumerProvider.immediate(allocator);
            Box playerBox = mc.player.getBoundingBox();

            for (Box zone : ZONE_BOXES) {
                int color = intersects(zone, playerBox) ? ZONE_COLOR_ACTIVE : ZONE_COLOR_IDLE;
                RenderUtils.drawBox(context.matrices(), consumers, cameraPos, zone, color, false);
            }

            consumers.draw();
        }
    }

    private static boolean intersects(Box a, Box b) {
        return b.maxX >= a.minX && b.minX <= a.maxX
            && b.maxY >= a.minY && b.minY <= a.maxY
            && b.maxZ >= a.minZ && b.minZ <= a.maxZ;
    }

    private static Box box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new Box(
            Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
            Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2)
        );
    }

    private static Box sphereBox(double cx, double cy, double cz, double r) {
        return box(cx - r, cy - r, cz - r, cx + r, cy + r, cz + r);
    }

    private static final Box[] ZONE_BOXES = {
        box(106, 118, 92, 110, 122, 96), // SS
        box(49, 108, 129, 58, 115, 133), // EE2
        box(59, 132, 138, 62, 135, 143), // EE2 High
        sphereBox(69.5, 109.0, 121.5, 0.4999), // EE2 Safe Spot
        sphereBox(67.5, 109.0, 121.5, 0.4999),
        sphereBox(48.5, 109.0, 121.5, 0.4999),
        sphereBox(46.5, 109.0, 121.5, 0.4999),
        box(63, 132, 146, 66, 133, 147), // EE2 Safe Spot High (extended)
        box(0, 108, 98, 4, 115, 107), // EE3
        sphereBox(18.5, 121.5, 91.5, 0.45), // EE3 Safe Spot line
        sphereBox(18.5, 121.5, 92.5, 0.45),
        sphereBox(18.5, 121.5, 93.5, 0.45),
        sphereBox(18.5, 121.5, 94.5, 0.45),
        sphereBox(18.5, 121.5, 95.5, 0.45),
        sphereBox(18.5, 121.5, 96.5, 0.45),
        sphereBox(18.5, 121.5, 97.5, 0.45),
        sphereBox(18.5, 121.5, 98.5, 0.45),
        sphereBox(18.5, 121.5, 99.5, 0.45),
        sphereBox(-0.5, 120.0, 77.5, 1.5), // Arrows Device
        sphereBox(58.0, 120.0, 122.0, 0.75), // Entering Core
        box(52, 113, 49, 57, 117, 53), // Core
        box(41, 110, 59, 68, 150, 117), // Goldor Tunnel
        sphereBox(63.5, 127.0, 35.5, 1.5), // PewPew Device
        box(47, 64, 69, 61.999, 75, 83.999), // Mid
        box(91, 129, 44, 93, 133, 46) // Pre4 Entry
    };
}
