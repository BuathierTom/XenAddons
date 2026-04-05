package xenus.xenaddons.features.impl.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.ChatScreen;

public final class PeekChatController {
    private static boolean peekHeld = false;

    private PeekChatController() {}

    public static void setPeekHeld(boolean held) {
        peekHeld = held;
    }

    public static boolean shouldRenderPeek(MinecraftClient client) {
        return peekHeld
                && client != null
                && client.player != null
                && client.inGameHud != null
                && client.currentScreen == null;
    }

    public static boolean shouldUseFocusedHeight(MinecraftClient client) {
        return shouldRenderPeek(client);
    }

    public static void stop(MinecraftClient client) {
        boolean wasHeld = peekHeld;
        peekHeld = false;
        if (!wasHeld || client == null || client.inGameHud == null) return;
        if (client.currentScreen instanceof ChatScreen) return;

        ChatHud chatHud = client.inGameHud.getChatHud();
        if (chatHud != null) {
            chatHud.resetScroll();
        }
    }
}
