package xenus.xenaddons.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ARGB;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import xenus.xenaddons.cosmetics.CosmeticsRepository;
import xenus.xenaddons.cosmetics.CosmeticsSnapshot.PlayerAnimatedName;
import xenus.xenaddons.cosmetics.CosmeticsSnapshot.PlayerCosmetic;
import xenus.xenaddons.cosmetics.CosmeticsSnapshot.PlayerScale;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.features.impl.misc.CustomSizeModule;

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
    private static final Identifier CAPY_FONT = Identifier.fromNamespaceAndPath("xenaddons", "capybara");
    private static final Identifier FRANCE_FONT = Identifier.fromNamespaceAndPath("xenaddons", "france");
    private static final Identifier LIP_FONT = Identifier.fromNamespaceAndPath("xenaddons", "lip");

    private static final String RICH_MARKER_PREFIX = "{xa:";
    private static final String RICH_MARKER_SUFFIX = "}";
    private static final Map<String, String> RENDERED_FORM_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Component> RICH_DISPLAY_NAMES = new ConcurrentHashMap<>();
    private static final Map<String, AnimatedRichName> ANIMATED_DISPLAY_NAMES = new ConcurrentHashMap<>();
    private static final Map<String, String> RICH_DISPLAY_PLAIN = new ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicInteger RICH_MARKER_SEQ = new java.util.concurrent.atomic.AtomicInteger();

    private static final boolean LEGACY_DISPLAY_NAMES_ENABLED = true;

    private static final String[] CONCAT_STATUS_SUFFIXES = new String[] {"offline", "online"};
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

    private static final long VISUAL_REBUILD_THROTTLE_MS = 33L;
    private static final int VISUAL_REBUILD_CACHE_MAX = 2048;
    private static final Map<String, TimedValue<Component>> COMPONENT_REBUILD_CACHE = Collections.synchronizedMap(new LinkedHashMap<>(VISUAL_REBUILD_CACHE_MAX, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, TimedValue<Component>> eldest) {
            return size() > VISUAL_REBUILD_CACHE_MAX;
        }
    });
    private static final Map<String, TimedValue<FormattedCharSequence>> SEQUENCE_REBUILD_CACHE = Collections.synchronizedMap(new LinkedHashMap<>(VISUAL_REBUILD_CACHE_MAX, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, TimedValue<FormattedCharSequence>> eldest) {
            return size() > VISUAL_REBUILD_CACHE_MAX;
        }
    });

    private record TimedValue<T>(T value, long computedAt) {}

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
        if (!shouldApplyCustomNames()) return null;
        if (name == null || name.isBlank()) return null;
        rebuildRuntimeNameMap();
        return RUNTIME_NAME_COSMETICS.get(name.toLowerCase(Locale.ROOT));
    }

    public static String applyVisualWords(String text) {
        return applyVisualWordsInternal(text, false);
    }

    public static String applyVisualWordsPlain(String text) {
        String replaced = applyVisualWordsInternal(text, false);
        return stripLegacyCodes(replaced);
    }

    public static Map<String, String> getRuntimeReplaceMap() {
        if (!shouldApplyCustomNames()) return Map.of();
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
        if (!shouldApplyCustomNames()) {
            return keepMarkers ? text : removeCosmeticMarkers(text);
        }

        String cacheKey = (keepMarkers ? "1|" : "0|") + text;
        String cached = VISUAL_STRING_CACHE.get(cacheKey);
        if (cached != null) return cached;

        rebuildRuntimeNameMap();
        if (SORTED_RUNTIME_ENTRIES.isEmpty()) {
            String plain = keepMarkers ? text : removeCosmeticMarkers(text);
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
            result = resolveRichMarkersToPlain(result);
            result = result.replace(CAPY_MARKER, "");
            result = result.replace(FRANCE_MARKER, "");
            result = result.replace(LIP_MARKER, "");
        }

        result = trimDanglingSectionSigns(result);

        VISUAL_STRING_CACHE.put(cacheKey, result);
        return result;
    }

    private static boolean shouldApplyCustomNames() {
        CustomSizeModule module = ModuleManager.getModule(CustomSizeModule.class);
        return module == null || module.shouldApplyCustomNames();
    }

    private static String removeCosmeticMarkers(String text) {
        if (text == null || text.isEmpty()) return text;
        return resolveRichMarkersToPlain(text)
            .replace(CAPY_MARKER, "")
            .replace(FRANCE_MARKER, "")
            .replace(LIP_MARKER, "");
    }

    private static String resolveRichMarkersToPlain(String text) {
        if (text == null || text.isEmpty() || RICH_DISPLAY_PLAIN.isEmpty()) return text;
        if (text.indexOf(RICH_MARKER_PREFIX) < 0) return text;

        String result = text;
        for (Map.Entry<String, String> entry : RICH_DISPLAY_PLAIN.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
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
        if (matchesReplacementAt(input, targetStart, target, replacement)) return true;

        String rendered = renderedFormOf(replacement);
        return !rendered.equals(replacement) && matchesReplacementAt(input, targetStart, target, rendered);
    }

    private static boolean matchesReplacementAt(String input, int targetStart, String target, String replacement) {
        if (replacement.isEmpty()) return false;
        String loweredReplacement = replacement.toLowerCase(Locale.ROOT);
        String loweredTarget = target.toLowerCase(Locale.ROOT);

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

    private static String renderedFormOf(String replacement) {
        String cached = RENDERED_FORM_CACHE.get(replacement);
        if (cached != null) return cached;

        String rendered = stripLegacyCodes(normalizeLegacyCodes(resolveRichMarkersToPlain(replacement)))
            .replace(CAPY_MARKER, CAPY_GLYPH)
            .replace(FRANCE_MARKER, FRANCE_GLYPH)
            .replace(LIP_MARKER, LIP_GLYPH);

        RENDERED_FORM_CACHE.put(replacement, rendered);
        return rendered;
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
                    String rendered = renderedFormOf(replacement);
                    if (rendered.length() > token.length()
                        && input.regionMatches(true, start, rendered, 0, rendered.length())) {
                        out.append(input, start, start + rendered.length());
                        i = start + rendered.length();
                        continue;
                    }
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

        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            if (mc.getConnection() != null) {
                for (PlayerInfo entry : mc.getConnection().getListedOnlinePlayers()) {
                    if (entry == null || entry.getProfile() == null) continue;
                    UUID uuid = entry.getProfile().id();
                    if (uuid == null) continue;

                    CosmeticEntry cosmetic = UUID_COSMETICS.get(uuid);
                    if (cosmetic == null || !cosmetic.hasDisplayName()) continue;

                    String profileName = entry.getProfile().name();
                    if (profileName == null || profileName.isBlank()) continue;

                    UUID_NAME_CACHE.put(uuid, profileName);
                    merged.put(profileName.toLowerCase(Locale.ROOT), cosmetic);
                }
            }

            if (mc.level != null) {
                for (Player player : mc.level.players()) {
                    if (player == null) continue;
                    UUID uuid = player.getUUID();
                    if (uuid == null) continue;

                    CosmeticEntry cosmetic = UUID_COSMETICS.get(uuid);
                    if (cosmetic == null || !cosmetic.hasDisplayName()) continue;

                    String name = player.getGameProfile().name();
                    if (name == null || name.isBlank()) continue;

                    UUID_NAME_CACHE.put(uuid, name);
                    merged.put(name.toLowerCase(Locale.ROOT), cosmetic);
                }
            }
        }

        for (Map.Entry<UUID, String> cached : UUID_NAME_CACHE.entrySet()) {
            CosmeticEntry cosmetic = UUID_COSMETICS.get(cached.getKey());
            if (cosmetic == null || !cosmetic.hasDisplayName()) continue;
            String name = cached.getValue();
            if (name == null || name.isBlank()) continue;
            merged.put(name.toLowerCase(Locale.ROOT), cosmetic);
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
        COMPONENT_REBUILD_CACHE.clear();
        SEQUENCE_REBUILD_CACHE.clear();
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
            String code = matcher.group(1).toLowerCase(Locale.ROOT);
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

    public static Component applyVisualWords(Component text) {
        if (text == null) return null;
        String original = text.getString();
        if (original == null || original.isEmpty()) return text;
        String replacedCombined = applyVisualWordsInternal(original, true);
        boolean hasLegacyMarkers = original.indexOf('§') >= 0 || original.indexOf('&') >= 0;
        if (replacedCombined.equals(original) && !hasLegacyMarkers) return text;

        long now = System.currentTimeMillis();
        TimedValue<Component> cached = COMPONENT_REBUILD_CACHE.get(original);
        if (cached != null && now - cached.computedAt() < VISUAL_REBUILD_THROTTLE_MS) {
            return cached.value();
        }

        MutableComponent rebuilt = Component.empty();
        text.visit((style, string) -> {
            appendLegacyFormatted(rebuilt, applyVisualWordsInternal(string, true), style);
            return Optional.empty();
        }, Style.EMPTY);
        COMPONENT_REBUILD_CACHE.put(original, new TimedValue<>(rebuilt, now));
        return rebuilt;
    }

    public static FormattedCharSequence applyVisualWords(FormattedCharSequence text) {
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

        long now = System.currentTimeMillis();
        TimedValue<FormattedCharSequence> cached = SEQUENCE_REBUILD_CACHE.get(original);
        if (cached != null && now - cached.computedAt() < VISUAL_REBUILD_THROTTLE_MS) {
            return cached.value();
        }

        MutableComponent rebuilt = Component.empty();
        final Style[] currentStyle = new Style[] {null};
        final StringBuilder buffer = new StringBuilder();

        text.accept((index, style, codePoint) -> {
            Style effective = style == null ? Style.EMPTY : style;
            if (currentStyle[0] == null) {
                currentStyle[0] = effective;
            } else if (!currentStyle[0].equals(effective)) {
                if (!buffer.isEmpty()) {
                    rebuilt.append(Component.literal(buffer.toString()).setStyle(currentStyle[0]));
                    buffer.setLength(0);
                }
                currentStyle[0] = effective;
            }
            buffer.append((char) codePoint);
            return true;
        });

        if (!buffer.isEmpty()) {
            rebuilt.append(Component.literal(buffer.toString()).setStyle(currentStyle[0] == null ? Style.EMPTY : currentStyle[0]));
        }

        FormattedCharSequence result = applyVisualWords(rebuilt).getVisualOrderText();
        SEQUENCE_REBUILD_CACHE.put(original, new TimedValue<>(result, now));
        return result;
    }

    public static Component applyVisualWordsForTooltip(Component text) {
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

        MutableComponent rebuilt = Component.empty();
        appendLegacyFormatted(rebuilt, replaced, firstStyle[0]);
        return rebuilt;
    }

    private static void appendLegacyFormatted(MutableComponent out, String input, Style baseStyle) {
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

                ChatFormatting fmt = ChatFormatting.getByCode(normalized.charAt(i + 1));
                if (fmt != null) {
                    if (fmt == ChatFormatting.RESET) {
                        currentStyle = baseStyle;
                    } else if (TextColor.fromLegacyFormat(fmt) != null) {
                        currentStyle = baseStyle.applyFormat(fmt);
                    } else {
                        currentStyle = currentStyle.applyFormat(fmt);
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

    private static void appendWithIconTokens(MutableComponent out, String plain, Style style) {
        if (plain == null || plain.isEmpty()) return;

        int idx = 0;
        while (idx < plain.length()) {
            int start = plain.indexOf(RICH_MARKER_PREFIX, idx);
            int end = start < 0 ? -1 : plain.indexOf(RICH_MARKER_SUFFIX, start);
            Component rich = end < 0 ? null : resolveRichComponent(plain.substring(start, end + 1));

            if (rich == null) {
                appendIconTokensOnly(out, plain.substring(idx), style);
                return;
            }

            if (start > idx) appendIconTokensOnly(out, plain.substring(idx, start), style);
            appendRichComponent(out, rich);
            idx = end + 1;
        }
    }

    // Icon markers may sit inside a rich display name, so flatten it and run each run
    // through the icon pass instead of appending the Component as-is.
    private static void appendRichComponent(MutableComponent out, Component rich) {
        if (rich == null) return;
        if (!rich.getString().contains(CAPY_MARKER)
            && !rich.getString().contains(FRANCE_MARKER)
            && !rich.getString().contains(LIP_MARKER)) {
            out.append(rich.copy());
            return;
        }

        rich.visit((style, string) -> {
            appendIconTokensOnly(out, string, style);
            return Optional.empty();
        }, Style.EMPTY);
    }

    private static void appendIconTokensOnly(MutableComponent out, String plain, Style style) {
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
                out.append(Component.literal(plain.substring(idx)).setStyle(style));
                break;
            }

            if (marker > idx) {
                out.append(Component.literal(plain.substring(idx, marker)).setStyle(style));
            }

            Style iconStyle = style.withColor(ChatFormatting.WHITE).withFont(new FontDescription.Resource(font));
            out.append(Component.literal(glyph).setStyle(iconStyle));
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
        COMPONENT_REBUILD_CACHE.clear();
        SEQUENCE_REBUILD_CACHE.clear();
        RICH_DISPLAY_NAMES.clear();
        ANIMATED_DISPLAY_NAMES.clear();
        RICH_DISPLAY_PLAIN.clear();
        RENDERED_FORM_CACHE.clear();
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
            STATIC_NAME_COSMETICS.put(alias.toLowerCase(Locale.ROOT), cosmetic);
        }
    }

    private static void addStaticAlias(String alias, CosmeticEntry cosmetic) {
        if (alias == null || alias.isBlank()) return;
        STATIC_NAME_COSMETICS.put(alias.toLowerCase(Locale.ROOT), cosmetic);
    }

    private static CosmeticEntry parseUuidCosmeticEntry(PlayerCosmetic player) {
        if (player == null) return null;

        String display;
        PlayerAnimatedName animatedName = player.animatedName();
        if (animatedName != null && !animatedName.frames().isEmpty()) {
            display = registerAnimatedDisplayName(animatedName);
        } else {
            display = player.displayName();
            if (display != null && !display.isBlank()) {
                Component rich = parseJsonDisplayName(display);
                if (rich != null) {
                    display = registerRichDisplayName(rich);
                } else if (LEGACY_DISPLAY_NAMES_ENABLED) {
                    display = sanitizeDisplayName(display);
                } else if (hasLegacyFormatting(display)) {
                    // Legacy formatting is off: drop the entry rather than leaking raw codes.
                    display = null;
                }
            }
        }

        PlayerScale scaleValue = player.scale();
        CosmeticScale scale = scaleValue == null ? null : new CosmeticScale(scaleValue.x(), scaleValue.y(), scaleValue.z());
        if ((display == null || display.isBlank()) && scale == null) return null;

        return new CosmeticEntry(display == null ? "" : display, new int[] {255, 255, 255, 127}, scale);
    }

    private static boolean hasLegacyFormatting(String text) {
        if (text == null || text.isEmpty()) return false;
        return HEX_AMP_PATTERN.matcher(text).find()
            || LEGACY_AMP_PATTERN.matcher(text).find()
            || LEGACY_SECTION_PATTERN.matcher(text).find();
    }

    private static String registerRichDisplayName(Component component) {
        String marker = RICH_MARKER_PREFIX + RICH_MARKER_SEQ.incrementAndGet() + RICH_MARKER_SUFFIX;
        RICH_DISPLAY_NAMES.put(marker, component);
        RICH_DISPLAY_PLAIN.put(marker, component.getString());
        return marker;
    }

    private static String registerAnimatedDisplayName(PlayerAnimatedName animated) {
        List<List<ColoredPart>> frames = new ArrayList<>();
        for (String frameText : animated.frames()) {
            List<ColoredPart> parts = parseColoredParts(frameText);
            if (!parts.isEmpty()) frames.add(parts);
        }
        if (frames.isEmpty()) return null;

        String marker = RICH_MARKER_PREFIX + RICH_MARKER_SEQ.incrementAndGet() + RICH_MARKER_SUFFIX;
        ANIMATED_DISPLAY_NAMES.put(marker, new AnimatedRichName(frames));
        RICH_DISPLAY_PLAIN.put(marker, buildComponent(frames.get(0)).getString());
        return marker;
    }

    private static List<ColoredPart> parseColoredParts(String input) {
        List<ColoredPart> parts = new ArrayList<>();
        if (input == null || input.isEmpty()) return parts;

        String normalized = normalizeLegacyCodes(input);
        StringBuilder plain = new StringBuilder();
        Style currentStyle = Style.EMPTY;
        int currentColor = 0xFFFFFF;

        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c == '§') {
                if (i + 1 >= normalized.length()) break;

                int rgb = parseLegacyHexColor(normalized, i);
                if (rgb >= 0) {
                    flushColoredPart(parts, plain, currentStyle, currentColor);
                    currentColor = rgb;
                    i += 13;
                    continue;
                }

                flushColoredPart(parts, plain, currentStyle, currentColor);

                ChatFormatting fmt = ChatFormatting.getByCode(normalized.charAt(i + 1));
                if (fmt != null) {
                    if (fmt == ChatFormatting.RESET) {
                        currentStyle = Style.EMPTY;
                        currentColor = 0xFFFFFF;
                    } else if (TextColor.fromLegacyFormat(fmt) == null) {
                        currentStyle = currentStyle.applyFormat(fmt);
                    }
                }
                i++;
                continue;
            }
            plain.append(c);
        }
        flushColoredPart(parts, plain, currentStyle, currentColor);
        return parts;
    }

    private static void flushColoredPart(List<ColoredPart> parts, StringBuilder plain, Style style, int color) {
        if (plain.isEmpty()) return;
        parts.add(new ColoredPart(plain.toString(), style, color));
        plain.setLength(0);
    }

    private static Component buildComponent(List<ColoredPart> parts) {
        MutableComponent out = Component.empty();
        for (ColoredPart part : parts) {
            out.append(Component.literal(part.text()).setStyle(part.style().withColor(part.color())));
        }
        return out;
    }

    private static Component buildInterpolated(List<ColoredPart> from, List<ColoredPart> to, float t) {
        if (from.size() != to.size()) return buildComponent(from);

        MutableComponent out = Component.empty();
        for (int i = 0; i < from.size(); i++) {
            ColoredPart a = from.get(i);
            ColoredPart b = to.get(i);
            int color = lerpColor(a.color(), b.color(), t);
            out.append(Component.literal(a.text()).setStyle(a.style().withColor(color)));
        }
        return out;
    }

    private static int lerpColor(int from, int to, float t) {
        int r = lerpChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int g = lerpChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = lerpChannel(from & 0xFF, to & 0xFF, t);
        return (r << 16) | (g << 8) | b;
    }

    private static int lerpChannel(int from, int to, float t) {
        return Math.max(0, Math.min(255, Math.round(from + (to - from) * t)));
    }

    private record ColoredPart(String text, Style style, int color) {}

    private static Component resolveRichComponent(String marker) {
        AnimatedRichName animated = ANIMATED_DISPLAY_NAMES.get(marker);
        if (animated != null) return animated.current();
        return RICH_DISPLAY_NAMES.get(marker);
    }

    // The whole cycle (through every frame and back to the first) always takes this long,
    // regardless of how many frames there are or what the data's own interval says.
    private static final long ANIMATED_NAME_FULL_CYCLE_MS = 1500L;

    // A name can be queried many times per frame (nametag, tab list, chat...). Recomputing and
    // allocating a fresh Component on every single call would scale with render calls, not with
    // actual visual change, so this throttles rebuilds to roughly the display's own frame rate.
    private static final long ANIMATED_NAME_RECOMPUTE_INTERVAL_MS = 16L;

    private static final class AnimatedRichName {
        private final List<List<ColoredPart>> frames;
        private Component cached;
        private long cachedAt = -1L;

        private AnimatedRichName(List<List<ColoredPart>> frames) {
            this.frames = frames;
        }

        Component current() {
            if (frames.isEmpty()) return Component.empty();
            if (frames.size() == 1) return buildComponent(frames.get(0));

            long now = System.currentTimeMillis();
            if (cached != null && now - cachedAt < ANIMATED_NAME_RECOMPUTE_INTERVAL_MS) {
                return cached;
            }

            long step = Math.max(1L, ANIMATED_NAME_FULL_CYCLE_MS / frames.size());
            long cycleMs = step * frames.size();
            long pos = now % cycleMs;
            int index = (int) (pos / step);
            float t = (pos % step) / (float) step;

            List<ColoredPart> from = frames.get(index);
            List<ColoredPart> to = frames.get((index + 1) % frames.size());
            cached = buildInterpolated(from, to, t);
            cachedAt = now;
            return cached;
        }
    }

    // Parses a vanilla JSON text component into a real Component, preserving shadow_color.
    // Returns null when the input is not JSON, so callers can fall back to the legacy path.
    static Component parseJsonDisplayName(String input) {
        if (input == null || input.isBlank()) return null;
        String trimmed = input.trim();
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) return null;

        try {
            MutableComponent out = Component.empty();
            appendJsonComponent(JsonParser.parseString(trimmed), Style.EMPTY, out);
            return out.getSiblings().isEmpty() ? null : out;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void appendJsonComponent(JsonElement element, Style inherited, MutableComponent out) {
        if (element == null || element.isJsonNull()) return;

        if (element.isJsonPrimitive()) {
            String text = element.getAsString();
            if (!text.isEmpty()) out.append(Component.literal(text).setStyle(inherited));
            return;
        }

        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                appendJsonComponent(child, inherited, out);
            }
            return;
        }

        if (!element.isJsonObject()) return;
        JsonObject obj = element.getAsJsonObject();
        Style merged = styleFromJson(obj, inherited);

        if (obj.has("text") && obj.get("text").isJsonPrimitive()) {
            String text = obj.get("text").getAsString();
            if (!text.isEmpty()) out.append(Component.literal(text).setStyle(merged));
        }

        if (obj.has("extra") && obj.get("extra").isJsonArray()) {
            for (JsonElement child : obj.getAsJsonArray("extra")) {
                appendJsonComponent(child, merged, out);
            }
        }
    }

    private static Style styleFromJson(JsonObject obj, Style inherited) {
        Style style = inherited == null ? Style.EMPTY : inherited;

        if (obj.has("color") && obj.get("color").isJsonPrimitive()) {
            TextColor color = TextColor.parseColor(obj.get("color").getAsString()).result().orElse(null);
            if (color != null) style = style.withColor(color);
        }

        Boolean bold = readJsonBoolean(obj, "bold");
        if (bold != null) style = style.withBold(bold);
        Boolean italic = readJsonBoolean(obj, "italic");
        if (italic != null) style = style.withItalic(italic);
        Boolean underlined = readJsonBoolean(obj, "underlined");
        if (underlined != null) style = style.withUnderlined(underlined);
        Boolean strikethrough = readJsonBoolean(obj, "strikethrough");
        if (strikethrough != null) style = style.withStrikethrough(strikethrough);
        Boolean obfuscated = readJsonBoolean(obj, "obfuscated");
        if (obfuscated != null) style = style.withObfuscated(obfuscated);

        Integer shadow = readShadowColor(obj);
        if (shadow != null) style = style.withShadowColor(shadow);

        return style;
    }

    private static Boolean readJsonBoolean(JsonObject obj, String key) {
        if (!obj.has(key) || !obj.get(key).isJsonPrimitive()) return null;
        try {
            return obj.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return null;
        }
    }

    // shadow_color is either a packed ARGB int or [r, g, b, a] floats in 0..1.
    private static Integer readShadowColor(JsonObject obj) {
        if (!obj.has("shadow_color")) return null;
        JsonElement element = obj.get("shadow_color");
        if (element == null || element.isJsonNull()) return null;

        try {
            if (element.isJsonPrimitive()) return element.getAsInt();
            if (element.isJsonArray()) {
                JsonArray arr = element.getAsJsonArray();
                if (arr.size() < 4) return null;
                return ARGB.color(
                    toColorChannel(arr.get(3).getAsFloat()),
                    toColorChannel(arr.get(0).getAsFloat()),
                    toColorChannel(arr.get(1).getAsFloat()),
                    toColorChannel(arr.get(2).getAsFloat())
                );
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static int toColorChannel(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0F)));
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

    public record CosmeticScale(float x, float y, float z) {}
}
