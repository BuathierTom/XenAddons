package xenus.xenaddons.compat;

import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Component;
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

    public static FormattedText replaceStringVisitable(FormattedText text) {
        if (text instanceof Component t) return GoatedPlayersManager.applyVisualWords(t);
        return text;
    }

    public static FormattedCharSequence replaceOrderedText(FormattedCharSequence text) {
        return GoatedPlayersManager.applyVisualWords(text);
    }

    public static Component replaceText(Component text) {
        return GoatedPlayersManager.applyVisualWords(text);
    }

    public static Component replaceTooltip(Component text) {
        return GoatedPlayersManager.applyVisualWordsForTooltip(text);
    }

    public static List<Component> replaceTooltipList(List<Component> lines) {
        if (lines == null || lines.isEmpty()) return lines;
        List<Component> replaced = new ArrayList<>(lines.size());
        for (Component line : lines) {
            replaced.add(replaceTooltip(line));
        }
        return replaced;
    }

    public static String normalizeLegacyCodes(String text) {
        return GoatedPlayersManager.normalizeLegacyCodes(text);
    }
}
