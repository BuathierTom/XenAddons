package xenus.xenaddons.gui.ui

import java.awt.Color

enum class Theme(private val baseColor: Int) {
    BgDark(0xFF101010.toInt()),
    Bg(0xFF1A1A1A.toInt()),
    BgLight(0xFF262626.toInt()),
    Text(0xFFF7F8FA.toInt()),
    TextMuted(0xFFB5BDC8.toInt()),
    Highlight(0xFF4C8FD8.toInt()),
    Border(0xFF3B3B3B.toInt()),
    BorderMuted(0xFF232323.toInt()),
    Primary(0xFF3296DC.toInt()),
    Secondary(0xFF2A6FA0.toInt()),
    Danger(0xFFD66F6F.toInt()),
    Warning(0xFFE2B45B.toInt()),
    Success(0xFF4FAA82.toInt()),
    Info(0xFF4C8FD8.toInt());

    val color: Int
        get() = when (this) {
            Primary -> menuPrimary ?: baseColor
            Secondary -> derive(menuPrimary ?: baseColor, 0.75f)
            Highlight -> derive(menuPrimary ?: baseColor, 1.15f)
            else -> baseColor
        }

    fun withAlpha(alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
        return (color and 0x00FFFFFF) or (a shl 24)
    }

    companion object {
        private var menuPrimary: Int? = null

        @JvmStatic
        fun setMenuPrimary(color: Int?) {
            menuPrimary = color
        }

        fun shift(color: Int, factor: Float): Int = derive(color, factor)

        private fun derive(color: Int, factor: Float): Int {
            val c = Color(color, true)
            val r = (c.red * factor).toInt().coerceIn(0, 255)
            val g = (c.green * factor).toInt().coerceIn(0, 255)
            val b = (c.blue * factor).toInt().coerceIn(0, 255)
            return Color(r, g, b, c.alpha).rgb
        }
    }
}
