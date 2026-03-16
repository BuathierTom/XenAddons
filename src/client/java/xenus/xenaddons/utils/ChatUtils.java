package xenus.xenaddons.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

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
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            String formatted = PREFIX + type.getColor() + message;
            mc.player.sendMessage(Text.of(formatted), false);
        }
    }

    public static void sendActionBar(String message, MessageType type) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            String formatted = PREFIX + type.getColor() + message;
            mc.player.sendMessage(Text.of(formatted), true);
        }
    }

    public static void info(String message) { sendMessage(message, MessageType.INFO); }
    public static void success(String message) { sendMessage(message, MessageType.SUCCESS); }
    public static void error(String message) { sendMessage(message, MessageType.ERROR); }
    public static void warning(String message) { sendMessage(message, MessageType.WARNING); }
    public static void successActionBar(String message) { sendActionBar(message, MessageType.SUCCESS); }
}
