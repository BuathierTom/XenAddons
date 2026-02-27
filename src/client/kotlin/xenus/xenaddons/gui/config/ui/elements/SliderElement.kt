package xenus.xenaddons.gui.config.ui.elements

import xyz.meowing.knit.api.input.KnitMouseButtons
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.components.base.VexelElement
import xyz.meowing.vexel.elements.Slider
import xenus.xenaddons.gui.config.ui.panels.Panel
import xenus.xenaddons.gui.ui.Theme

class SliderElement(
    name: String,
    initialValue: Double,
    private val min: Double,
    private val max: Double,
    private val showDouble: Boolean
) : VexelElement<SliderElement>() {
    private val scale = Panel.WIDTH / 240f

    private val label = Text(name, Theme.Text.color, 16f * scale)
        .setPositioning(6f, Pos.ParentPixels, 8f, Pos.ParentPixels)
        .childOf(this)

    private val valueText = Text(formatValue(initialValue), Theme.TextMuted.color, 16f * scale)
        .setPositioning(-6f, Pos.ParentPixels, 8f, Pos.ParentPixels)
        .alignRight()
        .childOf(this)

    val slider = Slider(
        value = ((initialValue - min) / (max - min)).toFloat(),
        minValue = 0f,
        maxValue = 1f,
        thumbColor = 0xFFFFFFFF.toInt(),
        thumbWidth = 15f * scale,
        thumbHeight = 15f * scale,
        trackColor = Theme.Bg.color,
        trackFillColor = Theme.Primary.color
    )
        .setSizing(220f * scale, Size.Pixels, 24f * scale, Size.Pixels)
        .setPositioning(10f * scale, Pos.ParentPixels, 0f, Pos.ParentPixels)
        .alignBottom()
        .childOf(this)

    init {
        setSizing(Panel.WIDTH, Size.Pixels, 48f * scale, Size.Pixels)
        setPositioning(Pos.ParentPixels, Pos.AfterSibling)

        slider.onValueChange { sliderValue ->
            val actualValue = min + (sliderValue as Float) * (max - min)
            valueText.text = formatValue(actualValue)
        }
    }

    private fun formatValue(value: Double): String =
        if (showDouble) String.format("%.1f", value)
        else value.toInt().toString()

    override fun onRender(mouseX: Float, mouseY: Float) {
        if (slider.isDragging && !KnitMouseButtons.LEFT.isPressed) {
            slider.isDragging = false
        }
    }
}
