package xenus.xenaddons.features.impl.misc;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;

public class Cursor189Module extends Module {
    private Screen lastScreen = null;

    public Cursor189Module() {
        super("Cursor 1.8.9", "Recenter cursor on crosshair when opening inventories/containers", Category.MISC);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Screen current = client.currentScreen;

            if (!this.isEnabled()) {
                lastScreen = current;
                return;
            }

            if (current != null && current != lastScreen) {
                long handle = client.getWindow().getHandle();
                double centerX = client.getWindow().getWidth() / 2.0;
                double centerY = client.getWindow().getHeight() / 2.0;
                GLFW.glfwSetCursorPos(handle, centerX, centerY);
            }

            lastScreen = current;
        });
    }

    @Override
    public void onDisable() {
        lastScreen = mc.currentScreen;
    }
}
