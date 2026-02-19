package xenus.xenaddons.features.impl.misc;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.InputUtil;
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
        super("Cursor 1.8.9", "Keeps cursor position in inventory and resets to center after close", Category.MISC);
        this.registerSetting(hardInDungeonsOnly);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Screen current = client.currentScreen;

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

            if (lastScreen instanceof InventoryScreen && current == null) {
                rememberedX = client.getWindow().getWidth() / 2.0;
                rememberedY = client.getWindow().getHeight() / 2.0;
            }

            lastScreen = current;
            wasCursorVisible = false;
        });
    }

    public static boolean isFeatureEnabled() {
        Cursor189Module module = ModuleManager.getModule(Cursor189Module.class);
        return module != null && module.isEnabled();
    }

    public static void applyRememberedOnInventoryInit(MinecraftClient client) {
        if (client == null || client.getWindow() == null || !isFeatureEnabled()) return;
        if (isHardDungeonModeActive(client)) {
            centerCursor(client);
            return;
        }

        double targetX = rememberedX >= 0 ? rememberedX : (client.getWindow().getWidth() / 2.0);
        double targetY = rememberedY >= 0 ? rememberedY : (client.getWindow().getHeight() / 2.0);
        InputUtil.setCursorParameters(client.getWindow(), InputUtil.GLFW_CURSOR_NORMAL, targetX, targetY);
    }

    public static void captureCurrentCursorInInventory(MinecraftClient client) {
        if (client == null || client.getWindow() == null || !isFeatureEnabled()) return;
        if (isHardDungeonModeActive(client)) return;
        if (!(client.currentScreen instanceof InventoryScreen)) return;

        X_POS.rewind();
        Y_POS.rewind();
        GLFW.glfwGetCursorPos(client.getWindow().getHandle(), X_POS, Y_POS);
        rememberedX = X_POS.get(0);
        rememberedY = Y_POS.get(0);
    }

    @Override
    public void onDisable() {
        lastScreen = mc.currentScreen;
        wasCursorVisible = false;
    }

    private static boolean isHardDungeonModeActive(MinecraftClient client) {
        Cursor189Module module = ModuleManager.getModule(Cursor189Module.class);
        return module != null
            && module.isEnabled()
            && module.hardInDungeonsOnly.getValue()
            && client != null
            && DungeonUtils.inDungeon();
    }

    private static void handleHardDungeonCursor(MinecraftClient client) {
        if (client == null || client.getWindow() == null) return;
        int cursorMode = GLFW.glfwGetInputMode(client.getWindow().getHandle(), GLFW.GLFW_CURSOR);
        boolean cursorVisible = cursorMode == GLFW.GLFW_CURSOR_NORMAL;
        if (cursorVisible && !wasCursorVisible) {
            centerCursor(client);
        }
        wasCursorVisible = cursorVisible;
    }

    private static void centerCursor(MinecraftClient client) {
        if (client == null || client.getWindow() == null) return;
        rememberedX = client.getWindow().getWidth() / 2.0;
        rememberedY = client.getWindow().getHeight() / 2.0;
        InputUtil.setCursorParameters(client.getWindow(), InputUtil.GLFW_CURSOR_NORMAL, rememberedX, rememberedY);
    }
}
