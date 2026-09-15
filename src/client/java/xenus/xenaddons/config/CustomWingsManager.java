package xenus.xenaddons.config;

import xenus.xenaddons.cosmetics.CosmeticsRepository;
import xenus.xenaddons.cosmetics.CosmeticsSnapshot.PlayerCosmetic;
import xenus.xenaddons.cosmetics.CosmeticsSnapshot.PlayerWings;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class CustomWingsManager {
    private static final Map<UUID, PlayerWings> WINGS = new LinkedHashMap<>();

    private CustomWingsManager() {}

    public static void init() {
        WINGS.clear();
        CosmeticsRepository.init();

        for (Map.Entry<UUID, PlayerCosmetic> entry : CosmeticsRepository.getPlayers().entrySet()) {
            PlayerCosmetic player = entry.getValue();
            if (player == null || player.wings() == null) continue;
            WINGS.put(entry.getKey(), player.wings());
        }
    }

    public static PlayerWings get(UUID uuid) {
        if (uuid == null) return null;
        return WINGS.get(uuid);
    }

    public static boolean isEmpty() {
        return WINGS.isEmpty();
    }
}
