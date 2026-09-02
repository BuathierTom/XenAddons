package xenus.xenaddons.features.impl.slayer;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.utils.slayer.SlayerTypes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Alerts on "CAUGHT! You cocooned a <mob>!", with an optional party announce naming the mob. */
public class SlayerCocoonAlertModule extends SlayerAlertModule {

    private static final Pattern COCOON_PATTERN = Pattern.compile("^CAUGHT! You cocooned a (.+)!$");
    private static final long DEBOUNCE_MS = 500L;
    // Hypixel throttles chat; never let a burst of catches turn into a burst of party messages.
    private static final long PARTY_COOLDOWN_MS = 3_000L;

    private final BooleanSetting announceInParty = new BooleanSetting("Announce in party chat", false);

    private String lastMessage = "";
    private long lastMessageAt = 0L;
    private long lastPartyAnnounceAt = 0L;

    public SlayerCocoonAlertModule() {
        super("Cocoon Alert", "Alerts when you cocoon a slayer mob",
                "slayer_cocoon_hud", "&cCocoon!", 190, 3.0);

        this.registerSetting(announceInParty);

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
        Matcher matcher = COCOON_PATTERN.matcher(clean);
        if (!matcher.matches()) return;

        long now = System.currentTimeMillis();
        if (clean.equals(lastMessage) && (now - lastMessageAt) < DEBOUNCE_MS) return;
        lastMessage = clean;
        lastMessageAt = now;

        fireAlert();

        if (announceInParty.getValue() && (now - lastPartyAnnounceAt) >= PARTY_COOLDOWN_MS) {
            lastPartyAnnounceAt = now;
            announce(matcher.group(1).trim());
        }
    }

    private void announce(String mobName) {
        if (mc.player == null || mc.player.connection == null) return;
        mc.player.connection.sendCommand("pc " + mobName + " cocooned!");
    }
}
