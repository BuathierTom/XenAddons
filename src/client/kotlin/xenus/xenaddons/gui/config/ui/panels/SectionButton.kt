package xenus.xenaddons.gui.config.ui.panels

import xyz.meowing.vexel.animations.EasingType
import xyz.meowing.vexel.animations.animateSize
import xyz.meowing.vexel.animations.colorTo
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.SvgImage
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.components.base.VexelElement
import xenus.xenaddons.gui.managers.config.FeatureElement
import xenus.xenaddons.gui.managers.config.ConfigManager
import xenus.xenaddons.gui.ui.Theme
import xenus.xenaddons.gui.config.ui.elements.base.ElementRenderer
import xenus.xenaddons.gui.config.ui.elements.SubsectionHeaderElement
import xenus.xenaddons.gui.config.ui.ClickGUI
import java.awt.Color
import kotlin.math.absoluteValue

class SectionButton(
    val feature: FeatureElement,
    container: VexelElement<*>,
    private val onConfigUpdate: (String, Any) -> Unit,
    val isLast: Boolean
) : VexelElement<SectionButton>() {
    private val rowHeight = Panel.HEADER_HEIGHT

    private var extended = false
    private var isAnimating = false
    private var isEnabled = false
    private val elementRenderers = mutableListOf<ElementRenderer>()
    private val optionElements = mutableListOf<VexelElement<*>>()
    private val conditionalElements = mutableListOf<Pair<ElementRenderer, VexelElement<*>>>()
    private val sectionHeaders = mutableMapOf<String, VexelElement<*>>()
    private val sectionChildren = mutableMapOf<String, MutableList<VexelElement<*>>>()

    private val buttonBackground = Rectangle(Theme.BgLight.color, 0x00000000, 0f, 0f)
        .setSizing(Panel.WIDTH, Size.Pixels, rowHeight, Size.Pixels)
        .childOf(this)

    val titleText = Text(feature.featureName, Theme.Text.color, 16f * (Panel.WIDTH / 240f))
        .setPositioning(4f, Pos.ParentPixels, 0f, Pos.ParentCenter)
        .childOf(buttonBackground)

    private val chevron = SvgImage(
        svgPath = "/assets/vexel/dropdown.svg",
        color = Color(Theme.TextMuted.color)
    )
        .setSizing(16f * (Panel.WIDTH / 240f), Size.Pixels, 16f * (Panel.WIDTH / 240f), Size.Pixels)
        .setPositioning(-4f * (Panel.WIDTH / 240f), Pos.ParentPixels, 0f, Pos.ParentCenter)
        .alignRight()
        .childOf(buttonBackground)

    private val optionsContainer = Rectangle(0x00000000, 0x00000000)
        .setSizing(Panel.WIDTH, Size.Pixels, 0f, Size.Pixels)
        .setPositioning(0f, Pos.ParentPixels, rowHeight, Pos.ParentPixels)
        .childOf(this)

    init {
        setSizing(Panel.WIDTH, Size.Pixels, rowHeight, Size.Pixels)
        setPositioning(0f, Pos.ParentPixels, 0f, Pos.AfterSibling)
        setRadius()
        childOf(container)

        chevron.visible = feature.options.values.flatten().isNotEmpty()

        isEnabled = ConfigManager.getConfigValue(feature.configElement.configKey) as? Boolean ?: false
        val targetColor = if (isEnabled) Theme.Primary.color else Theme.Bg.color
        buttonBackground.backgroundColor = targetColor

        buttonBackground.onHover(
            { _, _ ->
                val hoverColor = if (isEnabled) {
                    Theme.Primary.withAlpha(0.78f)
                } else {
                    Color(Theme.Bg.color).brighter().rgb
                }
                buttonBackground.colorTo(hoverColor, 150, EasingType.EASE_OUT)
                ClickGUI.updateTooltip(feature.description)
            },
            { _, _ ->
                val baseColor = if (isEnabled) Theme.Primary.color else Theme.Bg.color
                buttonBackground.colorTo(baseColor, 150, EasingType.EASE_IN)

                if (feature.description == ClickGUI.featureTooltip.currentText) { // two can be hovered at the same time, and i like it that way (please fix if you read this)
                    ClickGUI.updateTooltip("")
                }
            }
        )

        buttonBackground.onClick { _, _, button ->
            when (button) {
                0 -> {
                    val pressColor = if (isEnabled) {
                        Theme.Primary.withAlpha(0.92f)
                    } else {
                        Color(Theme.Bg.color).darker().rgb
                    }
                    buttonBackground.colorTo(pressColor, 100, EasingType.EASE_OUT) {
                        val baseColor = if (isEnabled) Theme.Primary.color else Theme.Bg.color
                        buttonBackground.colorTo(baseColor, 100, EasingType.EASE_OUT)
                    }
                    toggleEnabled()
                    true
                }
                1 -> {
                    if (optionElements.isNotEmpty() && !isAnimating) {
                        toggleExpanded()
                    }
                    true
                }
                else -> false
            }
        }

        feature.options.forEach { (sectionName, options) ->
            if (!sectionName.equals("Options", ignoreCase = true)) {
                val header = SubsectionHeaderElement(sectionName)
                header.visible = false
                header.childOf(optionsContainer)
                optionElements.add(header)
                sectionHeaders[sectionName] = header
            }

            options.forEach { option ->
                val renderer = ElementRenderer(option.configElement, onConfigUpdate)
                val element = renderer.createAndAttach(optionsContainer)
                element?.visible = false
                if (element != null) {
                    optionElements.add(element)
                    conditionalElements.add(renderer to element)
                    sectionChildren.getOrPut(sectionName) { mutableListOf() }.add(element)
                    elementRenderers.add(renderer)
                }
            } 
        }

        optionsContainer.visible = false
    }

    private fun toggleEnabled() {
        isEnabled = !isEnabled
        onConfigUpdate(feature.configElement.configKey, isEnabled)
        if (extended) {
            syncConditionalVisibility()
            recalculateHeight()
        }
    }

    private fun toggleExpanded() {
        extended = !extended
        isAnimating = true

        val targetRotation = if (extended) -90f else 0f
        chevron.rotateTo(targetRotation, 200, EasingType.EASE_OUT)

        if (extended) {
            optionsContainer.visible = true
            optionElements.forEach { it.visible = true }
            syncConditionalVisibility()

            val targetHeight = rowHeight + calculateOptionsHeight()
            animateSize(Panel.WIDTH, targetHeight, 200, EasingType.EASE_OUT) {
                isAnimating = false
                parent?.let { p ->
                    if (p is VexelElement<*>) {
                        p.cache.invalidatePosition()
                        p.invalidateChildrenPositions()
                    }
                }
            }
        } else {
            animateSize(Panel.WIDTH, rowHeight, 100, EasingType.EASE_IN) {
                optionsContainer.visible = false
                optionElements.forEach { it.visible = false }
                isAnimating = false
                parent?.let { p ->
                    if (p is VexelElement<*>) {
                        p.cache.invalidatePosition()
                        p.invalidateChildrenPositions()
                    }
                }
            }
        }

        setRadius()
    }

    private fun calculateOptionsHeight(): Float {
        return optionElements.sumOf {
            if (it.visible) it.height.toDouble() else 0.0
        }.toFloat()
    }

    private fun syncConditionalVisibility() {
        conditionalElements.forEach { (renderer, element) ->
            element.visible = renderer.shouldShow()
        }
        sectionHeaders.forEach { (sectionName, header) ->
            val children = sectionChildren[sectionName].orEmpty()
            header.visible = children.any { it.visible }
        }
    }

    fun updateVisibility(panelExtended: Boolean) {
        visible = panelExtended
    }

    fun recalculateHeight() {
        if (!extended || isAnimating) return

        val newHeight = rowHeight + calculateOptionsHeight()
        if ((newHeight - height).absoluteValue > 1f) {
            isAnimating = true
            animateSize(Panel.WIDTH, newHeight, 200, EasingType.EASE_OUT) {
                isAnimating = false
                parent?.let { p ->
                    if (p is VexelElement<*>) {
                        p.cache.invalidatePosition()
                        p.invalidateChildrenPositions()
                    }
                }
            }
        }
    }

    fun matchesSearch(query: String): Boolean {
        if (feature.featureName.contains(query, ignoreCase = true)) return true
        return feature.options.values.flatten().any {
            it.optionName.contains(query, ignoreCase = true)
        }
    }

    private fun setRadius() {
        val r = 5f * (Panel.WIDTH / 240f)
        val bottomLeftRadius = if (isLast && !extended) r else 0f
        val bottomRightRadius = if (isLast && !extended) r else 0f
        buttonBackground.borderRadiusVarying(topRight = 0f, topLeft = 0f, bottomRight = bottomRightRadius, bottomLeft = bottomLeftRadius)
    }

    override fun onRender(mouseX: Float, mouseY: Float) {}
}
