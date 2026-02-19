package xenus.xenaddons.zen.ui

import java.awt.Color

enum class Theme(private val baseColor: Int) {
    BgDark(0xFF0A0F18.toInt()),
    Bg(0xFF111A26.toInt()),
    BgLight(0xFF1A2533.toInt()),
    Text(0xFFE6EAF0.toInt()),
    TextMuted(0xFF9AA6B8.toInt()),
    Highlight(0xFF2F5D9F.toInt()),
    Border(0xFF1F2F46.toInt()),
    BorderMuted(0xFF162335.toInt()),
    Primary(0xFF3A6EA5.toInt()),
    Secondary(0xFF274B7A.toInt()),
    Danger(0xFFD25C5C.toInt()),
    Warning(0xFFE6B65C.toInt()),
    Success(0xFF4FAF83.toInt()),
    Info(0xFF4D8CC9.toInt());

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

        private fun derive(color: Int, factor: Float): Int {
            val c = Color(color, true)
            val r = (c.red * factor).toInt().coerceIn(0, 255)
            val g = (c.green * factor).toInt().coerceIn(0, 255)
            val b = (c.blue * factor).toInt().coerceIn(0, 255)
            return Color(r, g, b, c.alpha).rgb
        }
    }
}
