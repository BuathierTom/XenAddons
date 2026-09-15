package xenus.xenaddons.cosmetics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CosmeticsSnapshotParser {

    private static final float DEFAULT_WING_SIZE = 1.0F;
    private static final int DEFAULT_WING_COLOR = 0xFFFFFFFF;
    private CosmeticsSnapshotParser() {}

    public static CosmeticsSnapshot parseString(String json) {
        return parseRoot(JsonParser.parseString(json));
    }

    public static CosmeticsSnapshot parseReader(Reader reader) {
        return parseRoot(JsonParser.parseReader(reader));
    }

    private static CosmeticsSnapshot parseRoot(JsonElement rootElement) {
        if (rootElement == null || !rootElement.isJsonObject()) {
            throw new JsonParseException("Cosmetics payload root must be a JSON object");
        }

        JsonObject root = rootElement.getAsJsonObject();
        if (root.has("players")) {
            return parseNormalizedSnapshot(root);
        }

        return new CosmeticsSnapshot(1, null, parsePlayersObject(root));
    }

    private static CosmeticsSnapshot parseNormalizedSnapshot(JsonObject root) {
        JsonElement playersElement = root.get("players");
        if (playersElement == null || playersElement.isJsonNull()) {
            return new CosmeticsSnapshot(readVersion(root), readNullableString(root, "updatedAt"), Map.of());
        }
        if (!playersElement.isJsonObject()) {
            throw new JsonParseException("Cosmetics payload field 'players' must be an object");
        }

        return new CosmeticsSnapshot(
            readVersion(root),
            readNullableString(root, "updatedAt"),
            parsePlayersObject(playersElement.getAsJsonObject())
        );
    }

    private static Map<UUID, CosmeticsSnapshot.PlayerCosmetic> parsePlayersObject(JsonObject playersObject) {
        LinkedHashMap<UUID, CosmeticsSnapshot.PlayerCosmetic> players = new LinkedHashMap<>();

        for (Map.Entry<String, JsonElement> entry : playersObject.entrySet()) {
            UUID uuid;
            try {
                uuid = UUID.fromString(entry.getKey());
            } catch (Exception ignored) {
                continue;
            }

            if (!entry.getValue().isJsonObject()) continue;

            CosmeticsSnapshot.PlayerCosmetic cosmetic = parsePlayer(entry.getValue().getAsJsonObject());
            if (cosmetic == null) continue;

            players.put(uuid, cosmetic);
        }

        return players;
    }

    private static CosmeticsSnapshot.PlayerCosmetic parsePlayer(JsonObject obj) {
        String username = readNullableString(obj, "username");
        if (username == null) {
            username = readNullableString(obj, "pseudo");
        }

        List<String> aliases = readAliases(obj);

        String displayName = readDisplayName(obj, "displayName");
        if (displayName == null) {
            displayName = readDisplayName(obj, "name");
        }

        CosmeticsSnapshot.PlayerScale scale = readScale(obj);
        CosmeticsSnapshot.PlayerWings wings = readWings(obj);
        return new CosmeticsSnapshot.PlayerCosmetic(username, aliases, displayName, scale, wings);
    }

    private static CosmeticsSnapshot.PlayerWings readWings(JsonObject obj) {
        if (!obj.has("wings")) return null;

        JsonElement wingsElement = obj.get("wings");
        if (wingsElement == null || wingsElement.isJsonNull() || !wingsElement.isJsonObject()) return null;

        JsonObject wings = wingsElement.getAsJsonObject();
        Float size = readOptionalFloat(wings, "size");
        Integer color = readColor(wings, "color");

        return new CosmeticsSnapshot.PlayerWings(
            size == null ? DEFAULT_WING_SIZE : size,
            color == null ? DEFAULT_WING_COLOR : color
        );
    }

    private static Integer readColor(JsonObject obj, String key) {
        if (!obj.has(key)) return null;

        JsonElement element = obj.get(key);
        if (element == null || !element.isJsonPrimitive()) return null;

        String raw = element.getAsString();
        if (raw == null || raw.isBlank()) return null;

        String hex = raw.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.length() != 6 && hex.length() != 8) return null;

        try {
            long value = Long.parseLong(hex, 16);
            return hex.length() == 6 ? (int) (value | 0xFF000000L) : (int) value;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String readDisplayName(JsonObject obj, String key) {
        if (!obj.has(key)) return null;

        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull()) return null;
        if (element.isJsonObject() || element.isJsonArray()) return element.toString();

        return readNullableString(obj, key);
    }

    private static List<String> readAliases(JsonObject obj) {
        if (!obj.has("aliases") || !obj.get("aliases").isJsonArray()) {
            return List.of();
        }

        List<String> aliases = new ArrayList<>();
        for (JsonElement aliasElement : obj.getAsJsonArray("aliases")) {
            if (!aliasElement.isJsonPrimitive()) continue;
            String alias = aliasElement.getAsString();
            if (alias == null || alias.isBlank()) continue;
            aliases.add(alias);
        }
        return aliases;
    }

    private static CosmeticsSnapshot.PlayerScale readScale(JsonObject obj) {
        if (obj.has("scale")) {
            JsonElement scaleElement = obj.get("scale");
            if (scaleElement == null || scaleElement.isJsonNull()) {
                return null;
            }
            if (!scaleElement.isJsonObject()) {
                return null;
            }
            return readScaleObject(scaleElement.getAsJsonObject());
        }

        return readLegacyScale(obj);
    }

    private static CosmeticsSnapshot.PlayerScale readScaleObject(JsonObject obj) {
        Float x = readOptionalFloat(obj, "x");
        Float y = readOptionalFloat(obj, "y");
        Float z = readOptionalFloat(obj, "z");

        if (x == null && y == null && z == null) {
            return null;
        }

        return new CosmeticsSnapshot.PlayerScale(
            x != null ? x : 1.0F,
            y != null ? y : 1.0F,
            z != null ? z : 1.0F
        );
    }

    private static CosmeticsSnapshot.PlayerScale readLegacyScale(JsonObject obj) {
        Float x = readOptionalFloat(obj, "sizeX");
        Float y = readOptionalFloat(obj, "sizeY");
        Float z = readOptionalFloat(obj, "sizeZ");

        if (x == null) x = readOptionalFloat(obj, "x");
        if (y == null) y = readOptionalFloat(obj, "y");
        if (z == null) z = readOptionalFloat(obj, "z");

        if (x == null && y == null && z == null) {
            return null;
        }

        return new CosmeticsSnapshot.PlayerScale(
            x != null ? x : 1.0F,
            y != null ? y : 1.0F,
            z != null ? z : 1.0F
        );
    }

    private static int readVersion(JsonObject obj) {
        if (!obj.has("version")) {
            return 1;
        }

        try {
            return obj.get("version").getAsInt();
        } catch (Exception ignored) {
            return 1;
        }
    }

    private static String readNullableString(JsonObject obj, String key) {
        if (!obj.has(key)) {
            return null;
        }

        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return null;
        }

        return element.getAsString();
    }

    private static Float readOptionalFloat(JsonObject obj, String key) {
        if (!obj.has(key)) {
            return null;
        }

        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return null;
        }

        try {
            return element.getAsFloat();
        } catch (Exception ignored) {
            return null;
        }
    }
}
