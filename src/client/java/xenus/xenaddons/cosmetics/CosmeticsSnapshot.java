package xenus.xenaddons.cosmetics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CosmeticsSnapshot(int version, String updatedAt, Map<UUID, PlayerCosmetic> players) {
    public static final CosmeticsSnapshot EMPTY = new CosmeticsSnapshot(1, null, Map.of());

    public CosmeticsSnapshot {
        if (players == null || players.isEmpty()) {
            players = Map.of();
        } else {
            players = Collections.unmodifiableMap(new LinkedHashMap<>(players));
        }
    }

    public record PlayerCosmetic(String username, List<String> aliases, String displayName, PlayerAnimatedName animatedName, PlayerScale scale, PlayerWings wings) {
        public PlayerCosmetic {
            aliases = aliases == null || aliases.isEmpty() ? List.of() : List.copyOf(aliases);
        }
    }

    public record PlayerScale(float x, float y, float z) {}

    public record PlayerWings(float size, int color) {}

    public record PlayerAnimatedName(List<String> frames, int changeIntervalTicks) {
        public PlayerAnimatedName {
            frames = frames == null || frames.isEmpty() ? List.of() : List.copyOf(frames);
            changeIntervalTicks = Math.max(1, changeIntervalTicks);
        }
    }
}
