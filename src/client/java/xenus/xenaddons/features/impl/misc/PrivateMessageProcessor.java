package xenus.xenaddons.features.impl.misc;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Hypixel sends private messages as "<light purple>From <rank + name><gray>: <gray>text", which
// reads exactly like every other line in chat and gets lost. This rebuilds them as
// "Msg > From <rank + name>: text" with the body in white, keeping the rank formatting verbatim.
public final class PrivateMessageProcessor {
    private static final String SEPARATOR = ": ";
    private static final String[] DIRECTIONS = {"From ", "To "};
    private static final int HYPIXEL_PM_COLOR = ChatFormatting.LIGHT_PURPLE.getColor() == null
            ? 0xFF55FF
            : ChatFormatting.LIGHT_PURPLE.getColor();

    private PrivateMessageProcessor() {
    }

    public static Component format(Component original) {
        if (original == null || !PrivateMessageModule.isActive()) return original;

        List<Run> runs = flatten(original);
        if (runs.isEmpty()) return original;

        String plain = plainText(runs);
        String direction = directionOf(plain);
        // The colour guard is what keeps a player message that happens to start with "From " from
        // being rewritten: only Hypixel's own prefix is light purple.
        if (direction == null || !isHypixelPmColor(runs.get(0).style())) return original;

        int separator = plain.indexOf(SEPARATOR, direction.length());
        // A private message always carries a sender between the direction word and the colon.
        if (separator <= direction.length()) return original;

        return rebuild(runs, separator + SEPARATOR.length());
    }

    private static Component rebuild(List<Run> runs, int bodyStart) {
        MutableComponent out = Component.empty();
        out.append(Component.literal("Msg ").withStyle(ChatFormatting.LIGHT_PURPLE));
        out.append(Component.literal("> ").withStyle(ChatFormatting.DARK_GRAY));

        int cursor = 0;
        for (Run run : runs) {
            int start = cursor;
            int end = start + run.text().length();
            cursor = end;

            if (end <= bodyStart) {
                // Header: direction word, rank and name, kept exactly as Hypixel sent them.
                out.append(Component.literal(run.text()).setStyle(run.style()));
            } else if (start >= bodyStart) {
                out.append(Component.literal(run.text()).setStyle(whiten(run.style())));
            } else {
                // The colon and the message body share a single run, so split it in place.
                int cut = bodyStart - start;
                out.append(Component.literal(run.text().substring(0, cut)).setStyle(run.style()));
                out.append(Component.literal(run.text().substring(cut)).setStyle(whiten(run.style())));
            }
        }
        return out;
    }

    // Keeps any hover or click event the run carries, only the colour changes.
    private static Style whiten(Style style) {
        return style.withColor(ChatFormatting.WHITE);
    }

    private static String directionOf(String plain) {
        for (String direction : DIRECTIONS) {
            if (plain.startsWith(direction)) return direction;
        }
        return null;
    }

    private static boolean isHypixelPmColor(Style style) {
        TextColor color = style.getColor();
        return color != null && (color.getValue() & 0xFFFFFF) == HYPIXEL_PM_COLOR;
    }

    private static String plainText(List<Run> runs) {
        StringBuilder out = new StringBuilder();
        for (Run run : runs) out.append(run.text());
        return out.toString();
    }

    // visit() resolves style inheritance for us, so every run carries its effective style.
    private static List<Run> flatten(Component text) {
        List<Run> runs = new ArrayList<>();
        text.visit((style, string) -> {
            if (!string.isEmpty()) runs.add(new Run(string, style));
            return Optional.empty();
        }, Style.EMPTY);
        return runs;
    }

    private record Run(String text, Style style) {
    }
}
