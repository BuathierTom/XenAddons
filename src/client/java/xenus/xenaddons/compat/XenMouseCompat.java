package xenus.xenaddons.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.features.impl.misc.PeekChatController;
import xenus.xenaddons.features.impl.misc.SpotifyOverlayModule;

public final class XenMouseCompat {
    private static double peekChatAccumulatedScroll = 0.0D;

    private XenMouseCompat() {}

    public static boolean handleSpotifyOverlayClick(Minecraft client, long window, MouseButtonInfo input, int action) {
        if (window != client.getWindow().handle()) return false;
        if (action != GLFW.GLFW_PRESS) return false;
        if (client.screen == null || client.getOverlay() != null) return false;
        if ("xenus.xenaddons.gui.HudEditorScreen".equals(client.screen.getClass().getName())) return false;

        SpotifyOverlayModule module = ModuleManager.getModule(SpotifyOverlayModule.class);
        if (module == null) return false;

        double mouseX = client.mouseHandler.getScaledXPos(client.getWindow());
        double mouseY = client.mouseHandler.getScaledYPos(client.getWindow());
        if (!module.onMouseClicked(mouseX, mouseY, input.button())) return false;

        client.getFramerateLimitTracker().onInputReceived();
        return true;
    }

    public static boolean handlePeekChatScroll(Minecraft client, long window, double vertical) {
        if (!PeekChatController.shouldRenderPeek(client)) return false;
        if (window != client.getWindow().handle()) return false;
        if (client.getOverlay() != null) return false;

        client.getFramerateLimitTracker().onInputReceived();

        int scrollAmount = getScrollAmount(client, vertical);
        if (scrollAmount != 0) {
            client.gui.getChat().scrollChat(scrollAmount);
        }

        return true;
    }

    private static int getScrollAmount(Minecraft client, double vertical) {
        boolean discrete = client.options.discreteMouseScroll().get();
        double sensitivity = client.options.mouseWheelSensitivity().get();
        double adjustedVertical = (discrete ? Math.signum(vertical) : vertical) * sensitivity;

        if (peekChatAccumulatedScroll != 0.0D && Math.signum(adjustedVertical) != Math.signum(peekChatAccumulatedScroll)) {
            peekChatAccumulatedScroll = 0.0D;
        }

        peekChatAccumulatedScroll += adjustedVertical;
        double scroll = Mth.clamp(peekChatAccumulatedScroll, -1.0D, 1.0D);
        int scrollAmount = (int) scroll;
        if (scrollAmount == 0) return 0;

        peekChatAccumulatedScroll -= scrollAmount;
        if (!client.hasShiftDown()) {
            scrollAmount *= 7;
        }
        return scrollAmount;
    }
}
