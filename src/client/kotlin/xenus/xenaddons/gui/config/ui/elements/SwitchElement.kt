package xenus.xenaddons.gui.config.ui.elements

import xyz.meowing.vexel.elements.Switch
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.components.base.VexelElement
import xenus.xenaddons.gui.config.ui.panels.Panel
import xenus.xenaddons.gui.ui.Theme

class SwitchElement(
    name: String,
    initialValue: Boolean
) : VexelElement<SwitchElement>() {
    private val scale = Panel.WIDTH / 240f

    private val label = Text(name, Theme.Text.color, 16f * scale)
        .setPositioning(6f, Pos.ParentPixels, 0f, Pos.ParentCenter)
        .childOf(this)

    val switch = Switch(
        thumbColor = 0xFFFFFFFF.toInt(),
        trackEnabledColor = Theme.Success.color,
        trackDisabledColor = Theme.BgDark.color,
        thumbRadius = 5f * scale,
        borderRadius = 5f * scale,
        thumbWidth = 12f * scale,
        thumbHeight = 12f * scale,
        padding = floatArrayOf(0f, 0f, 0f, 0f)
    )
        .setSizing(40f * scale, Size.Pixels, 16f * scale, Size.Pixels)
        .setPositioning(-5f, Pos.ParentPixels, 0f, Pos.ParentCenter)
        .alignRight()
        .childOf(this)

    init {
        setSizing(Panel.WIDTH, Size.Pixels, Panel.HEADER_HEIGHT, Size.Pixels)
        setPositioning(Pos.ParentPixels, Pos.AfterSibling)
        switch.setEnabled(initialValue, animated = false, silent = true)
    }

    override fun onRender(mouseX: Float, mouseY: Float) {}
}
