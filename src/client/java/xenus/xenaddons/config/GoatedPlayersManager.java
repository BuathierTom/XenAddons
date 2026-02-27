package xenus.xenaddons.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoatedPlayersManager {
    private static final Pattern LEGACY_AMP_PATTERN = Pattern.compile("(?i)&([0-9A-FK-OR])");
    private static final Pattern HEX_AMP_PATTERN = Pattern.compile("(?i)&\\#([0-9a-f]{6})");
    private static final Pattern LEGACY_SECTION_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");
    private static final String VISUAL_WORDS_RESOURCE = "/assets/xenaddons/visual/visualWords.json";
    private static final String CAPY_MARKER = "{capy}";
    private static final String FRANCE_MARKER = "{france}";
    private static final String CAPY_GLYPH = "\uE100";
    private static final String FRANCE_GLYPH = "\uE101";
    private static final Identifier CAPY_FONT = Identifier.of("xenaddons", "capybara");
    private static final Identifier FRANCE_FONT = Identifier.of("xenaddons", "france");
    private static final String[] CONCAT_STATUS_SUFFIXES = new String[] {"offline", "online"};
    private static final int VISUAL_WORDS_CACHE_SIZE = 2048;

    private static final Map<String, GoatedEntry> GOATED_PLAYERS = new LinkedHashMap<>();
    private static final Map<String, String> VISUAL_WORDS_CACHE = Collections.synchronizedMap(
        new LinkedHashMap<>(VISUAL_WORDS_CACHE_SIZE + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > VISUAL_WORDS_CACHE_SIZE;
            }
        }
    );

    public static boolean isGoated(String name) {
        if (name == null) return false;
        return GOATED_PLAYERS.containsKey(name.toLowerCase());
    }

    public static String getDisplayName(String name) {
        if (name == null) return "";
        GoatedEntry entry = GOATED_PLAYERS.get(name.toLowerCase());
        return entry != null ? entry.displayName : name;
    }

    public static int[] getHighlightColor(String name) {
        if (name == null) return null;
        GoatedEntry entry = GOATED_PLAYERS.get(name.toLowerCase());
        return entry != null ? entry.highlightColor.clone() : null;
    }

    public static String applyVisualWords(String text) {
        return applyVisualWordsInternal(text, false);
    }

    private static String applyVisualWordsInternal(String text, boolean keepMarkers) {
        if (text == null || text.isEmpty()) return text;
        String cacheKey = (keepMarkers ? "1:" : "0:") + text;
        String cached = VISUAL_WORDS_CACHE.get(cacheKey);
        if (cached != null) return cached;
        String result = text;

        for (Map.Entry<String, GoatedEntry> entry : GOATED_PLAYERS.entrySet()) {
            String key = entry.getKey();
            String replacement = boundarySafeDisplayName(entry.getValue().displayName);
            result = replaceWithSmartBoundaries(result, key, replacement);
        }

        if (!keepMarkers) {
            result = result.replace(CAPY_MARKER, "");
            result = result.replace(FRANCE_MARKER, "");
        }

        VISUAL_WORDS_CACHE.put(cacheKey, result);
        return result;
    }

    private static String replaceWithSmartBoundaries(String input, String needle, String replacement) {
        if (input == null || input.isEmpty() || needle == null || needle.isEmpty()) return input;

        String loweredInput = input.toLowerCase();
        String[] friendListStatusPrefixes = new String[] {
            needle + " is ",
            needle + " est ",
            needle + " was ",
            needle + " has "
        };
        for (String prefix : friendListStatusPrefixes) {
            int idx = loweredInput.indexOf(prefix.toLowerCase());
            while (idx >= 0) {
                input = input.substring(0, idx) + replacement + input.substring(idx + needle.length());
                loweredInput = input.toLowerCase();
                idx = loweredInput.indexOf(prefix.toLowerCase(), idx + replacement.length());
            }
        }

        StringBuilder out = new StringBuilder(input.length());
        int i = 0;
        int n = input.length();
        int m = needle.length();

        while (i < n) {
            if (i + m <= n && input.regionMatches(true, i, needle, 0, m) && hasSmartBoundary(input, i, i + m)) {
                out.append(replacement);
                i += m;
            } else {
                out.append(input.charAt(i));
                i++;
            }
        }
        return out.toString();
    }

    private static boolean hasSmartBoundary(String s, int start, int end) {
        char first = s.charAt(start);
        char last = s.charAt(end - 1);

        boolean leftOk;
        if (start == 0) {
            leftOk = true;
        } else {
            int prevIdx = previousVisibleCharIndex(s, start - 1);
            char prev = prevIdx >= 0 ? s.charAt(prevIdx) : '\0';
            leftOk = !isWordChar(prev)
                || (Character.isLowerCase(prev) && Character.isUpperCase(first))
                || hasConcatenatedStatusPrefix(s, start);
        }

        boolean rightOk;
        if (end >= s.length()) {
            rightOk = true;
        } else {
            int nextIdx = nextVisibleCharIndex(s, end);
            char next = nextIdx >= 0 ? s.charAt(nextIdx) : '\0';
            rightOk = !isWordChar(next) || (Character.isLowerCase(last) && Character.isUpperCase(next));
        }

        return leftOk && rightOk;
    }

    private static boolean hasConcatenatedStatusPrefix(String s, int start) {
        String leftWord = readLeftVisibleWord(s, start).toLowerCase();
        for (String suffix : CONCAT_STATUS_SUFFIXES) {
            if (leftWord.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private static String readLeftVisibleWord(String s, int start) {
        StringBuilder out = new StringBuilder();
        int i = previousVisibleCharIndex(s, start - 1);
        while (i >= 0) {
            char c = s.charAt(i);
            if (!isWordChar(c)) break;
            out.append(c);
            i = previousVisibleCharIndex(s, i - 1);
        }
        return out.reverse().toString();
    }

    private static int previousVisibleCharIndex(String s, int from) {
        int i = from;
        while (i >= 0) {
            if (isLegacyCodePairAt(s, i - 1)) {
                i -= 2;
                continue;
            }
            if (s.charAt(i) == '§' && i + 1 < s.length() && isLegacyCodeChar(s.charAt(i + 1))) {
                i--;
                continue;
            }
            return i;
        }
        return -1;
    }

    private static int nextVisibleCharIndex(String s, int from) {
        int i = from;
        while (i < s.length()) {
            if (isLegacyCodePairAt(s, i)) {
                i += 2;
                continue;
            }
            return i;
        }
        return -1;
    }

    private static boolean isLegacyCodePairAt(String s, int idx) {
        return idx >= 0 && idx + 1 < s.length() && s.charAt(idx) == '§' && isLegacyCodeChar(s.charAt(idx + 1));
    }

    private static boolean isLegacyCodeChar(char c) {
        char lower = Character.toLowerCase(c);
        return (lower >= '0' && lower <= '9') || (lower >= 'a' && lower <= 'f') || (lower >= 'k' && lower <= 'o') || lower == 'r' || lower == 'x';
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
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
        MutableText rebuilt = Text.empty();
        text.visit((style, string) -> {
            appendLegacyFormatted(rebuilt, applyVisualWordsInternal(string, true), style);
            return Optional.empty();
        }, Style.EMPTY);
        return rebuilt;
    }

    private static void appendLegacyFormatted(MutableText out, String input, Style baseStyle) {
        if (input == null || input.isEmpty()) return;
        String normalized = normalizeLegacyCodes(input);
        StringBuilder plain = new StringBuilder();
        Style currentStyle = baseStyle;

        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c == '§' && i + 1 < normalized.length()) {
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

            int marker;
            boolean isCaby;
            if (capyIdx < 0 && franceIdx < 0) {
                marker = -1;
                isCaby = false;
            } else if (capyIdx >= 0 && (franceIdx < 0 || capyIdx < franceIdx)) {
                marker = capyIdx;
                isCaby = true;
            } else {
                marker = franceIdx;
                isCaby = false;
            }

            if (marker < 0) {
                out.append(Text.literal(plain.substring(idx)).setStyle(style));
                break;
            }

            if (marker > idx) {
                out.append(Text.literal(plain.substring(idx, marker)).setStyle(style));
            }

            if (isCaby) {
                Style iconStyle = style.withColor(Formatting.WHITE).withFont(new StyleSpriteSource.Font(CAPY_FONT));
                out.append(Text.literal(CAPY_GLYPH).setStyle(iconStyle));
                idx = marker + CAPY_MARKER.length();
            } else {
                Style iconStyle = style.withColor(Formatting.WHITE).withFont(new StyleSpriteSource.Font(FRANCE_FONT));
                out.append(Text.literal(FRANCE_GLYPH).setStyle(iconStyle));
                idx = marker + FRANCE_MARKER.length();
            }
        }
    }

    public static void init() {
        GOATED_PLAYERS.clear();
        VISUAL_WORDS_CACHE.clear();
        loadFromResources();
    }

    private static boolean loadFromResources() {
        InputStream stream = GoatedPlayersManager.class.getResourceAsStream(VISUAL_WORDS_RESOURCE);
        if (stream == null) return false;

        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject()) return false;
            return loadFromJsonObject(element.getAsJsonObject());
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean loadFromJsonObject(JsonObject root) {
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String key = entry.getKey().toLowerCase();
            JsonElement value = entry.getValue();

            if (value.isJsonPrimitive()) {
                String displayName = sanitizeDisplayName(value.getAsString());
                GOATED_PLAYERS.put(key, new GoatedEntry(displayName, new int[] {255, 255, 255, 127}));
            } else if (value.isJsonObject()) {
                JsonObject obj = value.getAsJsonObject();
                String displayName = obj.has("displayName")
                    ? sanitizeDisplayName(obj.get("displayName").getAsString())
                    : sanitizeDisplayName(entry.getKey());
                GOATED_PLAYERS.put(key, new GoatedEntry(displayName, new int[] {255, 255, 255, 127}));
            }
        }
        return !GOATED_PLAYERS.isEmpty();
    }

    private static final class GoatedEntry {
        private final String displayName;
        private final int[] highlightColor;

        private GoatedEntry(String displayName, int[] highlightColor) {
            this.displayName = displayName;
            this.highlightColor = highlightColor;
        }
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
}
