package xenus.xenaddons.features.impl.misc;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;
import xenus.xenaddons.features.ModuleManager;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CompactPotionMessageProcessor {
    private static final String EFFECTS_SUFFIX = " Press TAB or type /effects to view your active effects!";
    private static final Pattern[] LEGACY_PATTERNS = new Pattern[]{
            Pattern.compile(".*BUFF! .*You were splashed by (?<playerName>.*) .*with (?<effectName>.+?)(?:§r)*(?:§f)*!" + Pattern.quote(EFFECTS_SUFFIX) + "$"),
            Pattern.compile(".*BUFF! .*You have gained (?<effectName>.+?)(?:§r)*(?:§f)*!" + Pattern.quote(EFFECTS_SUFFIX) + "$"),
            Pattern.compile(".*BUFF! .*You splashed yourself with (?<effectName>.+?)(?:§r)*(?:§f)*!" + Pattern.quote(EFFECTS_SUFFIX) + "$"),
            Pattern.compile(".*BUFF! .*You have gained (?<effectName>§2Poisoned Candy I)(?:§r)*(?:§f)*!$"),
            Pattern.compile(".*BUFF! .*You splashed yourself with (?<effectName>§2Poisoned Candy I)(?:§r)*(?:§f)*!$"),
            Pattern.compile(".*BUFF! .*You were splashed by (?<playerName>.*) .*with (?<effectName>§2Poisoned Candy I)(?:§r)*(?:§f)*!$")
    };
    private static final Pattern[] CLEAN_PATTERNS = new Pattern[]{
            Pattern.compile("^BUFF! You were splashed by (?<playerName>.*) with (?<effectName>.*)!" + Pattern.quote(EFFECTS_SUFFIX) + "$"),
            Pattern.compile("^BUFF! You have gained (?<effectName>.*)!" + Pattern.quote(EFFECTS_SUFFIX) + "$"),
            Pattern.compile("^BUFF! You splashed yourself with (?<effectName>.*)!" + Pattern.quote(EFFECTS_SUFFIX) + "$"),
            Pattern.compile("^BUFF! You have gained (?<effectName>Poisoned Candy I)!$"),
            Pattern.compile("^BUFF! You splashed yourself with (?<effectName>Poisoned Candy I)!$"),
            Pattern.compile("^BUFF! You were splashed by (?<playerName>.*) with (?<effectName>Poisoned Candy I)!$")
    };
    private static final Map<Integer, Character> LEGACY_COLOR_CODES = Map.ofEntries(
            Map.entry(0x000000, '0'),
            Map.entry(0x0000AA, '1'),
            Map.entry(0x00AA00, '2'),
            Map.entry(0x00AAAA, '3'),
            Map.entry(0xAA0000, '4'),
            Map.entry(0xAA00AA, '5'),
            Map.entry(0xFFAA00, '6'),
            Map.entry(0xAAAAAA, '7'),
            Map.entry(0x555555, '8'),
            Map.entry(0x5555FF, '9'),
            Map.entry(0x55FF55, 'a'),
            Map.entry(0x55FFFF, 'b'),
            Map.entry(0xFF5555, 'c'),
            Map.entry(0xFF55FF, 'd'),
            Map.entry(0xFFFF55, 'e'),
            Map.entry(0xFFFFFF, 'f')
    );

    private CompactPotionMessageProcessor() {
    }

    public static Component compact(Component original) {
        if (original == null || !isEnabled()) return original;

        String legacy = toLegacyString(original);
        for (Pattern pattern : LEGACY_PATTERNS) {
            Matcher matcher = pattern.matcher(legacy);
            if (!matcher.matches()) continue;
            return toCompactMessage(matcher, true);
        }

        String clean = original.getString();
        for (Pattern pattern : CLEAN_PATTERNS) {
            Matcher matcher = pattern.matcher(clean);
            if (!matcher.matches()) continue;
            return toCompactMessage(matcher, false);
        }
        return original;
    }

    private static Component toCompactMessage(Matcher matcher, boolean preserveEffectFormatting) {
        String effectName = matcher.group("effectName");
        String playerName = null;
        try {
            playerName = matcher.group("playerName");
        } catch (IllegalArgumentException ignored) {
            // Pattern does not define playerName.
        }

        effectName = sanitizeEffect(effectName);
        String effectPart = preserveEffectFormatting ? effectName : "§f" + effectName;
        String byPlayer = (playerName != null && !playerName.isBlank()) ? " §aby " + playerName.trim() : "";
        return Component.literal("§a§lPotion Effect! §r" + effectPart + byPlayer);
    }

    private static String sanitizeEffect(String effectName) {
        if (effectName == null) return "";
        String out = effectName;
        while (out.startsWith("§r")) out = out.substring(2);
        out = out.replaceAll("(?i)(?:§r|§f)+$", "");
        return out;
    }

    private static String toLegacyString(Component text) {
        StringBuilder out = new StringBuilder();
        text.visit((style, string) -> {
            if (!string.isEmpty()) {
                out.append(styleToLegacyPrefix(style));
                out.append(string);
            }
            return Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return out.toString();
    }

    private static String styleToLegacyPrefix(net.minecraft.network.chat.Style style) {
        if (style == null) return "";
        StringBuilder out = new StringBuilder();

        TextColor color = style.getColor();
        boolean hasDecoration =
                Boolean.TRUE.equals(style.isBold()) ||
                Boolean.TRUE.equals(style.isItalic()) ||
                Boolean.TRUE.equals(style.isUnderlined()) ||
                Boolean.TRUE.equals(style.isStrikethrough()) ||
                Boolean.TRUE.equals(style.isObfuscated());

        if (color != null || hasDecoration) {
            out.append("§r");
        }

        if (color != null) {
            int rgb = color.getValue() & 0xFFFFFF;
            Character code = LEGACY_COLOR_CODES.get(rgb);
            if (code != null) {
                out.append('§').append(code);
            } else {
                out.append(toLegacyHex(rgb));
            }
        }
        if (Boolean.TRUE.equals(style.isBold())) out.append(ChatFormatting.BOLD);
        if (Boolean.TRUE.equals(style.isItalic())) out.append(ChatFormatting.ITALIC);
        if (Boolean.TRUE.equals(style.isUnderlined())) out.append(ChatFormatting.UNDERLINE);
        if (Boolean.TRUE.equals(style.isStrikethrough())) out.append(ChatFormatting.STRIKETHROUGH);
        if (Boolean.TRUE.equals(style.isObfuscated())) out.append(ChatFormatting.OBFUSCATED);
        return out.toString();
    }

    private static String toLegacyHex(int rgb) {
        String hex = String.format("%06X", rgb);
        StringBuilder out = new StringBuilder("§x");
        for (int i = 0; i < hex.length(); i++) {
            out.append('§').append(Character.toLowerCase(hex.charAt(i)));
        }
        return out.toString();
    }

    private static boolean isEnabled() {
        CompactPotionMessageModule module = ModuleManager.getModule(CompactPotionMessageModule.class);
        return module != null && module.isEnabled();
    }
}
