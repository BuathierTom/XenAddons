package xenus.xenaddons.features.impl.misc;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.KeybindSetting;

public class PeekChatModule extends Module {
    public final KeybindSetting peekKey = new KeybindSetting("Peek Key", -1);

    private boolean wasPeeking = false;

    public PeekChatModule() {
        super("Peek Chat", "Hold a key to temporarily open the chat view and scroll it", Category.MISC);
        this.registerSetting(peekKey);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean peeking = shouldPeek(client);
            PeekChatController.setPeekHeld(peeking);
            if (!peeking && wasPeeking) {
                PeekChatController.stop(client);
            }
            wasPeeking = peeking;
        });
    }

    @Override
    public void onDisable() {
        wasPeeking = false;
        PeekChatController.stop(mc);
    }

    private boolean shouldPeek(Minecraft client) {
        if (client == null || !this.isEnabled()) return false;
        if (client.player == null || client.gui == null || client.screen != null) return false;
        return isKeyPressed(peekKey.getKey());
    }

    private boolean isKeyPressed(int key) {
        if (key == -1 || mc == null || mc.getWindow() == null) return false;
        long handle = mc.getWindow().handle();
        try {
            if (key < 0) {
                int button = -100 - key;
                return button >= 0 && GLFW.glfwGetMouseButton(handle, button) == GLFW.GLFW_PRESS;
            }
            return GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
        } catch (Exception ignored) {
            return false;
        }
    }
}
