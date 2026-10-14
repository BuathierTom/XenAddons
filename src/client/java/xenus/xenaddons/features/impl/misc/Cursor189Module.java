package xenus.xenaddons.features.impl.misc;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import xenus.xenaddons.compat.XenInputCompat;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.utils.DungeonUtils;

public class Cursor189Module extends Module {
    public final BooleanSetting hardInDungeonsOnly = new BooleanSetting("Hard in dungeons only", true);
    private static double rememberedX = -1;
    private static double rememberedY = -1;
    private static Screen lastScreen = null;
    private static boolean wasCursorVisible = false;

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

        double[] cursor = XenInputCompat.cursorPos();
        rememberedX = clamp(cursor[0], 0.0, client.getWindow().getScreenWidth() - 1.0);
        rememberedY = clamp(cursor[1], 0.0, client.getWindow().getScreenHeight() - 1.0);
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
        boolean cursorVisible = XenInputCompat.isCursorVisible();
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
        double targetX = clamp(x, 0.0, client.getWindow().getScreenWidth() - 1.0);
        double targetY = clamp(y, 0.0, client.getWindow().getScreenHeight() - 1.0);
        XenInputCompat.showCursorAt(targetX, targetY);
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
