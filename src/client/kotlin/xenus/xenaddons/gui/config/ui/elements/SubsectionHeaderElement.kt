package xenus.xenaddons.gui.config.ui.elements

import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.components.base.VexelElement
import xyz.meowing.vexel.components.core.Text
import xenus.xenaddons.gui.config.ui.panels.Panel
import xenus.xenaddons.gui.ui.Theme

class SubsectionHeaderElement(
    title: String
) : VexelElement<SubsectionHeaderElement>() {
    private val scale = Panel.WIDTH / 240f

    init {
        setSizing(Panel.WIDTH, Size.Pixels, 22f * scale, Size.Pixels)
        setPositioning(Pos.ParentPixels, Pos.AfterSibling)

        Text(title, Theme.TextMuted.color, 13f * scale)
            .setPositioning(8f * scale, Pos.ParentPixels, 6f * scale, Pos.ParentPixels)
            .childOf(this)
    }

    override fun onRender(mouseX: Float, mouseY: Float) {}
}
