package xenus.xenaddons.gui.config.ui.elements

import xyz.meowing.vexel.animations.EasingType
import xyz.meowing.vexel.animations.colorTo
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.components.base.VexelElement
import xenus.xenaddons.gui.config.ui.panels.Panel
import xenus.xenaddons.gui.ui.Theme

class ButtonElement(
    text: String,
    private val onClick: () -> Unit
) : VexelElement<ButtonElement>() {
    private val scale = Panel.WIDTH / 240f

    private val button = Rectangle(
        Theme.Bg.color,
        Theme.Border.color,
        5f * scale,
        1f,
        floatArrayOf(8f * scale, 16f * scale, 8f * scale, 16f * scale)
    )
        .setSizing(Panel.WIDTH - (12f * scale), Size.Pixels, 0f, Size.Auto)
        .setPositioning(6f * scale, Pos.ParentPixels, 0f, Pos.ParentCenter)
        .childOf(this)

    private val buttonText = Text(text, Theme.Text.color, 16f * scale)
        .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentCenter)
        .childOf(button)

    init {
        setSizing(Panel.WIDTH, Size.Pixels, 40f * scale, Size.Pixels)
        setPositioning(Pos.ParentPixels, Pos.AfterSibling)

        button.onHover(
            { _, _ -> button.colorTo(Theme.BgLight.color, 150, EasingType.EASE_OUT) },
            { _, _ -> button.colorTo(Theme.Bg.color, 150, EasingType.EASE_IN) }
        )

        button.onClick { _, _, btn ->
            if (btn == 0) {
                button.colorTo(Theme.Highlight.withAlpha(0.8f), 100, EasingType.EASE_OUT) {
                    button.colorTo(Theme.BgLight.color, 100, EasingType.EASE_IN)
                }
                onClick()
                true
            } else false
        }
    }

    override fun onRender(mouseX: Float, mouseY: Float) {}
}
