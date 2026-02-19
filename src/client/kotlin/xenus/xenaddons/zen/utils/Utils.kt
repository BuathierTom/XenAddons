package xenus.xenaddons.zen.utils

import java.awt.Color

fun Map<*, *>.toColorFromMap(): Color? {
    val r = (this["r"] as? Number)?.toInt() ?: return null
    val g = (this["g"] as? Number)?.toInt() ?: return null
    val b = (this["b"] as? Number)?.toInt() ?: return null
    val a = (this["a"] as? Number)?.toInt() ?: 255
    return Color(r, g, b, a)
}

fun String.stripColor(): String {
    return replace("(?i)§[0-9A-FK-OR]".toRegex(), "")
}

