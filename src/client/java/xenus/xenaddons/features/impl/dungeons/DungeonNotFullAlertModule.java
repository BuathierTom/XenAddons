package xenus.xenaddons.features.impl.dungeons;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.utils.ChatUtils;
import xenus.xenaddons.utils.DungeonUtils;

import java.util.regex.Pattern;

public class DungeonNotFullAlertModule extends Module {
    private static final Pattern STARTING_IN_PATTERN =
        Pattern.compile("^Starting in \\d+ second(?:s)?\\.$", Pattern.CASE_INSENSITIVE);
    private static final long START_TRIGGER_DEBOUNCE_MS = 15_000L;

    private int pendingCheckTicks = -1;
    private int retries = 0;
    private long lastStartTriggerMs = 0L;
    private boolean alertedForCurrentStart = false;

    public DungeonNotFullAlertModule() {
        super("Dungeon not full alert", "Shows an alert when the dungeon starts but it is not 5/5", Category.DUNGEON);

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!this.isEnabled() || overlay) return;
            handleStartMessage(message.getString());
        });

        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            if (this.isEnabled()) {
                handleStartMessage(message.getString());
            }
            return true;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!this.isEnabled()) {
                pendingCheckTicks = -1;
                retries = 0;
                alertedForCurrentStart = false;
                return;
            }

            if (pendingCheckTicks < 0) return;
            if (--pendingCheckTicks > 0) return;

            int players = DungeonUtils.getDungeonPartyCountFromTab();
            if (players <= 0) {
                players = DungeonUtils.getDetectedDungeonClassCount();
            }

            if (players <= 0 && retries > 0) {
                retries--;
                pendingCheckTicks = 10; // retry ~0.5s later
                return;
            }

            pendingCheckTicks = -1;
            retries = 0;
            if (alertedForCurrentStart) return;
            if (players <= 0 || players >= 5) return;

            if (mc.gui != null) {
                mc.gui.setTitle(Component.literal("§cNot 5/5!"));
                mc.gui.setTimes(10, 40, 10);
            }

            ChatUtils.warning("Dungeon not full (" + players + "/5)!");
            alertedForCurrentStart = true;
        });
    }

    private void handleStartMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isEmpty()) return;

        String stripped = rawMessage.replaceAll("(?i)§[0-9A-FK-OR]", "").trim();
        if (!STARTING_IN_PATTERN.matcher(stripped).matches()) return;
        long now = System.currentTimeMillis();
        if (pendingCheckTicks >= 0) return;
        if (now - lastStartTriggerMs < START_TRIGGER_DEBOUNCE_MS) return;
        lastStartTriggerMs = now;
        alertedForCurrentStart = false;
        // Delay the check because tab/class lines can still be syncing at this moment.
        pendingCheckTicks = 20; // ~1s
        retries = 8;            // up to ~5s total wait
    }
}
