package xenus.xenaddons.gui.config.ui.elements

import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.components.base.VexelElement
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xenus.xenaddons.gui.ui.Theme

class SubsectionHeaderElement(
    title: String
) : VexelElement<SubsectionHeaderElement>() {
    init {
        setSizing(240f, Size.Pixels, 24f, Size.Pixels)
        setPositioning(Pos.ParentPixels, Pos.AfterSibling)

        Rectangle(Theme.BgDark.withAlpha(0.65f), 0x00000000, 0f, 0f)
            .setSizing(240f, Size.Pixels, 1f, Size.Pixels)
            .setPositioning(0f, Pos.ParentPixels, 3f, Pos.ParentPixels)
            .childOf(this)

        Text(title, Theme.TextMuted.color, 13f)
            .setPositioning(8f, Pos.ParentPixels, 8f, Pos.ParentPixels)
            .childOf(this)
    }

    override fun onRender(mouseX: Float, mouseY: Float) {}
}
