package xenus.xenaddons.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import xenus.xenaddons.XenAddons;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WarpShortcutsManager {

    public static final String DEFAULT_API_URL = "https://api.xenus.fr/v1/skyblock/warpshortcuts";
    public static final String API_URL_PROPERTY = "xenaddons.warpshortcuts.api.url";

    private static final String FALLBACK_RESOURCE = "/assets/xenaddons/skyblock/warpShortcuts.json";

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_NAME_LENGTH = 32;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();

    private static final AtomicBoolean LOADING = new AtomicBoolean(false);
    private static volatile Map<String, String> shortcuts = Map.of();

    private WarpShortcutsManager() {
    }

    public static Map<String, String> getShortcuts() {
        return shortcuts;
    }

    public static String getWarpTarget(String alias) {
        if (alias == null || alias.isBlank()) return null;
        return shortcuts.get(alias.toLowerCase(Locale.ROOT));
    }

    public static String endpoint() {
        String configured = System.getProperty(API_URL_PROPERTY);
        return configured == null || configured.isBlank() ? DEFAULT_API_URL : configured;
    }

    public static void loadAsync(Runnable onLoaded) {
        if (!LOADING.compareAndSet(false, true)) return;

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, String> loaded = loadFromApi();
                if (loaded.isEmpty()) loaded = loadFromFallback();
                if (loaded.isEmpty()) return;

                shortcuts = loaded;

                if (onLoaded == null) return;
                Minecraft client = Minecraft.getInstance();
                if (client != null) client.execute(onLoaded);
            } finally {
                LOADING.set(false);
            }
        });
    }

    private static Map<String, String> loadFromApi() {
        try {
            Map<String, String> parsed = parse(fetch());
            if (parsed.isEmpty()) {
                XenAddons.LOGGER.warn("Warp shortcuts API {} returned no usable entry, using bundled fallback resource", endpoint());
                return Map.of();
            }

            XenAddons.LOGGER.info("Loaded {} warp shortcuts from API {}", parsed.size(), endpoint());
            return parsed;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            XenAddons.LOGGER.warn("Interrupted while loading warp shortcuts from API {}, using bundled fallback resource", endpoint(), e);
            return Map.of();
        } catch (Exception e) {
            XenAddons.LOGGER.warn("Failed to load warp shortcuts from API {}, using bundled fallback resource", endpoint(), e);
            return Map.of();
        }
    }

    private static Map<String, String> loadFromFallback() {
        InputStream stream = WarpShortcutsManager.class.getResourceAsStream(FALLBACK_RESOURCE);
        if (stream == null) {
            XenAddons.LOGGER.error("Bundled warp shortcuts fallback resource is missing: {}", FALLBACK_RESOURCE);
            return Map.of();
        }

        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            Map<String, String> parsed = parseReader(reader);
            XenAddons.LOGGER.info("Loaded {} warp shortcuts from bundled fallback resource {}", parsed.size(), FALLBACK_RESOURCE);
            return parsed;
        } catch (Exception e) {
            XenAddons.LOGGER.error("Failed to parse bundled warp shortcuts fallback resource {}", FALLBACK_RESOURCE, e);
            return Map.of();
        }
    }

    private static String fetch() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint()))
            .timeout(REQUEST_TIMEOUT)
            .header("Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("Unexpected HTTP status " + response.statusCode() + " for " + endpoint());
        }

        return response.body();
    }

    static Map<String, String> parse(String json) {
        return parseRoot(JsonParser.parseString(json));
    }

    static Map<String, String> parseReader(Reader reader) {
        return parseRoot(JsonParser.parseReader(reader));
    }

    private static Map<String, String> parseRoot(JsonElement root) {
        if (root == null || !root.isJsonObject()) {
            throw new JsonParseException("Warp shortcuts payload root must be a JSON object");
        }

        JsonObject entries = root.getAsJsonObject();
        if (entries.has("shortcuts") && entries.get("shortcuts").isJsonObject()) {
            entries = entries.getAsJsonObject("shortcuts");
        }

        LinkedHashMap<String, String> parsed = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : entries.entrySet()) {
            String alias = readAlias(entry.getKey());
            if (alias == null) continue;

            JsonElement value = entry.getValue();
            if (value == null || !value.isJsonPrimitive()) continue;

            String target = readTarget(value.getAsString());
            if (target == null) continue;

            parsed.put(alias, target);
        }

        return Collections.unmodifiableMap(parsed);
    }

    private static String readAlias(String raw) {
        if (raw == null) return null;

        String alias = raw.trim().toLowerCase(Locale.ROOT);
        if (alias.isEmpty() || alias.length() > MAX_NAME_LENGTH) return null;

        for (int index = 0; index < alias.length(); index++) {
            char character = alias.charAt(index);
            boolean allowed = (character >= 'a' && character <= 'z')
                || (character >= '0' && character <= '9')
                || character == '_'
                || character == '-';
            if (!allowed) return null;
        }

        return alias;
    }

    private static String readTarget(String raw) {
        if (raw == null) return null;

        String target = raw.trim();
        if (target.isEmpty() || target.length() > MAX_NAME_LENGTH) return null;

        for (int index = 0; index < target.length(); index++) {
            char character = target.charAt(index);
            if (character <= ' ' || character == 0x7F) return null;
        }

        return target;
    }
}
