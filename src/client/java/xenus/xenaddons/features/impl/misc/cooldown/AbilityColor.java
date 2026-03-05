package xenus.xenaddons.features.impl.misc.cooldown;

public enum AbilityColor {
    RED("§c", 0xFF5555),
    YELLOW("§e", 0xFFFF55),
    GREEN("§a", 0x55FF55),
    WHITE("§f", 0xFFFFFF),
    BLUE("§9", 0x5555FF),
    DARK_PURPLE("§5", 0xAA00AA),
    LIGHT_PURPLE("§d", 0xFF55FF);

    public final String chatCode;
    public final int rgb;

    AbilityColor(String chatCode, int rgb) {
        this.chatCode = chatCode;
        this.rgb = rgb;
    }
}
