package xenus.xenaddons.cosmetics;

import xenus.xenaddons.XenAddons;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public final class CosmeticsRepository {
    private static final String COSMETIC_PEOPLE_RESOURCE = "/assets/xenaddons/visual/cosmeticPeople.json";
    private static final CosmeticsApiClient API_CLIENT = new CosmeticsApiClient();

    private static volatile CosmeticsSnapshot snapshot = CosmeticsSnapshot.EMPTY;
    private static volatile Source source = Source.NONE;
    private static volatile boolean initialized = false;

    private CosmeticsRepository() {}

    public static void init() {
        if (initialized) return;

        synchronized (CosmeticsRepository.class) {
            if (initialized) return;
            reloadInternal();
            initialized = true;
        }
    }

    public static void reload() {
        synchronized (CosmeticsRepository.class) {
            reloadInternal();
            initialized = true;
        }
    }

    public static CosmeticsSnapshot getSnapshot() {
        init();
        return snapshot;
    }

    public static Map<UUID, CosmeticsSnapshot.PlayerCosmetic> getPlayers() {
        return getSnapshot().players();
    }

    public static CosmeticsSnapshot.PlayerCosmetic getPlayer(UUID uuid) {
        if (uuid == null) return null;
        return getSnapshot().players().get(uuid);
    }

    public static boolean isUsingFallbackResource() {
        init();
        return source == Source.FALLBACK_RESOURCE;
    }

    public static String getApiUrl() {
        return API_CLIENT.endpoint();
    }

    private static void reloadInternal() {
        CosmeticsSnapshot previousSnapshot = snapshot;
        Source previousSource = source;

        try {
            CosmeticsSnapshot apiSnapshot = API_CLIENT.fetchSnapshot();
            snapshot = apiSnapshot;
            source = Source.API;
            XenAddons.LOGGER.info(
                "Loaded cosmetics snapshot from API {} ({} players)",
                API_CLIENT.endpoint(),
                apiSnapshot.players().size()
            );
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            loadFallbackOrKeepPrevious(previousSnapshot, previousSource, e);
            return;
        } catch (Exception e) {
            loadFallbackOrKeepPrevious(previousSnapshot, previousSource, e);
            return;
        }
    }

    private static void loadFallbackOrKeepPrevious(CosmeticsSnapshot previousSnapshot, Source previousSource, Exception apiError) {
        CosmeticsSnapshot fallbackSnapshot = loadFallbackSnapshot();
        if (fallbackSnapshot != null) {
            snapshot = fallbackSnapshot;
            source = Source.FALLBACK_RESOURCE;
            XenAddons.LOGGER.warn(
                "Failed to load cosmetics from API {}, using bundled fallback resource",
                API_CLIENT.endpoint(),
                apiError
            );
            return;
        }

        if (previousSource != Source.NONE || !previousSnapshot.players().isEmpty()) {
            XenAddons.LOGGER.warn("Failed to refresh cosmetics data, keeping previous snapshot", apiError);
            return;
        }

        snapshot = CosmeticsSnapshot.EMPTY;
        source = Source.NONE;
        XenAddons.LOGGER.error("Failed to load cosmetics from API and fallback resource", apiError);
    }

    private static CosmeticsSnapshot loadFallbackSnapshot() {
        InputStream stream = CosmeticsRepository.class.getResourceAsStream(COSMETIC_PEOPLE_RESOURCE);
        if (stream == null) {
            XenAddons.LOGGER.warn("Bundled cosmetics fallback resource is missing: {}", COSMETIC_PEOPLE_RESOURCE);
            return null;
        }

        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            CosmeticsSnapshot fallbackSnapshot = CosmeticsSnapshotParser.parseReader(reader);
            XenAddons.LOGGER.info(
                "Loaded cosmetics snapshot from bundled fallback resource {} ({} players)",
                COSMETIC_PEOPLE_RESOURCE,
                fallbackSnapshot.players().size()
            );
            return fallbackSnapshot;
        } catch (Exception e) {
            XenAddons.LOGGER.error("Failed to parse bundled cosmetics fallback resource {}", COSMETIC_PEOPLE_RESOURCE, e);
            return null;
        }
    }

    private enum Source {
        API,
        FALLBACK_RESOURCE,
        NONE
    }
}
