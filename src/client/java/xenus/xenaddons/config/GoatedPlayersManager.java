package xenus.xenaddons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoatedPlayersManager {
    private static final Pattern LEGACY_AMP_PATTERN = Pattern.compile("(?i)&([0-9A-FK-OR])");
    private static final Pattern LEGACY_SECTION_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File GOATED_FILE = new File(
        new File(FabricLoader.getInstance().getConfigDir().toFile(), "xenaddons"),
        "goated_players.json"
    );

    private static final Map<String, GoatedEntry> GOATED_PLAYERS = new LinkedHashMap<>();
    private static final Map<String, GoatedEntry> DEFAULT_GOATED_PLAYERS = new LinkedHashMap<>();

    static {
        addDefault("xenus", "xenus", 255, 0, 255, 127);
        addDefault("xen", "xen", 255, 0, 255, 127);
        addDefault("xen62", "xen62", 170, 0, 170, 127);
        addDefault("dtxenus", "§d§ldt§5§lXenus§r", 255, 0, 255, 117);
        addDefault("dtnyro", "§5§odtNyro§r", 140, 0, 255, 117);
        addDefault("fxhz", "§b§k?§r §b§lFxhz§r §b§k?§r", 0, 255, 255, 127);
        addDefault("ziimou", "ZiziToutDur", 0, 180, 180, 127);
        addDefault("e0le_", "uwuEole", 255, 105, 180, 127);
        addDefault("avgcata12", "avgcata12", 255, 170, 0, 127);
        addDefault("antorak", "antorak", 85, 255, 85, 127);
        addDefault("icantarch", "§5§k? §5§lI§d§lCant§5§lArch§r §5§k?", 170, 0, 255, 127);
    }

    private static void addDefault(String key, String displayName, int r, int g, int b, int a) {
        GoatedEntry value = new GoatedEntry(sanitizeDisplayName(displayName), new int[] {r, g, b, a});
        String lower = key.toLowerCase();
        GOATED_PLAYERS.put(lower, value);
        DEFAULT_GOATED_PLAYERS.put(lower, value);
    }

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
        if (text == null || text.isEmpty()) return text;
        String result = text;

        for (Map.Entry<String, GoatedEntry> entry : GOATED_PLAYERS.entrySet()) {
            String key = entry.getKey();
            String replacement = boundarySafeDisplayName(entry.getValue().displayName);
            String pattern = "(?i)\\b" + Pattern.quote(key) + "\\b";
            result = result.replaceAll(pattern, Matcher.quoteReplacement(replacement));
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

        // Safety: ensure formatting does not leak to following chat text.
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

        // Visit fully-resolved styled text fragments (works for literal + translatable chat lines)
        text.visit((style, string) -> {
            appendLegacyFormatted(rebuilt, applyVisualWords(string), style);
            return java.util.Optional.empty();
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
                    out.append(Text.literal(plain.toString()).setStyle(currentStyle));
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
            out.append(Text.literal(plain.toString()).setStyle(currentStyle));
        }
    }

    public static void init() {
        File parent = GOATED_FILE.getParentFile();
        if (parent != null && !parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }

        if (!GOATED_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(GOATED_FILE)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject()) return;

            JsonObject root = element.getAsJsonObject();
            GOATED_PLAYERS.clear();
            // Always start from code defaults to preserve intended casing/style.
            for (Map.Entry<String, GoatedEntry> def : DEFAULT_GOATED_PLAYERS.entrySet()) {
                GOATED_PLAYERS.put(def.getKey(), def.getValue().copy());
            }
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                String key = entry.getKey().toLowerCase();
                JsonElement value = entry.getValue();

                // Keep hardcoded defaults exactly as defined in code.
                if (DEFAULT_GOATED_PLAYERS.containsKey(key)) continue;

                // New expected format: "name": "§dStyledName§r"
                if (value.isJsonPrimitive()) {
                    GOATED_PLAYERS.put(key, new GoatedEntry(sanitizeDisplayName(value.getAsString()), new int[] {255, 255, 255, 127}));
                    continue;
                }

                // Backward compatibility with previous object format.
                if (value.isJsonObject()) {
                    JsonObject playerObj = value.getAsJsonObject();
                    String displayName = playerObj.has("displayName")
                        ? playerObj.get("displayName").getAsString()
                        : entry.getKey();
                    GOATED_PLAYERS.put(key, new GoatedEntry(sanitizeDisplayName(displayName), new int[] {255, 255, 255, 127}));
                }
            }
        } catch (IOException ignored) {}
    }

    public static void save() {
        JsonObject root = new JsonObject();

        for (Map.Entry<String, GoatedEntry> entry : GOATED_PLAYERS.entrySet()) {
            root.addProperty(entry.getKey(), entry.getValue().displayName);
        }

        try (FileWriter writer = new FileWriter(GOATED_FILE)) {
            GSON.toJson(root, writer);
        } catch (IOException ignored) {}
    }

    private static final class GoatedEntry {
        private final String displayName;
        private final int[] highlightColor;

        private GoatedEntry(String displayName, int[] highlightColor) {
            this.displayName = displayName;
            this.highlightColor = highlightColor;
        }

        private GoatedEntry copy() {
            return new GoatedEntry(displayName, highlightColor.clone());
        }
    }
}
