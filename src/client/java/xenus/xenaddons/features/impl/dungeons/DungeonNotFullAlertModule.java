package xenus.xenaddons.features.impl.dungeons;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.utils.ChatUtils;
import xenus.xenaddons.utils.DungeonUtils;

public class DungeonNotFullAlertModule extends Module {

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
    }

    private void handleStartMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isEmpty()) return;

        String stripped = rawMessage.replaceAll("§[0-9a-fk-or]", "").trim();
        if (!"Starting in 4 seconds.".equals(stripped)) return;

        int players = DungeonUtils.getDungeonPartyCountFromTab();
        if (players <= 0 && mc.player != null) {
            players = 1;
        }
        if (players >= 5) return;

        if (mc.inGameHud != null) {
            mc.inGameHud.setTitle(Text.of("§cNot 5/5!"));
            mc.inGameHud.setTitleTicks(10, 40, 10);
        }

        ChatUtils.warning("Dungeon not full (" + players + "/5)!");
    }
}
