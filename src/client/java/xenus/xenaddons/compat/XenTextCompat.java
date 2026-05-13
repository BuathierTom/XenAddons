package xenus.xenaddons.compat;

import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import xenus.xenaddons.config.GoatedPlayersManager;

import java.util.ArrayList;
import java.util.List;

public final class XenTextCompat {
    private XenTextCompat() {}

    public static String replacePlainString(String text) {
        if (text == null || text.indexOf('§') >= 0 || text.indexOf('&') >= 0) return text;
        return GoatedPlayersManager.applyVisualWordsPlain(text);
    }

    public static String replaceVisualWords(String text) {
        return GoatedPlayersManager.applyVisualWords(text);
    }

    public static StringVisitable replaceStringVisitable(StringVisitable text) {
        if (text instanceof Text t) return GoatedPlayersManager.applyVisualWords(t);
        return text;
    }

    public static OrderedText replaceOrderedText(OrderedText text) {
        return GoatedPlayersManager.applyVisualWords(text);
    }

    public static Text replaceText(Text text) {
        return GoatedPlayersManager.applyVisualWords(text);
    }

    public static Text replaceTooltip(Text text) {
        return GoatedPlayersManager.applyVisualWordsForTooltip(text);
    }

    public static List<Text> replaceTooltipList(List<Text> lines) {
        if (lines == null || lines.isEmpty()) return lines;
        List<Text> replaced = new ArrayList<>(lines.size());
        for (Text line : lines) {
            replaced.add(replaceTooltip(line));
        }
        return replaced;
    }

    public static String normalizeLegacyCodes(String text) {
        return GoatedPlayersManager.normalizeLegacyCodes(text);
    }
}
