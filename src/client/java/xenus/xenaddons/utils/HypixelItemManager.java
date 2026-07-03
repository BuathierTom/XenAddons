package xenus.xenaddons.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import xenus.xenaddons.XenAddons;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class HypixelItemManager {
    public static final List<HypixelItem> SKULL_ITEMS = new ArrayList<>();

    private static boolean loaded = false;

    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("%%.*?%%");

    public static final class HypixelItem {
        private final String name;
        private final String id;
        private final String configId;
        private volatile ItemStack iconStack;

        public HypixelItem(String name, String id, String configId) {
            this.name = name;
            this.id = id;
            this.configId = configId;
        }

        public String name() {
            return name;
        }

        public String id() {
            return id;
        }

        public String configId() {
            return configId;
        }

        public ItemStack iconStack() {
            ItemStack cached = iconStack;
            if (cached != null && !cached.isEmpty()) return cached;

            ItemStack stack = XenAddons.CustomButtonData.getSkullStack(configId);
            if (!stack.isEmpty()) {
                stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(name));
                iconStack = stack;
            }
            return stack;
        }
    }

    public static void loadAsync() {
        if (loaded) return;
        loaded = true;

        CompletableFuture.runAsync(() -> {
            try {
                XenAddons.LOGGER.info("Fetching Hypixel Skyblock items...");
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.hypixel.net/v2/resources/skyblock/items"))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    parseItems(response.body());
                    XenAddons.LOGGER.info("Loaded " + SKULL_ITEMS.size() + " skulls from Hypixel.");
                } else {
                    XenAddons.LOGGER.error("Failed to fetch Hypixel items: HTTP " + response.statusCode());
                }
            } catch (Exception e) {
                XenAddons.LOGGER.error("Error loading Hypixel items", e);
            }
        });
    }

    private static void parseItems(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("items")) return;

            JsonArray items = root.getAsJsonArray("items");
            for (JsonElement el : items) {
                if (!el.isJsonObject()) continue;

                try {
                    JsonObject obj = el.getAsJsonObject();

                    String rawName = getSafeString(obj, "name", "Unknown");
                    String name = cleanName(rawName); // FIX: Clean the name
                    String id = getSafeString(obj, "id", "UNKNOWN_ID");
                    String material = getSafeString(obj, "material", "STONE");

                    if ((material.equals("SKULL_ITEM") || material.equals("PLAYER_HEAD")) && obj.has("skin")) {
                        String skinBase64 = null;
                        JsonElement skinEl = obj.get("skin");

                        if (skinEl.isJsonPrimitive()) {
                            skinBase64 = skinEl.getAsString();
                        } else if (skinEl.isJsonObject()) {
                            JsonObject skinObj = skinEl.getAsJsonObject();
                            if (skinObj.has("value")) {
                                skinBase64 = skinObj.get("value").getAsString();
                            }
                        }

                        if (skinBase64 != null) {
                            String hash = extractHash(skinBase64);
                            if (hash != null) {
                                String configId = "skull:" + hash;
                                synchronized (SKULL_ITEMS) {
                                    SKULL_ITEMS.add(new HypixelItem(name, id, configId));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // continue
                }
            }
        } catch (Exception e) {
            XenAddons.LOGGER.error("Error parsing Hypixel items JSON", e);
        }
    }

    private static String cleanName(String name) {
        if (name == null) return "";
        return COLOR_CODE_PATTERN.matcher(name).replaceAll("").trim();
    }

    private static String getSafeString(JsonObject obj, String key, String def) {
        if (obj.has(key)) {
            JsonElement el = obj.get(key);
            if (el.isJsonPrimitive()) {
                return el.getAsString();
            }
        }
        return def;
    }

    private static String extractHash(String base64) {
        try {
            String json = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
            int urlIndex = json.indexOf("http://textures.minecraft.net/texture/");
            if (urlIndex == -1) return null;

            int hashStart = urlIndex + "http://textures.minecraft.net/texture/".length();
            int hashEnd = json.indexOf("\"", hashStart);
            if (hashEnd == -1) hashEnd = json.length();

            return json.substring(hashStart, hashEnd);
        } catch (Exception e) {
            return null;
        }
    }
}
