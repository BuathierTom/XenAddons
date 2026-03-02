package xenus.xenaddons.features.impl.dungeons;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.utils.ChatUtils;
import xenus.xenaddons.utils.DungeonUtils;

public class DungeonNotFullAlertModule extends Module {
    private int pendingCheckTicks = -1;
    private int retries = 0;

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
                return;
            }

            if (pendingCheckTicks < 0) return;
            if (--pendingCheckTicks > 0) return;

            int players = DungeonUtils.getDungeonPartyCountFromTab();
            if (players <= 0 && retries > 0) {
                retries--;
                pendingCheckTicks = 10; // retry ~0.5s later
                return;
            }

            pendingCheckTicks = -1;
            retries = 0;
            if (players <= 0 || players >= 5) return;

            if (mc.inGameHud != null) {
                mc.inGameHud.setTitle(Text.of("§cNot 5/5!"));
                mc.inGameHud.setTitleTicks(10, 40, 10);
            }

            ChatUtils.warning("Dungeon not full (" + players + "/5)!");
        });
    }

    private void handleStartMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isEmpty()) return;

        String stripped = rawMessage.replaceAll("§[0-9a-fk-or]", "").trim();
        if (!"Starting in 4 seconds.".equals(stripped)) return;
        // Delay the check because tab/class lines can still be syncing at this moment.
        pendingCheckTicks = 20; // ~1s
        retries = 4;            // up to ~3s total wait
    }
}
