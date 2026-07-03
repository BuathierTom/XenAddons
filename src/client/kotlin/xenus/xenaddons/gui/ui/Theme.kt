package xenus.xenaddons.gui.ui

import java.awt.Color

enum class Theme(private val baseColor: Int) {
    BgDark(0xFF090B0D.toInt()),
    Bg(0xFF15181C.toInt()),
    BgLight(0xFF20252B.toInt()),
    Component(0xFFF2F4F7.toInt()),
    TextMuted(0xFFA7B0BC.toInt()),
    Highlight(0xFF62B6CB.toInt()),
    Border(0xFF323943.toInt()),
    BorderMuted(0xFF242A31.toInt()),
    Primary(0xFF55B7C7.toInt()),
    Secondary(0xFF3B7F8C.toInt()),
    Danger(0xFFD06767.toInt()),
    Warning(0xFFD9A94E.toInt()),
    Success(0xFF57A978.toInt()),
    Info(0xFF62B6CB.toInt());

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
