package xenus.xenaddons.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.MouseInput;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.features.impl.misc.PeekChatController;
import xenus.xenaddons.features.impl.misc.SpotifyOverlayModule;

public final class XenMouseCompat {
    private XenMouseCompat() {}

    public static boolean handleSpotifyOverlayClick(MinecraftClient client, long window, MouseInput input, int action) {
        if (window != client.getWindow().getHandle()) return false;
        if (action != GLFW.GLFW_PRESS) return false;
        if (client.currentScreen == null || client.getOverlay() != null) return false;
        if ("xenus.xenaddons.gui.HudEditorScreen".equals(client.currentScreen.getClass().getName())) return false;

        SpotifyOverlayModule module = ModuleManager.getModule(SpotifyOverlayModule.class);
        if (module == null) return false;

        double mouseX = client.mouse.getScaledX(client.getWindow());
        double mouseY = client.mouse.getScaledY(client.getWindow());
        if (!module.onMouseClicked(mouseX, mouseY, input.button())) return false;

        client.getInactivityFpsLimiter().onInput();
        return true;
    }

    public static boolean handlePeekChatScroll(MinecraftClient client, long window, double vertical) {
        if (!PeekChatController.shouldRenderPeek(client)) return false;
        if (window != client.getWindow().getHandle()) return false;
        if (client.getOverlay() != null) return false;

        client.getInactivityFpsLimiter().onInput();

        int scrollAmount = getScrollAmount(client, vertical);
        if (scrollAmount != 0) {
            client.inGameHud.getChatHud().scroll(scrollAmount);
        }

        return true;
    }

    private static int getScrollAmount(MinecraftClient client, double vertical) {
        boolean discrete = client.options.getDiscreteMouseScroll().getValue();
        double sensitivity = client.options.getMouseWheelSensitivity().getValue();
        double adjustedVertical = (discrete ? Math.signum(vertical) : vertical) * sensitivity;
        adjustedVertical = MathHelper.clamp(adjustedVertical, -1.0D, 1.0D);
        if (!client.isShiftPressed()) {
            adjustedVertical *= 7.0D;
        }
        return (int) adjustedVertical;
    }
}
