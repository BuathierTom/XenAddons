package xenus.xenaddons.features.impl.slayer;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import xenus.xenaddons.utils.slayer.SlayerTypes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Alerts on "SLAYER MINI-BOSS <mob> has spawned!". Only the mob name changes between slayers, so
 * matching the fixed prefix covers all of them without a name table.
 */
public class SlayerMiniBossAlertModule extends SlayerAlertModule {

    private static final Pattern SPAWN_PATTERN = Pattern.compile("^SLAYER MINI-BOSS (.+) has spawned!$");
    // The same line can reach us through both chat listeners; ignore the echo.
    private static final long DEBOUNCE_MS = 500L;

    private String lastMessage = "";
    private long lastMessageAt = 0L;

    public SlayerMiniBossAlertModule() {
        super("Mini-Boss Alert", "Alerts when a slayer mini-boss spawns",
                "&6Mini-Boss!", 165.0f, 3.0f);

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!this.isEnabled() || overlay) return;
            handleChat(message.getString());
        });

        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            if (this.isEnabled()) handleChat(message.getString());
            return true;
        });
    }

    private void handleChat(String rawMessage) {
        if (rawMessage == null || rawMessage.isEmpty()) return;

        String clean = SlayerTypes.stripFormatting(rawMessage).trim();
        Matcher matcher = SPAWN_PATTERN.matcher(clean);
        if (!matcher.matches()) return;

        long now = System.currentTimeMillis();
        if (clean.equals(lastMessage) && (now - lastMessageAt) < DEBOUNCE_MS) return;
        lastMessage = clean;
        lastMessageAt = now;

        fireAlert();
    }
}
