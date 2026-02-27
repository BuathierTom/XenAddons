package xenus.xenaddons.gui.config.ui.elements

import xyz.meowing.vexel.animations.EasingType
import xyz.meowing.vexel.animations.colorTo
import xyz.meowing.vexel.elements.TextInput
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.components.base.VexelElement
import xenus.xenaddons.gui.config.ui.panels.Panel
import xenus.xenaddons.gui.ui.Theme
import java.awt.Color

class TextInputElement(
    name: String,
    initialValue: String,
    private val placeholder: String
) : VexelElement<TextInputElement>() {
    private val scale = Panel.WIDTH / 240f
    private val hoverEnterColor = Color(Theme.BgLight.color).brighter().rgb

    private val label = Text(name, Theme.Text.color, 16f * scale)
        .setPositioning(6f * scale, Pos.ParentPixels, 6f * scale, Pos.ParentPixels)
        .childOf(this)

    val input = TextInput(
        initialValue = initialValue,
        placeholder = placeholder,
        backgroundColor = Theme.BgLight.color,
        hoverColor = null,
        borderColor = Theme.Border.color
    )
        .setSizing(230f * scale, Size.Pixels, 0f, Size.Auto)
        .setPositioning(10f * scale, Pos.ParentCenter, 28f * scale, Pos.ParentPixels)
        .childOf(this)

    init {
        setSizing(Panel.WIDTH, Size.Pixels, 58f * scale, Size.Pixels)
        setPositioning(Pos.ParentPixels, Pos.AfterSibling)

        input
            .onHover(
                { _, _ -> input.background.colorTo(hoverEnterColor, 200, EasingType.EASE_IN) },
                { _, _ -> input.background.colorTo(Theme.BgLight.color, 200, EasingType.EASE_IN) }
            )
    }

    override fun onRender(mouseX: Float, mouseY: Float) {}
}
