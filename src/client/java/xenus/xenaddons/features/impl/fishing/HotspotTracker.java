package xenus.xenaddons.features.impl.fishing;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HotspotTracker {
    private HotspotTracker() {}

    public static final class Hotspot {
        public final int standId;
        public final HotspotType type;
        public Vec3 pos;
        public Double radius;
        final Map<Double, Integer> radiusVotes = new HashMap<>();

        Hotspot(int standId, HotspotType type, Vec3 pos) {
            this.standId = standId;
            this.type = type;
            this.pos = pos;
        }
    }

    private static final Map<Integer, Hotspot> hotspots = new HashMap<>();

    // Widest particle-to-center distance seen across islands (Crimson Isle/Torrhus Canyon), plus slack.
    private static final double MAX_PARTICLE_DISTANCE_SQ = 25.5 * 25.5;
    private static final Vector3f DUST_COLOR_TARGET = new Vector3f(1.0f, 0.4117647f, 0.7058824f);
    private static final float DUST_COLOR_TOLERANCE = 0.01f;

    public static List<Hotspot> getHotspots() {
        return new ArrayList<>(hotspots.values());
    }

    public static void reset() {
        hotspots.clear();
    }

    public static void scan(ClientLevel level) {
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof ArmorStand stand)) continue;
            if (!stand.hasCustomName() || hotspots.containsKey(stand.getId())) continue;

            String plain = stripFormatting(stand.getCustomName().getString());
            HotspotType type = HotspotType.match(plain);
            if (type == null) continue;

            Vec3 pos = resolveSurfacePosition(level, stand.position());
            hotspots.put(stand.getId(), new Hotspot(stand.getId(), type, pos));
        }

        hotspots.values().removeIf(hotspot -> {
            Entity stand = level.getEntity(hotspot.standId);
            return stand == null || !stand.isAlive();
        });
    }

    private static Vec3 resolveSurfacePosition(ClientLevel level, Vec3 standPos) {
        BlockPos base = BlockPos.containing(standPos);
        for (int dy = 0; dy <= 3; dy++) {
            BlockPos check = base.below(dy);
            FluidState fluid = level.getFluidState(check);
            if (!fluid.isEmpty()) {
                double y = check.getY() + fluid.getHeight(level, check) + 0.1;
                return new Vec3(standPos.x, y, standPos.z);
            }
        }
        return standPos;
    }

    /** @return whether this packet was a hotspot particle burst, so the caller can hide it. */
    public static boolean onParticlePacket(ClientboundLevelParticlesPacket packet) {
        if (!isHotspotParticle(packet)) return false;
        if (hotspots.isEmpty()) return true;

        Hotspot closest = null;
        double closestDistSq = Double.MAX_VALUE;
        for (Hotspot hotspot : hotspots.values()) {
            if (hotspot.pos == null) continue;
            double dx = packet.getX() - hotspot.pos.x;
            double dz = packet.getZ() - hotspot.pos.z;
            double distSq = dx * dx + dz * dz;
            if (distSq > MAX_PARTICLE_DISTANCE_SQ || distSq >= closestDistSq) continue;
            closest = hotspot;
            closestDistSq = distSq;
        }
        if (closest == null) return true;

        // Multiple particle bursts land at slightly different distances, so vote for the most
        // common rounded radius instead of trusting a single noisy sample.
        double radius = Math.round(Math.sqrt(closestDistSq) * 2.0) / 2.0;
        Map<Double, Integer> votes = closest.radiusVotes;
        votes.merge(radius, 1, Integer::sum);

        Double bestRadius = null;
        int bestVotes = -1;
        for (Map.Entry<Double, Integer> entry : votes.entrySet()) {
            if (entry.getValue() > bestVotes) {
                bestVotes = entry.getValue();
                bestRadius = entry.getKey();
            }
        }
        closest.radius = bestRadius;
        return true;
    }

    private static boolean isHotspotParticle(ClientboundLevelParticlesPacket packet) {
        ParticleOptions options = packet.getParticle();
        if (options instanceof DustParticleOptions dust) {
            Vector3f color = dust.getColor();
            if (Math.abs(color.x() - DUST_COLOR_TARGET.x()) > DUST_COLOR_TOLERANCE) return false;
            if (Math.abs(color.y() - DUST_COLOR_TARGET.y()) > DUST_COLOR_TOLERANCE) return false;
            if (Math.abs(color.z() - DUST_COLOR_TARGET.z()) > DUST_COLOR_TOLERANCE) return false;
            return packet.getCount() == 0 && packet.getXDist() == 1f && packet.getMaxSpeed() == 1f;
        }
        if (options != null && options.getType() == ParticleTypes.SMOKE) {
            return packet.getCount() == 5 || packet.getCount() == 2;
        }
        return false;
    }

    private static String stripFormatting(String input) {
        return input == null ? "" : input.replaceAll("§[0-9a-fk-or]", "");
    }
}
