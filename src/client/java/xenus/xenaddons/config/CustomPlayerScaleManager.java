package xenus.xenaddons.config;

import xenus.xenaddons.cosmetics.CosmeticsRepository;
import xenus.xenaddons.cosmetics.CosmeticsSnapshot.PlayerCosmetic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class CustomPlayerScaleManager {
    private static final Map<UUID, PlayerScale> SCALES = new LinkedHashMap<>();

    private CustomPlayerScaleManager() {}

    public static void init() {
        SCALES.clear();
        CosmeticsRepository.init();

        for (Map.Entry<UUID, PlayerCosmetic> entry : CosmeticsRepository.getPlayers().entrySet()) {
            PlayerScale scale = toPlayerScale(entry.getValue());
            if (scale == null) continue;
            SCALES.put(entry.getKey(), scale);
        }
    }

    public static PlayerScale get(UUID uuid) {
        if (uuid == null) return null;
        return SCALES.get(uuid);
    }

    private static PlayerScale toPlayerScale(PlayerCosmetic player) {
        if (player == null) return null;

        xenus.xenaddons.cosmetics.CosmeticsSnapshot.PlayerScale snapshotScale = player.scale();
        if (snapshotScale == null) return null;

        return new PlayerScale(snapshotScale.x(), snapshotScale.y(), snapshotScale.z());
    }

    public record PlayerScale(float x, float y, float z) {}
}
