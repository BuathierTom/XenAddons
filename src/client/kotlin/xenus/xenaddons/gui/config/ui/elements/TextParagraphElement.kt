package xenus.xenaddons.gui.config.ui.elements

import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.components.base.VexelElement
import xyz.meowing.vexel.utils.render.NVGRenderer
import xenus.xenaddons.gui.config.ui.panels.Panel
import xenus.xenaddons.gui.ui.Theme

class TextParagraphElement(
    text: String
) : VexelElement<TextParagraphElement>() {
    private val scale = Panel.WIDTH / 240f
    private val textSize = 14f * scale
    private val contentWidth = Panel.WIDTH - (12f * scale)

    init {
        val bounds = NVGRenderer.wrappedTextBounds(text, contentWidth, textSize, NVGRenderer.defaultFont)
        val textHeight = bounds[3] - bounds[1]
        setSizing(Panel.WIDTH, Size.Pixels, textHeight + (16f * scale), Size.Pixels)
        setPositioning(Pos.ParentPixels, Pos.AfterSibling)
        Text(text, Theme.TextMuted.color, textSize)
            .setPositioning(6f * scale, Pos.ParentPixels, 8f * scale, Pos.ParentPixels)
            .childOf(this)
    }

    override fun onRender(mouseX: Float, mouseY: Float) {
        children.firstOrNull()?.let { textElement ->
            if (textElement is Text) {
                NVGRenderer.drawWrappedString(
                    textElement.text,
                    x + (6f * scale),
                    y + (8f * scale),
                    contentWidth,
                    textSize,
                    Theme.TextMuted.color,
                    NVGRenderer.defaultFont
                )
            }
        }
    }
}
