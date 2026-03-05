package xenus.xenaddons.features.impl.misc;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;

public class CompactPotionMessageModule extends Module {
    private static boolean listenersRegistered = false;

    public CompactPotionMessageModule() {
        super("Compact Potion Message", "Shorten chat messages about player potion effects", Category.MISC);

        if (!listenersRegistered) {
            listenersRegistered = true;
            ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
                CompactPotionMessageModule module = ModuleManager.getModule(CompactPotionMessageModule.class);
                if (module == null || !module.isEnabled()) return true;

                Text compacted = CompactPotionMessageProcessor.compact(message);
                if (compacted == message) return true;

                MinecraftClient client = MinecraftClient.getInstance();
                if (client.inGameHud != null) {
                    client.inGameHud.getChatHud().addMessage(compacted);
                }
                return false;
            });

            ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
                CompactPotionMessageModule module = ModuleManager.getModule(CompactPotionMessageModule.class);
                if (module == null || !module.isEnabled()) return true;

                Text compacted = CompactPotionMessageProcessor.compact(message);
                if (compacted == message) return true;

                MinecraftClient client = MinecraftClient.getInstance();
                if (client.inGameHud != null) {
                    client.inGameHud.getChatHud().addMessage(compacted);
                }
                return false;
            });
        }
    }
}
