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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoatedPlayersManager {
    private static final Pattern LEGACY_AMP_PATTERN = Pattern.compile("(?i)&([0-9A-FK-OR])");
    private static final Pattern LEGACY_SECTION_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");
    private static final String VISUAL_WORDS_RESOURCE = "/assets/xenaddons/visual/visualWords.json";
    private static final String CAPY_MARKER = "{capy}";
    private static final String FRANCE_MARKER = "{france}";
    private static final String CAPY_GLYPH = "\uE100";
    private static final String FRANCE_GLYPH = "\uE101";
    private static final Identifier CAPY_FONT = Identifier.of("xenaddons", "capybara");
    private static final Identifier FRANCE_FONT = Identifier.of("xenaddons", "france");

    private static final Map<String, GoatedEntry> GOATED_PLAYERS = new LinkedHashMap<>();

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
        String result = text;

        for (Map.Entry<String, GoatedEntry> entry : GOATED_PLAYERS.entrySet()) {
            String key = entry.getKey();
            String replacement = boundarySafeDisplayName(entry.getValue().displayName);
            String pattern = "(?i)\\b" + Pattern.quote(key) + "\\b";
            result = result.replaceAll(pattern, Matcher.quoteReplacement(replacement));
        }

        if (!keepMarkers) {
            result = result.replace(CAPY_MARKER, "");
            result = result.replace(FRANCE_MARKER, "");
        }

        return result;
    }

    public static String normalizeLegacyCodes(String text) {
        if (text == null || text.isEmpty()) return text;
        Matcher matcher = LEGACY_AMP_PATTERN.matcher(text);
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
}
