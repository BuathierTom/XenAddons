package xenus.xenaddons.features.impl.misc;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import xenus.xenaddons.compat.XenChatHudCompat;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;

public class CompactStashModule extends Module {
    private static final Pattern FORMATTING_PATTERN = Pattern.compile("§.");
    private static final Pattern COUNT_PATTERN = Pattern.compile("^You have (?<count>[\\d,]+) (?<type>item|material)s? stashed away!$");
    private static final Pattern TOTALS_PATTERN = Pattern.compile("^\\(This totals [\\d,]+ types? of (?:item|material)s? stashed!\\)$");
    private static final Pattern PICKUP_PATTERN = Pattern.compile("^>>> CLICK HERE to pick (?:them|it) up! <<<$");
    private static final Pattern ADDED_PATTERN = Pattern.compile(
        "^One or more (?<type>item|material)s? didn't fit in your inventory and were added to your (?:item|material) stash! Click here to pick them up!$"
    );
    private static final String PICKUP_COMMAND = "/pickupstash";
    private static final long BLANK_WINDOW_MS = 500L;
    private static final int MAX_LEADING_BLANKS = 2;

    private long lastBlankAt;
    private int recentBlanks;
    private long skipBlankUntil;

    public CompactStashModule() {
        super("Compact Stash", "Replaces the stash reminders with one clickable line that runs /pickupstash", Category.MISC);

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> overlay || !this.isEnabled() || handle(message));
    }

    private boolean handle(Component message) {
        String raw = FORMATTING_PATTERN.matcher(message.getString()).replaceAll("");
        long now = System.currentTimeMillis();
        if (raw.contains("\n")) return handleBlock(raw, now);

        String text = raw.strip();

        if (text.isEmpty()) {
            if (now <= skipBlankUntil) {
                skipBlankUntil = 0L;
                return false;
            }
            recentBlanks = now - lastBlankAt <= BLANK_WINDOW_MS ? Math.min(recentBlanks + 1, MAX_LEADING_BLANKS) : 1;
            lastBlankAt = now;
            return true;
        }

        Matcher count = COUNT_PATTERN.matcher(text);
        if (count.matches()) {
            removeLeadingBlanks(now);
            sendCount(count);
            return false;
        }

        if (TOTALS_PATTERN.matcher(text).matches()) return false;

        if (PICKUP_PATTERN.matcher(text).matches()) {
            skipBlankUntil = now + BLANK_WINDOW_MS;
            return false;
        }

        Matcher added = ADDED_PATTERN.matcher(text);
        if (added.matches()) {
            sendAdded(added);
            skipBlankUntil = now + BLANK_WINDOW_MS;
            return false;
        }

        recentBlanks = 0;
        return true;
    }

    private boolean handleBlock(String raw, long now) {
        for (String line : raw.split("\n")) {
            String text = line.strip();
            Matcher count = COUNT_PATTERN.matcher(text);
            if (count.matches()) {
                removeLeadingBlanks(now);
                sendCount(count);
                skipBlankUntil = now + BLANK_WINDOW_MS;
                return false;
            }

            Matcher added = ADDED_PATTERN.matcher(text);
            if (added.matches()) {
                sendAdded(added);
                skipBlankUntil = now + BLANK_WINDOW_MS;
                return false;
            }
        }

        recentBlanks = 0;
        return true;
    }

    private void sendCount(Matcher count) {
        String type = count.group("type");
        String amount = count.group("count");
        String plural = "1".equals(amount) ? "" : "s";
        send(type, "You have " + accent(type) + amount + main(type) + " " + type + plural + " stashed away!");
    }

    private void sendAdded(Matcher added) {
        String type = added.group("type");
        send(type, "Some " + type + "s didn't fit and were added to your stash!");
    }

    private void removeLeadingBlanks(long now) {
        if (recentBlanks <= 0 || now - lastBlankAt > BLANK_WINDOW_MS) return;

        ChatComponent chat = mc.gui.getChat();
        List<GuiMessage> messages = XenChatHudCompat.getMessages(chat);
        int removed = 0;
        while (removed < recentBlanks && !messages.isEmpty() && messages.get(0).content().getString().isBlank()) {
            messages.remove(0);
            removed++;
        }
        recentBlanks = 0;
        if (removed > 0) chat.rescaleChat();
    }

    private void send(String type, String body) {
        String prefix = accent(type) + ("material".equals(type) ? "Material" : "Item") + " Stash §7> ";
        Component line = Component.literal(prefix + main(type) + body).withStyle(style -> style
            .withClickEvent(new ClickEvent.RunCommand(PICKUP_COMMAND))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("§eClick to run " + PICKUP_COMMAND))));
        mc.gui.getChat().addClientSystemMessage(line);
    }

    private static String main(String type) {
        return "material".equals(type) ? "§b" : "§e";
    }

    private static String accent(String type) {
        return "material".equals(type) ? "§3" : "§6";
    }
}
