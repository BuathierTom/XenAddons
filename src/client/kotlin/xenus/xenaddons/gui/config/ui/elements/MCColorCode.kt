package xenus.xenaddons.gui.config.ui.elements

import java.awt.Color

enum class MCColorCode(val code: String, val color: Color, val displayName: String) {
    BLACK("§0", Color(0, 0, 0), "Black"),
    DARK_BLUE("§1", Color(0, 0, 170), "Dark Blue"),
    DARK_GREEN("§2", Color(0, 170, 0), "Dark Green"),
    DARK_AQUA("§3", Color(0, 170, 170), "Dark Aqua"),
    DARK_RED("§4", Color(170, 0, 0), "Dark Red"),
    DARK_PURPLE("§5", Color(170, 0, 170), "Dark Purple"),
    GOLD("§6", Color(255, 170, 0), "Gold"),
    GRAY("§7", Color(170, 170, 170), "Gray"),
    DARK_GRAY("§8", Color(85, 85, 85), "Dark Gray"),
    BLUE("§9", Color(85, 85, 255), "Blue"),
    GREEN("§a", Color(85, 255, 85), "Green"),
    AQUA("§b", Color(85, 255, 255), "Aqua"),
    RED("§c", Color(255, 85, 85), "Red"),
    LIGHT_PURPLE("§d", Color(255, 85, 255), "Light Purple"),
    YELLOW("§e", Color(255, 255, 85), "Yellow"),
    WHITE("§f", Color(255, 255, 255), "White")
}
