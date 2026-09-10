package xenus.xenaddons.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import xenus.xenaddons.XenAddons;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public final class ItemPriceManager {
    private static final String HYPIXEL_BAZAAR_API_URL = "https://api.hypixel.net/v2/skyblock/bazaar";
    private static final String LOWEST_BIN_API_URL = "https://api.xenus.fr/v1/skyblock/lowestbin";
    private static final long REFRESH_MS = 5 * 60 * 1000L;
    private static final Pattern PET_LEVEL_PREFIX_PATTERN = Pattern.compile("^\\s*\\[\\s*Lvl\\s+\\d+\\s*]\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PET_SUFFIX_PATTERN = Pattern.compile("\\s+Pet\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHARD_NAME_PATTERN = Pattern.compile("^(.+?)\\s+Shard$", Pattern.CASE_INSENSITIVE);
    // The "COMMON FOREST SHARD (ID C49)" footer every shard entry carries.
    private static final Pattern SHARD_FOOTER_PATTERN = Pattern.compile("\\bSHARD\\b\\s*\\(\\s*ID\\s+[^)]+\\)", Pattern.CASE_INSENSITIVE);
    private static final String[] PET_RARITIES = {
        "VERY SPECIAL",
        "SPECIAL",
        "DIVINE",
        "MYTHIC",
        "LEGENDARY",
        "EPIC",
        "RARE",
        "UNCOMMON",
        "COMMON"
    };
    private static final Set<String> ULTIMATE_ENCHANT_BASE_NAMES = Set.of(
        "BANK",
        "BOBBIN_TIME",
        "CHIMERA",
        "COMBO",
        "CROP_FEVER",
        "DUPLEX",
        "FATAL_TEMPO",
        "FLASH",
        "HABANERO_TACTICS",
        "HECATOMB",
        "INFERNO",
        "LAST_STAND",
        "LEGION",
        "NO_PAIN_NO_GAIN",
        "ONE_FOR_ALL",
        "REND",
        "SOUL_EATER",
        "SWARM",
        "THE_ONE",
        "JERRY",
        "WISE",
        "WISDOM"
    );

    private static final Map<String, Double> LOWEST_BIN_BY_ID = new ConcurrentHashMap<>();
    private static final Map<String, BazaarPrice> BAZAAR_BY_ID = new ConcurrentHashMap<>();

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final AtomicBoolean FETCHING = new AtomicBoolean(false);
    private static volatile long lastFetchAt = 0L;

    private ItemPriceManager() {}

    public static void requestRefreshIfNeeded() {
        long now = System.currentTimeMillis();
        if ((now - lastFetchAt) < REFRESH_MS && !LOWEST_BIN_BY_ID.isEmpty() && !BAZAAR_BY_ID.isEmpty()) {
            return;
        }
        if (!FETCHING.compareAndSet(false, true)) return;

        Thread.startVirtualThread(() -> {
            try {
                Map<String, BazaarPrice> bz = fetchBazaarPrices();
                if (!bz.isEmpty()) {
                    BAZAAR_BY_ID.clear();
                    BAZAAR_BY_ID.putAll(bz);
                }

                Map<String, Double> lb = fetchLowestBins();
                if (!lb.isEmpty()) {
                    LOWEST_BIN_BY_ID.clear();
                    LOWEST_BIN_BY_ID.putAll(lb);
                }

                lastFetchAt = System.currentTimeMillis();
            } catch (Exception e) {
                XenAddons.LOGGER.warn("Failed to load Item Price data", e);
            } finally {
                FETCHING.set(false);
            }
        });
    }

    public static Double getLowestBin(String itemId) {
        if (itemId == null || itemId.isBlank()) return null;
        return LOWEST_BIN_BY_ID.get(itemId);
    }

    public static BazaarPrice getBazaarPrice(String itemId) {
        if (itemId == null || itemId.isBlank()) return null;
        return BAZAAR_BY_ID.get(itemId);
    }

    public static String extractSkyBlockId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;

        CompoundTag root = customData.copyTag();
        if (root == null) return null;

        CompoundTag extra = root.getCompound("ExtraAttributes")
            .orElse(root.getCompound("extra_attributes").orElse(null));
        if (extra != null) {
            String id = readString(extra, "id");
            if (id != null) return id;
            id = readString(extra, "ID");
            if (id != null) return id;
        }

        String direct = readString(root, "id");
        if (direct != null) return direct;
        return readString(root, "ID");
    }

    public static String resolvePricingId(ItemStack stack) {
        String sbId = extractSkyBlockId(stack);
        String petPricingId = resolvePetPricingId(stack, sbId);
        if (petPricingId != null) return petPricingId;

        if (isEnchantedBookStack(stack, sbId)) {
            String converted = resolveEnchantedBookPricingId(stack);
            if (converted != null) return converted;
            if ("ENCHANTED_BOOK".equals(sbId)) return sbId;
        }

        String shardPricingId = resolveShardPricingId(stack);
        if (shardPricingId != null) return shardPricingId;

        return sbId;
    }

    public static String resolveQuickSearchName(ItemStack stack) {
        if (!isEnchantedBookStack(stack, extractSkyBlockId(stack))) return null;

        String fromDisplay = resolveEnchantedBookSearchNameFromDisplay(stack);
        if (fromDisplay != null) return fromDisplay;

        String fromLore = resolveEnchantedBookSearchNameFromLore(stack);
        if (fromLore != null) return fromLore;

        return formatEnchantmentSearchName(resolveEnchantedBookPricingIdFromNbt(stack));
    }

    public static String formatCoins(double value) {
        double abs = Math.abs(value);
        if (abs >= 1_000_000_000d) return trim(value / 1_000_000_000d) + "B";
        if (abs >= 1_000_000d) return trim(value / 1_000_000d) + "M";
        if (abs >= 1_000d) return trim(value / 1_000d) + "K";
        return new DecimalFormat("#,###").format(Math.round(value));
    }

    private static String trim(double value) {
        String s = String.format(Locale.US, "%.2f", value);
        if (s.endsWith(".00")) return s.substring(0, s.length() - 3);
        if (s.endsWith("0")) return s.substring(0, s.length() - 1);
        return s;
    }

    private static String readString(CompoundTag nbt, String key) {
        if (nbt == null || key == null) return null;
        String value = nbt.getStringOr(key, "");
        if (value == null || value.isBlank()) return null;
        return value;
    }

    private static boolean isEnchantedBookStack(ItemStack stack, String sbId) {
        return "ENCHANTED_BOOK".equals(sbId)
            || (stack != null && !stack.isEmpty() && stack.is(Items.ENCHANTED_BOOK));
    }

    private static String resolvePetPricingId(ItemStack stack, String sbId) {
        if (stack == null || stack.isEmpty()) return null;

        String petInfo = readPetInfo(stack);
        String petPricingId = resolvePetPricingIdFromInfo(petInfo);
        if (petPricingId != null) return petPricingId;

        if (!"PET".equals(sbId)) return null;

        String petName = resolvePetNameFromDisplay(stack);
        String petRarity = resolvePetRarityFromLore(stack);
        return buildPetPricingId(petName, petRarity);
    }

    private static String readPetInfo(ItemStack stack) {
        CompoundTag root = getCustomDataTag(stack);
        if (root == null) return null;

        CompoundTag extra = root.getCompound("ExtraAttributes")
            .orElse(root.getCompound("extra_attributes").orElse(null));
        String petInfo = readString(extra, "petInfo");
        if (petInfo != null) return petInfo;

        return readString(root, "petInfo");
    }

    private static String resolvePetPricingIdFromInfo(String petInfo) {
        if (petInfo == null || petInfo.isBlank()) return null;

        try {
            JsonElement rootElement = JsonParser.parseString(petInfo);
            if (rootElement == null || !rootElement.isJsonObject()) return null;

            JsonObject root = rootElement.getAsJsonObject();
            String type = readJsonString(root, "type");
            String tier = readJsonString(root, "tier");
            if (tier == null) tier = readJsonString(root, "rarity");

            return buildPetPricingId(type, tier);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String resolvePetNameFromDisplay(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        String displayName = stripFormatting(stack.getHoverName().getString());
        if (displayName == null || displayName.isBlank()) return null;

        String name = PET_LEVEL_PREFIX_PATTERN.matcher(displayName).replaceFirst("").trim();
        name = PET_SUFFIX_PATTERN.matcher(name).replaceFirst("").trim();
        return name.isBlank() ? null : name;
    }

    private static String resolvePetRarityFromLore(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null || lore.lines().isEmpty()) return null;

        for (int i = lore.lines().size() - 1; i >= 0; i--) {
            Component component = lore.lines().get(i);
            if (component == null) continue;

            String line = stripFormatting(component.getString());
            if (line == null || line.isBlank()) continue;

            String upper = line.toUpperCase(Locale.ROOT);
            if (!upper.contains("PET")) continue;

            for (String rarity : PET_RARITIES) {
                if (upper.contains(rarity)) return rarity;
            }
        }

        return null;
    }

    private static String buildPetPricingId(String petName, String rarity) {
        String normalizedPet = normalizeApiPart(petName);
        String normalizedRarity = normalizeApiPart(rarity);
        if (normalizedPet == null || normalizedRarity == null) return null;

        if (normalizedPet.endsWith("_PET")) {
            normalizedPet = normalizedPet.substring(0, normalizedPet.length() - 4);
        }
        if (normalizedPet.isBlank()) return null;

        return normalizedPet + "_PET_" + normalizedRarity;
    }

    private static String resolveShardPricingId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        String shardId = shardIdCandidate(stripFormatting(stack.getHoverName().getString()), readLoreLines(stack));
        if (shardId == null) return null;

        // Guard: a name that merely looks like a shard resolves to no price rather than a wrong one.
        return BAZAAR_BY_ID.containsKey(shardId) ? shardId : null;
    }

    /**
     * The Bazaar id a shard would have. Held as an item the entry is named "Foxtrot Shard", but in
     * the shard menu it is named after the attribute alone and only the trailing
     * "COMMON FOREST SHARD (ID C49)" line says what it really is.
     */
    static String shardIdCandidate(String displayName, List<String> loreLines) {
        if (displayName == null || displayName.isBlank()) return null;

        var matcher = SHARD_NAME_PATTERN.matcher(displayName);
        String shardName = matcher.matches()
            ? normalizeApiPart(matcher.group(1))
            : (hasShardFooter(loreLines) ? normalizeApiPart(displayName) : null);
        if (shardName == null) return null;

        return "SHARD_" + shardName;
    }

    private static boolean hasShardFooter(List<String> loreLines) {
        if (loreLines == null) return false;

        for (String line : loreLines) {
            if (line != null && SHARD_FOOTER_PATTERN.matcher(line).find()) return true;
        }
        return false;
    }

    private static List<String> readLoreLines(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null || lore.lines().isEmpty()) return List.of();

        List<String> lines = new ArrayList<>(lore.lines().size());
        for (Component line : lore.lines()) {
            if (line != null) lines.add(stripFormatting(line.getString()));
        }
        return lines;
    }

    private static String normalizeApiPart(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = stripFormatting(value)
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]+", "_")
            .replaceAll("^_+|_+$", "");
        return normalized.isBlank() ? null : normalized;
    }

    private static String readJsonString(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)) return null;
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive()) return null;
        String string = value.getAsString();
        return string == null || string.isBlank() ? null : string;
    }

    private static CompoundTag getCustomDataTag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? null : customData.copyTag();
    }

    private static String resolveEnchantedBookPricingId(ItemStack stack) {
        String fromNbt = resolveEnchantedBookPricingIdFromNbt(stack);
        if (fromNbt != null) return fromNbt;

        String fromDisplay = resolveEnchantedBookPricingIdFromDisplay(stack);
        if (fromDisplay != null) return fromDisplay;

        return resolveEnchantedBookPricingIdFromLore(stack);
    }

    private static String resolveEnchantedBookPricingIdFromNbt(ItemStack stack) {
        CompoundTag root = getCustomDataTag(stack);
        if (root == null) return null;

        CompoundTag extra = root.getCompound("ExtraAttributes")
            .orElse(root.getCompound("extra_attributes").orElse(root));
        CompoundTag enchantments = extra.getCompound("enchantments").orElse(null);
        if (enchantments == null || enchantments.isEmpty()) return null;

        for (String enchantName : enchantments.keySet()) {
            int level = enchantments.getIntOr(enchantName, 0);
            String pricingId = buildEnchantmentPricingId(enchantName, level);
            if (pricingId != null) return pricingId;
        }

        return null;
    }

    private static String resolveEnchantedBookPricingIdFromDisplay(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return enchantNameToId(stack.getHoverName().getString());
    }

    private static String resolveEnchantedBookPricingIdFromLore(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null || lore.lines().isEmpty()) return null;

        for (Component component : lore.lines()) {
            if (component == null) continue;
            String pricingId = enchantNameToId(component.getString());
            if (pricingId != null) return pricingId;
        }

        return null;
    }

    private static String resolveEnchantedBookSearchNameFromDisplay(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return resolveEnchantedBookSearchNameFromText(stack.getHoverName().getString());
    }

    private static String resolveEnchantedBookSearchNameFromLore(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null || lore.lines().isEmpty()) return null;

        for (Component component : lore.lines()) {
            if (component == null) continue;
            String searchName = resolveEnchantedBookSearchNameFromText(component.getString());
            if (searchName != null) return searchName;
        }

        return null;
    }

    private static String resolveEnchantedBookSearchNameFromText(String text) {
        String enchantmentText = extractEnchantmentText(text);
        if (enchantmentText == null || enchantmentText.isBlank()) return null;
        return enchantNameToId(enchantmentText) == null ? null : enchantmentText;
    }

    private static String stripFormatting(String input) {
        if (input == null) return null;
        return input.replaceAll("(?i)[§&][0-9A-FK-OR]", "").trim();
    }

    private static String enchantNameToId(String loreLine) {
        if (loreLine == null || loreLine.isBlank()) return null;

        String cleaned = extractEnchantmentText(loreLine);
        if (cleaned == null || cleaned.isBlank()) return null;

        int lastSpace = cleaned.lastIndexOf(' ');
        if (lastSpace <= 0 || lastSpace >= cleaned.length() - 1) return null;

        String namePart = cleaned.substring(0, lastSpace).trim();
        String levelPart = cleaned.substring(lastSpace + 1).replaceAll("[^A-Za-z0-9]", "").trim();
        int level = parseLevel(levelPart);
        if (level <= 0) return null;

        return buildEnchantmentPricingId(namePart, level);
    }

    private static String extractEnchantmentText(String loreLine) {
        String cleaned = stripFormatting(loreLine);
        if (cleaned == null || cleaned.isBlank()) return null;
        if ("Enchanted Book".equalsIgnoreCase(cleaned)) return null;

        int bookPrefix = cleaned.indexOf("Enchanted Book (");
        if (bookPrefix >= 0) {
            int open = cleaned.indexOf('(', bookPrefix);
            int close = cleaned.lastIndexOf(')');
            if (open >= 0 && close > open) {
                return cleaned.substring(open + 1, close).trim();
            }
        }

        return cleaned;
    }

    private static String buildEnchantmentPricingId(String enchantName, int level) {
        if (level <= 0) return null;

        String normalized = normalizeApiPart(enchantName);
        if (normalized == null) return null;

        if (normalized.startsWith("ULTIMATE_")) {
            return "ENCHANTMENT_" + normalized + "_" + level;
        }

        String normalId = "ENCHANTMENT_" + normalized + "_" + level;
        String ultimateId = "ENCHANTMENT_ULTIMATE_" + normalized + "_" + level;
        if (ULTIMATE_ENCHANT_BASE_NAMES.contains(normalized) || BAZAAR_BY_ID.containsKey(ultimateId)) {
            return ultimateId;
        }

        return normalId;
    }

    private static String formatEnchantmentSearchName(String pricingId) {
        if (pricingId == null || !pricingId.startsWith("ENCHANTMENT_")) return null;

        String body = pricingId.substring("ENCHANTMENT_".length());
        int split = body.lastIndexOf('_');
        if (split <= 0 || split >= body.length() - 1) return null;

        String levelPart = body.substring(split + 1);
        int level;
        try {
            level = Integer.parseInt(levelPart);
        } catch (Exception ignored) {
            return null;
        }

        String name = body.substring(0, split);
        if (name.startsWith("ULTIMATE_")) {
            name = name.substring("ULTIMATE_".length());
        }

        String displayName = titleCaseApiPart(name);
        if (displayName == null) return null;
        return displayName + " " + intToRoman(level);
    }

    private static String titleCaseApiPart(String value) {
        if (value == null || value.isBlank()) return null;

        String[] parts = value.toLowerCase(Locale.ROOT).split("_+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) out.append(part.substring(1));
        }

        return out.isEmpty() ? null : out.toString();
    }

    private static String intToRoman(int value) {
        if (value <= 0) return Integer.toString(value);
        int[] numbers = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] romans = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder out = new StringBuilder();
        int remaining = value;
        for (int i = 0; i < numbers.length; i++) {
            while (remaining >= numbers[i]) {
                out.append(romans[i]);
                remaining -= numbers[i];
            }
        }
        return out.toString();
    }

    private static int parseLevel(String level) {
        try {
            return Integer.parseInt(level);
        } catch (Exception ignored) {}
        return romanToInt(level);
    }

    private static int romanToInt(String roman) {
        if (roman == null || roman.isBlank()) return -1;
        String upper = roman.toUpperCase(Locale.ROOT);
        int total = 0;
        int prev = 0;
        for (int i = upper.length() - 1; i >= 0; i--) {
            int curr = switch (upper.charAt(i)) {
                case 'I' -> 1;
                case 'V' -> 5;
                case 'X' -> 10;
                case 'L' -> 50;
                case 'C' -> 100;
                case 'D' -> 500;
                case 'M' -> 1000;
                default -> -1;
            };
            if (curr < 0) return -1;
            if (curr < prev) total -= curr;
            else total += curr;
            prev = curr;
        }
        return total;
    }

    private static Map<String, BazaarPrice> fetchBazaarPrices() {
        Map<String, BazaarPrice> out = new HashMap<>();
        JsonObject root = fetchJsonObject(HYPIXEL_BAZAAR_API_URL);
        if (root == null || !root.has("products") || !root.get("products").isJsonObject()) return out;

        JsonObject products = root.getAsJsonObject("products");
        for (Map.Entry<String, JsonElement> entry : products.entrySet()) {
            String id = entry.getKey();
            JsonObject product = entry.getValue().getAsJsonObject();

            Double buy = readSummaryPrice(product, "buy_summary");
            Double sell = readSummaryPrice(product, "sell_summary");
            Double instaBuy = readQuickStatusPrice(product, "buyPrice");
            Double instaSell = readQuickStatusPrice(product, "sellPrice");

            if (buy == null && sell == null && instaBuy == null && instaSell == null) continue;
            out.put(id, new BazaarPrice(buy, sell, instaBuy, instaSell));
        }

        return out;
    }

    private static Map<String, Double> fetchLowestBins() {
        Map<String, Double> out = new HashMap<>();
        JsonElement rootElement = fetchJsonElement(LOWEST_BIN_API_URL);
        if (rootElement == null || !rootElement.isJsonObject()) return out;

        JsonObject root = rootElement.getAsJsonObject();
        if (!root.has("auction") || !root.get("auction").isJsonObject()) return out;
        JsonObject source = root.getAsJsonObject("auction");

        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            JsonElement value = entry.getValue();
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) continue;
            out.put(entry.getKey(), value.getAsDouble());
        }

        return out;
    }

    private static Double readSummaryPrice(JsonObject product, String field) {
        if (!product.has(field) || !product.get(field).isJsonArray()) return null;
        var arr = product.getAsJsonArray(field);
        if (arr.isEmpty() || !arr.get(0).isJsonObject()) return null;
        JsonObject first = arr.get(0).getAsJsonObject();
        if (!first.has("pricePerUnit") || !first.get("pricePerUnit").isJsonPrimitive()) return null;
        return first.get("pricePerUnit").getAsDouble();
    }

    private static Double readQuickStatusPrice(JsonObject product, String field) {
        if (!product.has("quick_status") || !product.get("quick_status").isJsonObject()) return null;
        JsonObject quick = product.getAsJsonObject("quick_status");
        if (!quick.has(field) || !quick.get(field).isJsonPrimitive()) return null;
        return quick.get(field).getAsDouble();
    }

    private static JsonObject fetchJsonObject(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) return null;

            JsonElement root = JsonParser.parseString(response.body());
            if (!root.isJsonObject()) return null;
            return root.getAsJsonObject();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonElement fetchJsonElement(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) return null;
            return JsonParser.parseString(response.body());
        } catch (Exception ignored) {
            return null;
        }
    }

    public record BazaarPrice(Double buy, Double sell, Double instaBuy, Double instaSell) {}
}
