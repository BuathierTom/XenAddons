package xenus.xenaddons.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ChatUtils {
    public static final String PREFIX = "§8[§6XenAddons§8] ";

    public enum MessageType {
        INFO("§e"),
        SUCCESS("§a"),
        ERROR("§c"),
        WARNING("§6");

        private final String color;

        MessageType(String color) {
            this.color = color;
        }

        public String getColor() {
            return color;
        }
    }

    public static void sendMessage(String message, MessageType type) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            String formatted = PREFIX + type.getColor() + message;
            mc.player.sendSystemMessage(Component.literal(formatted));
        }
    }

    public static void sendActionBar(String message, MessageType type) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            String formatted = PREFIX + type.getColor() + message;
            mc.player.sendOverlayMessage(Component.literal(formatted));
        }
    }

    public static void info(String message) { sendMessage(message, MessageType.INFO); }
    public static void success(String message) { sendMessage(message, MessageType.SUCCESS); }
    public static void error(String message) { sendMessage(message, MessageType.ERROR); }
    public static void warning(String message) { sendMessage(message, MessageType.WARNING); }
    public static void successActionBar(String message) { sendActionBar(message, MessageType.SUCCESS); }
}
