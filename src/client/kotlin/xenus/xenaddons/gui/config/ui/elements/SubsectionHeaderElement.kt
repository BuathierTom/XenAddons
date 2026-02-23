package xenus.xenaddons.gui.config.ui.elements

import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.components.base.VexelElement
import xyz.meowing.vexel.components.core.Text
import xenus.xenaddons.gui.ui.Theme

class SubsectionHeaderElement(
    title: String
) : VexelElement<SubsectionHeaderElement>() {
    init {
        setSizing(240f, Size.Pixels, 22f, Size.Pixels)
        setPositioning(Pos.ParentPixels, Pos.AfterSibling)

        Text(title, Theme.TextMuted.color, 13f)
            .setPositioning(8f, Pos.ParentPixels, 6f, Pos.ParentPixels)
            .childOf(this)
    }

    override fun onRender(mouseX: Float, mouseY: Float) {}
}
