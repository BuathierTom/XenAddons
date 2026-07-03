package xenus.xenaddons.features.impl.misc;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.utils.DungeonUtils;

import java.nio.DoubleBuffer;

public class Cursor189Module extends Module {
    public final BooleanSetting hardInDungeonsOnly = new BooleanSetting("Hard in dungeons only", true);
    private static double rememberedX = -1;
    private static double rememberedY = -1;
    private static Screen lastScreen = null;
    private static boolean wasCursorVisible = false;
    private static final DoubleBuffer X_POS = BufferUtils.createDoubleBuffer(1);
    private static final DoubleBuffer Y_POS = BufferUtils.createDoubleBuffer(1);

    public Cursor189Module() {
        super("Cursor 1.8.9", "Centers cursor in dungeons and can optionally keep legacy inventory cursor behavior", Category.MISC);
        this.registerSetting(hardInDungeonsOnly);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Screen current = client.screen;

            if (!this.isEnabled()) {
                lastScreen = current;
                wasCursorVisible = false;
                return;
            }

            if (isHardDungeonModeActive(client)) {
                handleHardDungeonCursor(client);
                lastScreen = current;
                return;
            }

            if (isLegacyInventoryCursorActive()) {
                if (lastScreen instanceof InventoryScreen && current == null) {
                    rememberedX = centerX(client);
                    rememberedY = centerY(client);
                }
            } else {
                rememberedX = centerX(client);
                rememberedY = centerY(client);
                wasCursorVisible = false;
            }

            lastScreen = current;
            wasCursorVisible = false;
        });
    }

    public static boolean isFeatureEnabled() {
        Cursor189Module module = ModuleManager.getModule(Cursor189Module.class);
        return module != null && module.isEnabled();
    }

    public static void applyRememberedOnInventoryInit(Minecraft client) {
        if (client == null || client.getWindow() == null || !isFeatureEnabled()) return;
        if (isHardDungeonModeActive(client)) {
            centerCursor(client);
            return;
        }
        if (!isLegacyInventoryCursorActive()) return;

        double targetX = rememberedX >= 0 ? rememberedX : centerX(client);
        double targetY = rememberedY >= 0 ? rememberedY : centerY(client);
        setCursorNormalAt(client, targetX, targetY);
    }

    public static void captureCurrentCursorInInventory(Minecraft client) {
        if (client == null || client.getWindow() == null || !isFeatureEnabled()) return;
        if (isHardDungeonModeActive(client)) return;
        if (!isLegacyInventoryCursorActive()) return;
        if (!(client.screen instanceof InventoryScreen)) return;

        X_POS.rewind();
        Y_POS.rewind();
        GLFW.glfwGetCursorPos(client.getWindow().handle(), X_POS, Y_POS);
        rememberedX = clamp(X_POS.get(0), 0.0, client.getWindow().getScreenWidth() - 1.0);
        rememberedY = clamp(Y_POS.get(0), 0.0, client.getWindow().getScreenHeight() - 1.0);
    }

    @Override
    public void onDisable() {
        lastScreen = mc.screen;
        wasCursorVisible = false;
    }

    private static boolean isHardDungeonModeActive(Minecraft client) {
        Cursor189Module module = ModuleManager.getModule(Cursor189Module.class);
        return module != null
            && module.isEnabled()
            && module.hardInDungeonsOnly.getValue()
            && client != null
            && DungeonUtils.inDungeon();
    }

    private static boolean isLegacyInventoryCursorActive() {
        Cursor189Module module = ModuleManager.getModule(Cursor189Module.class);
        return module != null
            && module.isEnabled()
            && !module.hardInDungeonsOnly.getValue();
    }

    private static void handleHardDungeonCursor(Minecraft client) {
        if (client == null || client.getWindow() == null) return;
        int cursorMode = GLFW.glfwGetInputMode(client.getWindow().handle(), GLFW.GLFW_CURSOR);
        boolean cursorVisible = cursorMode == GLFW.GLFW_CURSOR_NORMAL;
        if (cursorVisible && !wasCursorVisible) {
            centerCursor(client);
        }
        wasCursorVisible = cursorVisible;
    }

    private static void centerCursor(Minecraft client) {
        if (client == null || client.getWindow() == null) return;
        rememberedX = centerX(client);
        rememberedY = centerY(client);
        setCursorNormalAt(client, rememberedX, rememberedY);
    }

    private static void setCursorNormalAt(Minecraft client, double x, double y) {
        long handle = client.getWindow().handle();
        double targetX = clamp(x, 0.0, client.getWindow().getScreenWidth() - 1.0);
        double targetY = clamp(y, 0.0, client.getWindow().getScreenHeight() - 1.0);
        GLFW.glfwSetInputMode(handle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        GLFW.glfwSetCursorPos(handle, targetX, targetY);
    }

    private static double centerX(Minecraft client) {
        return client.getWindow().getScreenWidth() / 2.0;
    }

    private static double centerY(Minecraft client) {
        return client.getWindow().getScreenHeight() / 2.0;
    }

    private static double clamp(double value, double min, double max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }
}
