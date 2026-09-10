package xenus.xenaddons.features.impl.misc;

import net.minecraft.network.chat.Component;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.BooleanSetting;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemPriceModule extends Module {

    private static final int DEFAULT_STACK = 64;

    /**
     * Lines that state how much of an item you actually hold. Group 1 is the amount and group 2 its
     * optional k/M/B suffix, since Hypixel abbreviates large counts.
     */
    private static final Pattern[] QUANTITY_PATTERNS = {
        // Sack entry: "Stored: 152/22.2k". Only the first number counts, the second is the capacity.
        Pattern.compile("^Stored:\\s*([\\d.,]+)\\s*([kKmMbB])?\\s*/"),
        // Shard menu entry: "Owned: 24 Shards".
        Pattern.compile("^Owned:\\s*([\\d.,]+)\\s*([kKmMbB])?\\s+Shards?\\b", Pattern.CASE_INSENSITIVE)
    };

    public final BooleanSetting showAhPrice = new BooleanSetting("AH Price", true);
    public final BooleanSetting showBzPrice = new BooleanSetting("Bazaar Price", true);

    public ItemPriceModule() {
        super("Item Price", "Displays item prices in SkyBlock tooltips", Category.MISC);
        this.registerSetting(showAhPrice);
        this.registerSetting(showBzPrice);
    }

    /**
     * How much SHIFT should multiply the prices by. In a sack or the shard menu the useful figure is
     * what you actually hold, not a vanilla stack, so the tooltip's own count wins over the stack.
     */
    public static int resolveShiftAmount(List<Component> tooltip, int stackCount) {
        int held = readQuantity(tooltip);
        if (held > 0) return held;
        return stackCount > 1 ? stackCount : DEFAULT_STACK;
    }

    /** The amount the tooltip says you hold, or -1 when it does not state one. */
    static int readQuantity(List<Component> tooltip) {
        if (tooltip == null) return -1;

        for (Component line : tooltip) {
            if (line == null) continue;

            String text = stripFormatting(line.getString());
            for (Pattern pattern : QUANTITY_PATTERNS) {
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) return parseAmount(matcher.group(1), matcher.group(2));
            }
        }
        return -1;
    }

    private static int parseAmount(String digits, String suffix) {
        double value;
        try {
            value = Double.parseDouble(digits.replace(",", ""));
        } catch (NumberFormatException ignored) {
            return -1;
        }

        if (suffix != null && !suffix.isEmpty()) {
            value *= switch (Character.toLowerCase(suffix.charAt(0))) {
                case 'k' -> 1_000D;
                case 'm' -> 1_000_000D;
                case 'b' -> 1_000_000_000D;
                default -> 1D;
            };
        }

        if (value < 1) return -1;
        return (int) Math.min(Integer.MAX_VALUE, Math.round(value));
    }

    private static String stripFormatting(String input) {
        if (input == null) return "";
        return input.replaceAll("(?i)[§&][0-9A-FK-OR]", "").trim();
    }
}
