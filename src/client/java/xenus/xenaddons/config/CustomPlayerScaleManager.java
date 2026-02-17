package xenus.xenaddons.config;

import com.google.gson.JsonArray;
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
    private static final String CUSTOM_SIZES_RESOURCE = "/assets/xenaddons/visual/customSizes.json";
    private static final Map<UUID, PlayerScale> SCALES = new LinkedHashMap<>();

    private CustomPlayerScaleManager() {}

    public static void init() {
        SCALES.clear();
        loadFromResources();
    }

    private static boolean loadFromResources() {
        InputStream stream = CustomPlayerScaleManager.class.getResourceAsStream(CUSTOM_SIZES_RESOURCE);
        if (stream == null) return false;

        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonArray()) return false;
            return loadFromJsonArray(root.getAsJsonArray());
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean loadFromJsonArray(JsonArray arr) {
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            if (!obj.has("uuid") || !obj.has("x") || !obj.has("y") || !obj.has("z")) continue;

            try {
                UUID uuid = UUID.fromString(obj.get("uuid").getAsString());
                float x = obj.get("x").getAsFloat();
                float y = obj.get("y").getAsFloat();
                float z = obj.get("z").getAsFloat();
                SCALES.put(uuid, new PlayerScale(x, y, z));
            } catch (Exception ignored) {}
        }
        return !SCALES.isEmpty();
    }

    public static PlayerScale get(UUID uuid) {
        if (uuid == null) return null;
        return SCALES.get(uuid);
    }

    public record PlayerScale(float x, float y, float z) {}
}
