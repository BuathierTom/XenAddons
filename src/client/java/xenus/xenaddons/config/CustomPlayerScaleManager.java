package xenus.xenaddons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class CustomPlayerScaleManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File SCALE_FILE = new File(
        new File(FabricLoader.getInstance().getConfigDir().toFile(), "xenaddons"),
        "custom_sizes.json"
    );

    private static final Map<UUID, PlayerScale> SCALES = new LinkedHashMap<>();

    private CustomPlayerScaleManager() {}

    public static void init() {
        File parent = SCALE_FILE.getParentFile();
        if (parent != null && !parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }

        SCALES.clear();
        addDefaultEntries();

        if (!SCALE_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(SCALE_FILE)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonArray()) {
                save();
                return;
            }

            JsonArray arr = root.getAsJsonArray();
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

            // Force the requested default profile value for this UUID.
            SCALES.put(
                UUID.fromString("24c56ab5-b504-4d38-bd06-e5af6686fcaa"),
                new PlayerScale(2.0f, 0.5f, 1.0f)
            );
        } catch (IOException ignored) {}
    }

    private static void addDefaultEntries() {
        // User default requested:
        // 24c56ab5-b504-4d38-bd06-e5af6686fcaa -> x: 2.0, y: 0.5, z: 1.0
        SCALES.put(
            UUID.fromString("24c56ab5-b504-4d38-bd06-e5af6686fcaa"),
            new PlayerScale(2.0f, 0.5f, 1.0f)
        );
    }

    public static PlayerScale get(UUID uuid) {
        if (uuid == null) return null;
        return SCALES.get(uuid);
    }

    public static void save() {
        JsonArray arr = new JsonArray();
        for (Map.Entry<UUID, PlayerScale> entry : SCALES.entrySet()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("uuid", entry.getKey().toString());
            obj.addProperty("x", entry.getValue().x());
            obj.addProperty("y", entry.getValue().y());
            obj.addProperty("z", entry.getValue().z());
            arr.add(obj);
        }

        try (FileWriter writer = new FileWriter(SCALE_FILE)) {
            GSON.toJson(arr, writer);
        } catch (IOException ignored) {}
    }

    public record PlayerScale(float x, float y, float z) {}
}
