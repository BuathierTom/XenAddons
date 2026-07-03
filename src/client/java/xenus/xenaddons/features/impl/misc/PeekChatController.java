package xenus.xenaddons.features.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;

public final class PeekChatController {
    private static boolean peekHeld = false;

    private PeekChatController() {}

    public static void setPeekHeld(boolean held) {
        peekHeld = held;
    }

    public static boolean shouldRenderPeek(Minecraft client) {
        return peekHeld
                && client != null
                && client.player != null
                && client.gui != null
                && client.screen == null;
    }

    public static boolean shouldUseFocusedHeight(Minecraft client) {
        return shouldRenderPeek(client);
    }

    public static void stop(Minecraft client) {
        boolean wasHeld = peekHeld;
        peekHeld = false;
        if (!wasHeld || client == null || client.gui == null) return;
        if (client.screen instanceof ChatScreen) return;

        ChatComponent chatHud = client.gui.getChat();
        if (chatHud != null) {
            chatHud.resetChatScroll();
        }
    }
}
