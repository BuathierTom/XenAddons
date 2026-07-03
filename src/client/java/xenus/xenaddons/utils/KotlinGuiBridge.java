package xenus.xenaddons.utils;

import net.minecraft.client.gui.screens.Screen;
import xenus.xenaddons.XenAddons;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class KotlinGuiBridge {
    private KotlinGuiBridge() {}

    public static void applyMenuPrimary(int color) {
        try {
            Class<?> themeClass = Class.forName("xenus.xenaddons.gui.ui.Theme");
            Method method;
            try {
                method = themeClass.getMethod("setMenuPrimary", Integer.class);
            } catch (NoSuchMethodException ignored) {
                method = themeClass.getMethod("setMenuPrimary", int.class);
            }
            method.invoke(null, Integer.valueOf(color));
        } catch (Throwable t) {
            XenAddons.LOGGER.warn("Failed to apply menu color through Kotlin bridge", t);
        }
    }

    public static boolean isClickGuiInstance(Screen screen) {
        if (screen == null) return false;
        try {
            Class<?> clickGuiClass = Class.forName("xenus.xenaddons.gui.config.ui.ClickGUI");
            Field instanceField = clickGuiClass.getField("INSTANCE");
            Object instance = instanceField.get(null);
            return screen == instance;
        } catch (Throwable t) {
            XenAddons.LOGGER.debug("Failed to resolve ClickGUI instance through Kotlin bridge", t);
            return false;
        }
    }

    public static void openConfigScreen() {
        try {
            Class<?> configManagerClass = Class.forName("xenus.xenaddons.gui.managers.config.ConfigManager");
            Method method = configManagerClass.getMethod("openConfig");
            method.invoke(null);
        } catch (Throwable t) {
            XenAddons.LOGGER.warn("Failed to open config screen through Kotlin bridge", t);
        }
    }

    public static Screen createHudEditorScreen(Screen parent) {
        try {
            Class<?> hudEditorClass = Class.forName("xenus.xenaddons.gui.HudEditorScreen");
            Constructor<?> ctor = hudEditorClass.getConstructor(Screen.class);
            Object instance = ctor.newInstance(parent);
            if (instance instanceof Screen screen) return screen;
        } catch (Throwable t) {
            XenAddons.LOGGER.warn("Failed to create HUD editor screen through Kotlin bridge", t);
        }
        return null;
    }
}
