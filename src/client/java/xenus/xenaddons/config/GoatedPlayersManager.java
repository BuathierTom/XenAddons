package xenus.xenaddons.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import xenus.xenaddons.cosmetics.CosmeticsRepository;
import xenus.xenaddons.cosmetics.CosmeticsSnapshot.PlayerCosmetic;
import xenus.xenaddons.cosmetics.CosmeticsSnapshot.PlayerScale;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoatedPlayersManager {
    private static final Pattern LEGACY_AMP_PATTERN = Pattern.compile("(?i)&([0-9A-FK-OR])");
    private static final Pattern HEX_AMP_PATTERN = Pattern.compile("(?i)&\\#([0-9a-f]{6})");
    private static final Pattern LEGACY_SECTION_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");

    private static final String MOJANG_PROFILE_BY_UUID = "https://sessionserver.mojang.com/session/minecraft/profile/";

    private static final String CAPY_MARKER = "{capy}";
    private static final String FRANCE_MARKER = "{france}";
    private static final String LIP_MARKER = "{lip}";
    private static final String CAPY_GLYPH = "\uE100";
    private static final String FRANCE_GLYPH = "\uE101";
    private static final String LIP_GLYPH = "\uE102";
    private static final Identifier CAPY_FONT = Identifier.of("xenaddons", "capybara");
    private static final Identifier FRANCE_FONT = Identifier.of("xenaddons", "france");
    private static final Identifier LIP_FONT = Identifier.of("xenaddons", "lip");

    private static final String[] CONCAT_STATUS_SUFFIXES = new String[] {"offline", "online"};
    private static final Map<String, String> NAMED_COLOR_TO_LEGACY = buildNamedColorMap();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private static final Map<UUID, CosmeticEntry> UUID_COSMETICS = new LinkedHashMap<>();
    private static final Map<String, CosmeticEntry> STATIC_NAME_COSMETICS = new LinkedHashMap<>();
    private static final Map<String, CosmeticEntry> RUNTIME_NAME_COSMETICS = new LinkedHashMap<>();
    private static final Map<UUID, String> UUID_NAME_CACHE = new ConcurrentHashMap<>();
    private static final Set<UUID> UUID_LOOKUP_IN_FLIGHT = ConcurrentHashMap.newKeySet();
    private static final long RUNTIME_REBUILD_INTERVAL_MS = 1_000L;
    private static final int VISUAL_STRING_CACHE_MAX = 4096;
    private static volatile long lastRuntimeRebuildMs = 0L;
    private static volatile List<Map.Entry<String, CosmeticEntry>> SORTED_RUNTIME_ENTRIES = List.of();
    private static volatile Map<String, CosmeticEntry> TOKEN_NAME_COSMETICS = Map.of();
    private static volatile List<Map.Entry<String, CosmeticEntry>> SPECIAL_NAME_ENTRIES = List.of();
    private static final Map<String, String> VISUAL_STRING_CACHE = Collections.synchronizedMap(new LinkedHashMap<>(VISUAL_STRING_CACHE_MAX, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > VISUAL_STRING_CACHE_MAX;
        }
    });

    public static boolean isGoated(String name) {
        return getEntryForName(name) != null;
    }

    public static String getDisplayName(String name) {
        CosmeticEntry entry = getEntryForName(name);
        return entry != null ? entry.displayName : (name == null ? "" : name);
    }

    public static int[] getHighlightColor(String name) {
        CosmeticEntry entry = getEntryForName(name);
        return entry != null ? entry.highlightColor.clone() : null;
    }

    private static CosmeticEntry getEntryForName(String name) {
        if (name == null || name.isBlank()) return null;
        rebuildRuntimeNameMap();
        return RUNTIME_NAME_COSMETICS.get(name.toLowerCase());
    }

    public static String applyVisualWords(String text) {
        return applyVisualWordsInternal(text, false);
    }

    // Safe path for plain String rendering contexts (chat tabs filters, etc.).
    // Never return raw legacy formatting markers here.
    public static String applyVisualWordsPlain(String text) {
        String replaced = applyVisualWordsInternal(text, false);
        return stripLegacyCodes(replaced);
    }

    public static Map<String, String> getRuntimeReplaceMap() {
        rebuildRuntimeNameMap();
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, CosmeticEntry> entry : RUNTIME_NAME_COSMETICS.entrySet()) {
            CosmeticEntry value = entry.getValue();
            if (value == null || !value.hasDisplayName()) continue;
            out.put(entry.getKey(), value.displayName);
        }
        return out;
    }

    private static String applyVisualWordsInternal(String text, boolean keepMarkers) {
        if (text == null || text.isEmpty()) return text;
        String cacheKey = (keepMarkers ? "1|" : "0|") + text;
        String cached = VISUAL_STRING_CACHE.get(cacheKey);
        if (cached != null) return cached;

        rebuildRuntimeNameMap();
        if (SORTED_RUNTIME_ENTRIES.isEmpty()) {
            String plain = keepMarkers ? text : text
                .replace(CAPY_MARKER, "")
                .replace(FRANCE_MARKER, "")
                .replace(LIP_MARKER, "");
            VISUAL_STRING_CACHE.put(cacheKey, plain);
            return plain;
        }
        String result = text;

        if (!TOKEN_NAME_COSMETICS.isEmpty()) {
            result = replaceTokenizedNames(result);
        }

        if (!SPECIAL_NAME_ENTRIES.isEmpty()) {
            String loweredResult = result.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, CosmeticEntry> entry : SPECIAL_NAME_ENTRIES) {
                String key = entry.getKey();
                String replacement = boundarySafeDisplayName(entry.getValue().displayName);
                if (key == null || key.isEmpty() || replacement == null) continue;
                String lowerKey = key.toLowerCase(Locale.ROOT);
                if (!loweredResult.contains(lowerKey)) continue;
                result = replaceIgnoreCase(result, key, replacement);
                loweredResult = result.toLowerCase(Locale.ROOT);
            }
        }

        if (!keepMarkers) {
            result = result.replace(CAPY_MARKER, "");
            result = result.replace(FRANCE_MARKER, "");
            result = result.replace(LIP_MARKER, "");
        }

        result = trimDanglingSectionSigns(result);

        VISUAL_STRING_CACHE.put(cacheKey, result);
        return result;
    }

    private static String replaceIgnoreCase(String input, String target, String replacement) {
        if (input == null || input.isEmpty() || target == null || target.isEmpty()) return input;

        StringBuilder out = new StringBuilder(input.length());
        int from = 0;
        int max = input.length() - target.length();
        while (from <= max) {
            int found = indexOfIgnoreCase(input, target, from);
            if (found < 0) break;
            if (isInsideExistingReplacement(input, found, target, replacement)) {
                out.append(input, from, found + target.length());
                from = found + target.length();
                continue;
            }
            out.append(input, from, found).append(replacement);
            from = found + target.length();
        }
        if (from < input.length()) out.append(input, from, input.length());
        return out.toString();
    }

    private static boolean isInsideExistingReplacement(String input, int targetStart, String target, String replacement) {
        if (replacement == null || replacement.isEmpty()) return false;
        int targetLen = target.length();
        String loweredReplacement = replacement.toLowerCase();
        String loweredTarget = target.toLowerCase();

        int pos = loweredReplacement.indexOf(loweredTarget);
        while (pos >= 0) {
            int wholeStart = targetStart - pos;
            int wholeEnd = wholeStart + replacement.length();
            if (wholeStart >= 0 && wholeEnd <= input.length()
                && input.regionMatches(true, wholeStart, replacement, 0, replacement.length())) {
                return true;
            }
            pos = loweredReplacement.indexOf(loweredTarget, pos + 1);
        }
        return false;
    }

    private static int indexOfIgnoreCase(String text, String needle, int fromIndex) {
        int max = text.length() - needle.length();
        for (int i = Math.max(0, fromIndex); i <= max; i++) {
            if (text.regionMatches(true, i, needle, 0, needle.length())) return i;
        }
        return -1;
    }

    private static boolean isTokenChar(char c) {
        return (c >= 'a' && c <= 'z')
            || (c >= 'A' && c <= 'Z')
            || (c >= '0' && c <= '9')
            || c == '_';
    }

    private static String trimDanglingSectionSigns(String input) {
        if (input == null || input.isEmpty()) return input;
        int end = input.length();
        while (end > 0 && input.charAt(end - 1) == '§') {
            end--;
        }
        return end == input.length() ? input : input.substring(0, end);
    }

    private static String stripLegacyCodes(String input) {
        if (input == null || input.isEmpty()) return input;
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '§') {
                if (i + 1 < input.length()) {
                    i++;
                }
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String replaceTokenizedNames(String input) {
        if (input == null || input.isEmpty()) return input;
        StringBuilder out = new StringBuilder(input.length() + 16);
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (!isTokenChar(c)) {
                out.append(c);
                i++;
                continue;
            }

            int start = i;
            i++;
            while (i < input.length() && isTokenChar(input.charAt(i))) i++;
            String token = input.substring(start, i);
            CosmeticEntry entry = TOKEN_NAME_COSMETICS.get(token.toLowerCase(Locale.ROOT));
            if (entry != null && entry.hasDisplayName()) {
                String replacement = boundarySafeDisplayName(entry.displayName);
                if (replacement != null) {
                    out.append(replacement);
                    continue;
                }
            }
            out.append(token);
        }
        return out.toString();
    }

    private static List<Map.Entry<String, CosmeticEntry>> getNameEntriesSorted() {
        List<Map.Entry<String, CosmeticEntry>> entries = new ArrayList<>(RUNTIME_NAME_COSMETICS.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
        return entries;
    }

    private static void rebuildRuntimeNameMap() {
        long now = System.currentTimeMillis();
        if (now - lastRuntimeRebuildMs < RUNTIME_REBUILD_INTERVAL_MS && !RUNTIME_NAME_COSMETICS.isEmpty()) return;
        synchronized (GoatedPlayersManager.class) {
            now = System.currentTimeMillis();
            if (now - lastRuntimeRebuildMs < RUNTIME_REBUILD_INTERVAL_MS && !RUNTIME_NAME_COSMETICS.isEmpty()) return;

        LinkedHashMap<String, CosmeticEntry> merged = new LinkedHashMap<>(STATIC_NAME_COSMETICS);

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            if (mc.getNetworkHandler() != null) {
                for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                    if (entry == null || entry.getProfile() == null) continue;
                    UUID uuid = entry.getProfile().id();
                    if (uuid == null) continue;

                    CosmeticEntry cosmetic = UUID_COSMETICS.get(uuid);
                    if (cosmetic == null || !cosmetic.hasDisplayName()) continue;

                    String profileName = entry.getProfile().name();
                    if (profileName == null || profileName.isBlank()) continue;

                    UUID_NAME_CACHE.put(uuid, profileName);
                    merged.put(profileName.toLowerCase(), cosmetic);
                }
            }

            if (mc.world != null) {
                for (PlayerEntity player : mc.world.getPlayers()) {
                    if (player == null) continue;
                    UUID uuid = player.getUuid();
                    if (uuid == null) continue;

                    CosmeticEntry cosmetic = UUID_COSMETICS.get(uuid);
                    if (cosmetic == null || !cosmetic.hasDisplayName()) continue;

                    String name = player.getGameProfile().name();
                    if (name == null || name.isBlank()) continue;

                    UUID_NAME_CACHE.put(uuid, name);
                    merged.put(name.toLowerCase(), cosmetic);
                }
            }
        }

        for (Map.Entry<UUID, String> cached : UUID_NAME_CACHE.entrySet()) {
            CosmeticEntry cosmetic = UUID_COSMETICS.get(cached.getKey());
            if (cosmetic == null || !cosmetic.hasDisplayName()) continue;
            String name = cached.getValue();
            if (name == null || name.isBlank()) continue;
            merged.put(name.toLowerCase(), cosmetic);
        }

        RUNTIME_NAME_COSMETICS.clear();
        RUNTIME_NAME_COSMETICS.putAll(merged);
        SORTED_RUNTIME_ENTRIES = getNameEntriesSorted();
        LinkedHashMap<String, CosmeticEntry> tokenEntries = new LinkedHashMap<>();
        List<Map.Entry<String, CosmeticEntry>> specialEntries = new ArrayList<>();
        for (Map.Entry<String, CosmeticEntry> entry : SORTED_RUNTIME_ENTRIES) {
            String key = entry.getKey();
            CosmeticEntry value = entry.getValue();
            if (key == null || key.isEmpty() || value == null || !value.hasDisplayName()) continue;
            boolean tokenOnly = true;
            for (int idx = 0; idx < key.length(); idx++) {
                if (!isTokenChar(key.charAt(idx))) {
                    tokenOnly = false;
                    break;
                }
            }
            if (tokenOnly) tokenEntries.put(key.toLowerCase(Locale.ROOT), value);
            else specialEntries.add(entry);
        }
        TOKEN_NAME_COSMETICS = tokenEntries.isEmpty() ? Map.of() : tokenEntries;
        SPECIAL_NAME_ENTRIES = specialEntries.isEmpty() ? List.of() : specialEntries;
        VISUAL_STRING_CACHE.clear();
        lastRuntimeRebuildMs = now;
        }
    }


    public static String normalizeLegacyCodes(String text) {
        if (text == null || text.isEmpty()) return text;
        Matcher hexMatcher = HEX_AMP_PATTERN.matcher(text);
        StringBuffer hexOut = new StringBuffer();
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1).toUpperCase();
            hexMatcher.appendReplacement(hexOut, Matcher.quoteReplacement(toLegacyHexCode(hex)));
        }
        hexMatcher.appendTail(hexOut);

        Matcher matcher = LEGACY_AMP_PATTERN.matcher(hexOut.toString());
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String code = matcher.group(1).toLowerCase();
            matcher.appendReplacement(out, Matcher.quoteReplacement("§" + code));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String sanitizeDisplayName(String text) {
        String normalized = normalizeLegacyCodes(text);
        if (normalized == null || normalized.isEmpty()) return normalized;

        if (normalized.indexOf('§') >= 0 && !normalized.endsWith("§r")) {
            return normalized + "§r";
        }
        return normalized;
    }

    private static String boundarySafeDisplayName(String displayName) {
        if (displayName == null || displayName.isEmpty()) return displayName;
        String normalized = sanitizeDisplayName(displayName);
        if (!LEGACY_SECTION_PATTERN.matcher(normalized).find()) {
            return normalized;
        }
        if (!normalized.startsWith("§r")) {
            normalized = "§r" + normalized;
        }
        if (!normalized.endsWith("§r")) {
            normalized = normalized + "§r";
        }
        return normalized;
    }

    public static Text applyVisualWords(Text text) {
        if (text == null) return null;
        String original = text.getString();
        if (original == null || original.isEmpty()) return text;
        String replacedCombined = applyVisualWordsInternal(original, true);
        boolean hasLegacyMarkers = original.indexOf('§') >= 0 || original.indexOf('&') >= 0;
        if (replacedCombined.equals(original) && !hasLegacyMarkers) return text;

        MutableText rebuilt = Text.empty();
        text.visit((style, string) -> {
            appendLegacyFormatted(rebuilt, applyVisualWordsInternal(string, true), style);
            return Optional.empty();
        }, Style.EMPTY);
        return rebuilt;
    }

    public static OrderedText applyVisualWords(OrderedText text) {
        if (text == null) return null;
        StringBuilder plainBuilder = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            plainBuilder.appendCodePoint(codePoint);
            return true;
        });
        String original = plainBuilder.toString();
        if (original.isEmpty()) return text;
        String replacedCombined = applyVisualWordsInternal(original, true);
        boolean hasLegacyMarkers = original.indexOf('§') >= 0 || original.indexOf('&') >= 0;
        if (replacedCombined.equals(original) && !hasLegacyMarkers) return text;

        MutableText rebuilt = Text.empty();
        final Style[] currentStyle = new Style[] {null};
        final StringBuilder buffer = new StringBuilder();

        text.accept((index, style, codePoint) -> {
            Style effective = style == null ? Style.EMPTY : style;
            if (currentStyle[0] == null) {
                currentStyle[0] = effective;
            } else if (!currentStyle[0].equals(effective)) {
                if (!buffer.isEmpty()) {
                    rebuilt.append(Text.literal(buffer.toString()).setStyle(currentStyle[0]));
                    buffer.setLength(0);
                }
                currentStyle[0] = effective;
            }
            buffer.append((char) codePoint);
            return true;
        });

        if (!buffer.isEmpty()) {
            rebuilt.append(Text.literal(buffer.toString()).setStyle(currentStyle[0] == null ? Style.EMPTY : currentStyle[0]));
        }

        return applyVisualWords(rebuilt).asOrderedText();
    }

    public static Text applyVisualWordsForTooltip(Text text) {
        if (text == null) return null;
        String original = text.getString();
        if (original == null || original.isEmpty()) return text;

        String replaced = applyVisualWordsInternal(original, true);
        if (replaced.equals(original)) return text;

        final Style[] firstSeenStyle = new Style[] {null};
        final boolean[] multipleStyles = new boolean[] {false};
        text.visit((style, string) -> {
            if (string == null || string.isEmpty()) return Optional.empty();
            if (firstSeenStyle[0] == null) {
                firstSeenStyle[0] = style == null ? Style.EMPTY : style;
            } else if (!firstSeenStyle[0].equals(style)) {
                multipleStyles[0] = true;
                return Optional.of(Boolean.TRUE);
            }
            return Optional.empty();
        }, Style.EMPTY);
        if (multipleStyles[0]) {
            return applyVisualWords(text);
        }

        final Style[] firstStyle = new Style[] {Style.EMPTY};
        text.visit((style, string) -> {
            if (string != null && !string.isEmpty()) {
                firstStyle[0] = style == null ? Style.EMPTY : style;
                return Optional.of(Boolean.TRUE);
            }
            return Optional.empty();
        }, Style.EMPTY);

        MutableText rebuilt = Text.empty();
        appendLegacyFormatted(rebuilt, replaced, firstStyle[0]);
        return rebuilt;
    }

    private static void appendLegacyFormatted(MutableText out, String input, Style baseStyle) {
        if (input == null || input.isEmpty()) return;
        String normalized = normalizeLegacyCodes(input);
        StringBuilder plain = new StringBuilder();
        Style currentStyle = baseStyle;

        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c == '§') {
                // Drop dangling section markers to avoid downstream parser crashes
                // in other chat mods expecting valid code pairs.
                if (i + 1 >= normalized.length()) {
                    break;
                }

                int rgb = parseLegacyHexColor(normalized, i);
                if (rgb >= 0) {
                    if (!plain.isEmpty()) {
                        appendWithIconTokens(out, plain.toString(), currentStyle);
                        plain.setLength(0);
                    }
                    currentStyle = baseStyle.withColor(rgb);
                    i += 13;
                    continue;
                }

                if (!plain.isEmpty()) {
                    appendWithIconTokens(out, plain.toString(), currentStyle);
                    plain.setLength(0);
                }

                Formatting fmt = Formatting.byCode(normalized.charAt(i + 1));
                if (fmt != null) {
                    if (fmt == Formatting.RESET) {
                        currentStyle = baseStyle;
                    } else if (fmt.isColor()) {
                        currentStyle = baseStyle.withFormatting(fmt);
                    } else {
                        currentStyle = currentStyle.withFormatting(fmt);
                    }
                }
                i++;
                continue;
            }
            plain.append(c);
        }

        if (!plain.isEmpty()) {
            appendWithIconTokens(out, plain.toString(), currentStyle);
        }
    }

    private static void appendWithIconTokens(MutableText out, String plain, Style style) {
        if (plain == null || plain.isEmpty()) return;

        int idx = 0;
        while (idx < plain.length()) {
            int capyIdx = plain.indexOf(CAPY_MARKER, idx);
            int franceIdx = plain.indexOf(FRANCE_MARKER, idx);
            int lipIdx = plain.indexOf(LIP_MARKER, idx);

            int marker = capyIdx;
            String markerToken = CAPY_MARKER;
            String glyph = CAPY_GLYPH;
            Identifier font = CAPY_FONT;

            if (marker < 0 || (franceIdx >= 0 && franceIdx < marker)) {
                marker = franceIdx;
                markerToken = FRANCE_MARKER;
                glyph = FRANCE_GLYPH;
                font = FRANCE_FONT;
            }

            if (marker < 0 || (lipIdx >= 0 && lipIdx < marker)) {
                marker = lipIdx;
                markerToken = LIP_MARKER;
                glyph = LIP_GLYPH;
                font = LIP_FONT;
            }

            if (marker < 0) {
                out.append(Text.literal(plain.substring(idx)).setStyle(style));
                break;
            }

            if (marker > idx) {
                out.append(Text.literal(plain.substring(idx, marker)).setStyle(style));
            }

            Style iconStyle = style.withColor(Formatting.WHITE).withFont(new StyleSpriteSource.Font(font));
            out.append(Text.literal(glyph).setStyle(iconStyle));
            idx = marker + markerToken.length();
        }
    }

    public static void init() {
        UUID_COSMETICS.clear();
        STATIC_NAME_COSMETICS.clear();
        RUNTIME_NAME_COSMETICS.clear();
        UUID_NAME_CACHE.clear();
        SORTED_RUNTIME_ENTRIES = List.of();
        TOKEN_NAME_COSMETICS = Map.of();
        SPECIAL_NAME_ENTRIES = List.of();
        VISUAL_STRING_CACHE.clear();
        lastRuntimeRebuildMs = 0L;

        loadUuidCosmetics();
        rebuildRuntimeNameMap();
    }

    public static CosmeticScale getScale(UUID uuid) {
        if (uuid == null) return null;
        CosmeticEntry entry = UUID_COSMETICS.get(uuid);
        return entry != null ? entry.scale : null;
    }

    private static boolean loadUuidCosmetics() {
        CosmeticsRepository.init();

        for (Map.Entry<UUID, PlayerCosmetic> entry : CosmeticsRepository.getPlayers().entrySet()) {
            UUID uuid = entry.getKey();
            PlayerCosmetic player = entry.getValue();

            CosmeticEntry cosmetic = parseUuidCosmeticEntry(player);
            if (cosmetic == null) continue;

            UUID_COSMETICS.put(uuid, cosmetic);
            addStaticAliases(player, cosmetic);

            String username = player.username();
            if (username != null && !username.isBlank()) {
                UUID_NAME_CACHE.put(uuid, username);
            } else {
                requestNameLookupIfNeeded(uuid, cosmetic);
            }
        }

        return !UUID_COSMETICS.isEmpty();
    }

    private static void requestNameLookupIfNeeded(UUID uuid, CosmeticEntry cosmetic) {
        if (uuid == null || cosmetic == null || !cosmetic.hasDisplayName()) return;
        if (UUID_NAME_CACHE.containsKey(uuid)) return;
        if (!UUID_LOOKUP_IN_FLIGHT.add(uuid)) return;

        Thread.startVirtualThread(() -> {
            try {
                String cleanUuid = uuid.toString().replace("-", "");
                HttpRequest request = HttpRequest.newBuilder(URI.create(MOJANG_PROFILE_BY_UUID + cleanUuid))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) return;

                JsonElement root = JsonParser.parseString(response.body());
                if (!root.isJsonObject()) return;
                JsonObject obj = root.getAsJsonObject();
                if (!obj.has("name") || !obj.get("name").isJsonPrimitive()) return;

                String name = obj.get("name").getAsString();
                if (name == null || name.isBlank()) return;

                synchronized (UUID_NAME_CACHE) {
                    UUID_NAME_CACHE.put(uuid, name);
                }
            } catch (Exception ignored) {
            } finally {
                UUID_LOOKUP_IN_FLIGHT.remove(uuid);
            }
        });
    }

    private static void addStaticAliases(PlayerCosmetic player, CosmeticEntry cosmetic) {
        if (player == null || cosmetic == null || !cosmetic.hasDisplayName()) return;

        addStaticAlias(player.username(), cosmetic);

        for (String alias : player.aliases()) {
            if (alias == null || alias.isBlank()) continue;
            STATIC_NAME_COSMETICS.put(alias.toLowerCase(), cosmetic);
        }
    }

    private static void addStaticAlias(String alias, CosmeticEntry cosmetic) {
        if (alias == null || alias.isBlank()) return;
        STATIC_NAME_COSMETICS.put(alias.toLowerCase(), cosmetic);
    }

    private static CosmeticEntry parseUuidCosmeticEntry(PlayerCosmetic player) {
        if (player == null) return null;

        String display = player.displayName();
        if (display != null && !display.isBlank()) {
            display = sanitizeDisplayName(decodeNoammJsonName(display));
        }

        PlayerScale scaleValue = player.scale();
        CosmeticScale scale = scaleValue == null ? null : new CosmeticScale(scaleValue.x(), scaleValue.y(), scaleValue.z());
        if ((display == null || display.isBlank()) && scale == null) return null;

        return new CosmeticEntry(display == null ? "" : display, new int[] {255, 255, 255, 127}, scale);
    }

    private static String decodeNoammJsonName(String input) {
        if (input == null || input.isBlank()) return input;
        String trimmed = input.trim();
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) return input;

        try {
            JsonElement root = JsonParser.parseString(trimmed);
            StringBuilder out = new StringBuilder();
            appendJsonTextComponent(root, TextStyle.EMPTY, out);
            return out.isEmpty() ? input : out.toString();
        } catch (Exception ignored) {
            return input;
        }
    }

    private static void appendJsonTextComponent(JsonElement element, TextStyle inherited, StringBuilder out) {
        if (element == null || element.isJsonNull()) return;

        if (element.isJsonPrimitive()) {
            String text = element.getAsString();
            if (!text.isEmpty()) out.append(styledChunk(text, inherited));
            return;
        }

        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                appendJsonTextComponent(child, inherited, out);
            }
            return;
        }

        if (!element.isJsonObject()) return;
        JsonObject obj = element.getAsJsonObject();
        TextStyle merged = TextStyle.from(obj, inherited);

        if (obj.has("text") && obj.get("text").isJsonPrimitive()) {
            String text = obj.get("text").getAsString();
            if (!text.isEmpty()) out.append(styledChunk(text, merged));
        }

        if (obj.has("extra") && obj.get("extra").isJsonArray()) {
            for (JsonElement child : obj.getAsJsonArray("extra")) {
                appendJsonTextComponent(child, merged, out);
            }
        }
    }

    private static String styledChunk(String text, TextStyle style) {
        StringBuilder sb = new StringBuilder();
        sb.append("§r");
        appendStyleCodes(sb, style);
        sb.append(text);
        return sb.toString();
    }

    private static void appendStyleCodes(StringBuilder sb, TextStyle style) {
        if (style == null) return;

        if (style.color != null && !style.color.isBlank()) {
            String color = style.color.trim();
            if (color.startsWith("#") && color.length() == 7) {
                sb.append('&').append(color);
            } else {
                String legacy = NAMED_COLOR_TO_LEGACY.get(color.toLowerCase());
                if (legacy != null) sb.append('&').append(legacy);
            }
        }

        if (style.bold != null && style.bold) sb.append("&l");
        if (style.italic != null && style.italic) sb.append("&o");
        if (style.underlined != null && style.underlined) sb.append("&n");
        if (style.strikethrough != null && style.strikethrough) sb.append("&m");
        if (style.obfuscated != null && style.obfuscated) sb.append("&k");
    }

    private static Map<String, String> buildNamedColorMap() {
        Map<String, String> map = new HashMap<>();
        map.put("black", "0");
        map.put("dark_blue", "1");
        map.put("dark_green", "2");
        map.put("dark_aqua", "3");
        map.put("dark_red", "4");
        map.put("dark_purple", "5");
        map.put("gold", "6");
        map.put("gray", "7");
        map.put("dark_gray", "8");
        map.put("blue", "9");
        map.put("green", "a");
        map.put("aqua", "b");
        map.put("red", "c");
        map.put("light_purple", "d");
        map.put("yellow", "e");
        map.put("white", "f");
        return map;
    }

    private static String toLegacyHexCode(String upperHex) {
        StringBuilder out = new StringBuilder(14);
        out.append('§').append('x');
        for (int i = 0; i < upperHex.length(); i++) {
            out.append('§').append(upperHex.charAt(i));
        }
        return out.toString();
    }

    private static int parseLegacyHexColor(String input, int start) {
        if (start < 0 || start + 13 >= input.length()) return -1;
        if (input.charAt(start) != '§') return -1;
        if (Character.toLowerCase(input.charAt(start + 1)) != 'x') return -1;

        int[] nibble = new int[6];
        for (int i = 0; i < 6; i++) {
            int sectionIdx = start + 2 + i * 2;
            int digitIdx = sectionIdx + 1;
            if (input.charAt(sectionIdx) != '§') return -1;
            int value = Character.digit(input.charAt(digitIdx), 16);
            if (value < 0) return -1;
            nibble[i] = value;
        }

        return (nibble[0] << 20) | (nibble[1] << 16) | (nibble[2] << 12) | (nibble[3] << 8) | (nibble[4] << 4) | nibble[5];
    }

    private static final class CosmeticEntry {
        private final String displayName;
        private final int[] highlightColor;
        private final CosmeticScale scale;

        private CosmeticEntry(String displayName, int[] highlightColor, CosmeticScale scale) {
            this.displayName = displayName;
            this.highlightColor = highlightColor;
            this.scale = scale;
        }

        private boolean hasDisplayName() {
            return displayName != null && !displayName.isBlank();
        }
    }

    private static final class TextStyle {
        private static final TextStyle EMPTY = new TextStyle(null, null, null, null, null, null);

        private final String color;
        private final Boolean bold;
        private final Boolean italic;
        private final Boolean underlined;
        private final Boolean strikethrough;
        private final Boolean obfuscated;

        private TextStyle(String color, Boolean bold, Boolean italic, Boolean underlined, Boolean strikethrough, Boolean obfuscated) {
            this.color = color;
            this.bold = bold;
            this.italic = italic;
            this.underlined = underlined;
            this.strikethrough = strikethrough;
            this.obfuscated = obfuscated;
        }

        private static TextStyle from(JsonObject obj, TextStyle inherited) {
            if (obj == null) return inherited;
            TextStyle base = inherited == null ? EMPTY : inherited;

            String color = obj.has("color") && obj.get("color").isJsonPrimitive() ? obj.get("color").getAsString() : base.color;
            Boolean bold = readBoolean(obj, "bold", base.bold);
            Boolean italic = readBoolean(obj, "italic", base.italic);
            Boolean underlined = readBoolean(obj, "underlined", base.underlined);
            Boolean strikethrough = readBoolean(obj, "strikethrough", base.strikethrough);
            Boolean obfuscated = readBoolean(obj, "obfuscated", base.obfuscated);

            return new TextStyle(color, bold, italic, underlined, strikethrough, obfuscated);
        }

        private static Boolean readBoolean(JsonObject obj, String key, Boolean fallback) {
            if (!obj.has(key)) return fallback;
            try {
                return obj.get(key).getAsBoolean();
            } catch (Exception ignored) {
                return fallback;
            }
        }
    }

    public record CosmeticScale(float x, float y, float z) {}
}
