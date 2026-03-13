package xenus.xenaddons.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class CustomPlayerScaleManager {
    private static final String COSMETIC_PEOPLE_RESOURCE = "/assets/xenaddons/visual/cosmeticPeople.json";
    private static final Map<UUID, PlayerScale> SCALES = new LinkedHashMap<>();

    private CustomPlayerScaleManager() {}

    public static void init() {
        SCALES.clear();
        loadFromCosmeticPeopleResource();
    }

    private static boolean loadFromCosmeticPeopleResource() {
        InputStream stream = CustomPlayerScaleManager.class.getResourceAsStream(COSMETIC_PEOPLE_RESOURCE);
        if (stream == null) return false;

        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) return false;
            return loadFromCosmeticPeopleObject(root.getAsJsonObject());
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean loadFromCosmeticPeopleObject(JsonObject root) {
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            UUID uuid;
            try {
                uuid = UUID.fromString(entry.getKey());
            } catch (Exception ignored) {
                continue;
            }

            if (!entry.getValue().isJsonObject()) continue;
            JsonObject obj = entry.getValue().getAsJsonObject();
            PlayerScale scale = parseScale(obj);
            if (scale == null) continue;

            SCALES.put(uuid, scale);
        }
        return !SCALES.isEmpty();
    }

    private static PlayerScale parseScale(JsonObject obj) {
        Float x = getOptionalFloat(obj, "sizeX");
        Float y = getOptionalFloat(obj, "sizeY");
        Float z = getOptionalFloat(obj, "sizeZ");

        if (x == null) x = getOptionalFloat(obj, "x");
        if (y == null) y = getOptionalFloat(obj, "y");
        if (z == null) z = getOptionalFloat(obj, "z");

        if (x == null && y == null && z == null) return null;

        return new PlayerScale(
            x != null ? x : 1.0F,
            y != null ? y : 1.0F,
            z != null ? z : 1.0F
        );
    }

    private static Float getOptionalFloat(JsonObject obj, String key) {
        if (!obj.has(key)) return null;
        try {
            return obj.get(key).getAsFloat();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static PlayerScale get(UUID uuid) {
        if (uuid == null) return null;
        return SCALES.get(uuid);
    }

    public record PlayerScale(float x, float y, float z) {}
}
