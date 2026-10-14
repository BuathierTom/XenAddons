package xenus.xenaddons.compat;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
//? if <26.3 {
import org.lwjgl.glfw.GLFW;
//?} else {
/*import java.nio.FloatBuffer;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.system.MemoryStack;
*///?}

public final class XenInputCompat {
    private XenInputCompat() {}

    public static boolean isKeyDown(int key) {
        Window window = window();
        if (window == null || key < 0) return false;
        try {
            //? if <26.3 {
            return InputConstants.isKeyDown(window, key);
            //?} else {
            /*return InputConstants.isKeyDown(key);
            *///?}
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static boolean isMouseButtonDown(int button) {
        Window window = window();
        if (window == null || button < 0) return false;
        //? if <26.3 {
        return GLFW.glfwGetMouseButton(window.handle(), button) == GLFW.GLFW_PRESS;
        //?} else {
        /*int mask = 1 << (toSdlButton(button) - 1);
        return (SDLMouse.SDL_GetMouseState((FloatBuffer) null, (FloatBuffer) null) & mask) != 0;
        *///?}
    }

    public static int button(int rawButton) {
        //? if <26.3 {
        return rawButton;
        //?} else {
        /*return switch (rawButton) {
            case 1 -> 0;
            case 2 -> 2;
            case 3 -> 1;
            default -> rawButton - 1;
        };
        *///?}
    }

    public static double[] cursorPos() {
        Window window = window();
        if (window == null) return new double[] {0.0, 0.0};
        //? if <26.3 {
        double[] x = new double[1];
        double[] y = new double[1];
        GLFW.glfwGetCursorPos(window.handle(), x, y);
        return new double[] {x[0], y[0]};
        //?} else {
        /*try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer x = stack.mallocFloat(1);
            FloatBuffer y = stack.mallocFloat(1);
            SDLMouse.SDL_GetMouseState(x, y);
            return new double[] {x.get(0), y.get(0)};
        }
        *///?}
    }

    public static boolean isCursorVisible() {
        Window window = window();
        if (window == null) return false;
        //? if <26.3 {
        return GLFW.glfwGetInputMode(window.handle(), GLFW.GLFW_CURSOR) == GLFW.GLFW_CURSOR_NORMAL;
        //?} else {
        /*return !SDLMouse.SDL_GetWindowRelativeMouseMode(window.handle());
        *///?}
    }

    public static void showCursorAt(double x, double y) {
        Window window = window();
        if (window == null) return;
        //? if <26.3 {
        GLFW.glfwSetInputMode(window.handle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        GLFW.glfwSetCursorPos(window.handle(), x, y);
        //?} else {
        /*InputConstants.releaseMouse(window, x, y);
        *///?}
    }

    //? if >=26.3 {
    /*private static int toSdlButton(int button) {
        return switch (button) {
            case 0 -> 1;
            case 1 -> 3;
            case 2 -> 2;
            default -> button + 1;
        };
    }
    *///?}

    private static Window window() {
        Minecraft client = Minecraft.getInstance();
        return client == null ? null : client.getWindow();
    }
}
