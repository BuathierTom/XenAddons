package xenus.xenaddons.gui.config.ui.elements

import xyz.meowing.vexel.animations.EasingType
import xyz.meowing.vexel.animations.animateSize
import xyz.meowing.vexel.animations.colorTo
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.components.base.VexelElement
import xenus.xenaddons.gui.config.ui.elements.utils.fadeIn
import xenus.xenaddons.gui.config.ui.elements.utils.fadeOut
import xenus.xenaddons.gui.config.ui.panels.Panel
import xenus.xenaddons.gui.ui.Theme
import xenus.xenaddons.gui.config.ui.panels.SectionButton

class MultiCheckboxElement(
    name: String,
    private val options: List<String>,
    selectedIndices: Set<Int>
) : VexelElement<MultiCheckboxElement>() {
    private val scale = Panel.WIDTH / 240f
    private val rowHeight = Panel.HEADER_HEIGHT
    private val optionHeight = 26f * scale
    private val containerWidth = Panel.WIDTH - (12f * scale)

    companion object {
        var openMultiCheckbox: MultiCheckboxElement? = null
        fun closeAllMultiCheckboxes() = openMultiCheckbox?.collapse()
    }

    private var expanded = false
    private var isAnimating = false
    var selectedIndices = selectedIndices.toMutableSet()
        private set

    private val label = Text(name, Theme.Text.color, 16f * scale)
        .setPositioning(6f * scale, Pos.ParentPixels, 8f * scale, Pos.ParentPixels)
        .childOf(this)

    private val selectedButton = Rectangle(Theme.BgLight.color, Theme.Border.color, 5f, 1f)
        .setPositioning(-6f * scale, Pos.ParentPixels, 0f, Pos.MatchSibling)
        .alignRight()
        .setOffset(0f, -2f)
        .childOf(this)

    private val selectedText = Text("${selectedIndices.size} selected", Theme.Text.color, 16f * scale)
        .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentCenter)
        .childOf(selectedButton)

    private val optionsContainer = Rectangle(Theme.BgLight.color, 0x00000000, 5f, 0f)
        .setSizing(containerWidth, Size.Pixels, 0f, Size.Pixels)
        .setPositioning(6f * scale, Pos.ParentPixels, rowHeight, Pos.ParentPixels)
        .childOf(this)

    init {
        setSizing(Panel.WIDTH, Size.Pixels, rowHeight, Size.Pixels)
        setPositioning(Pos.ParentPixels, Pos.AfterSibling)

        updateButtonSize()

        selectedButton.onHover(
            { _, _ -> selectedButton.colorTo(Theme.Highlight.color, 150, EasingType.EASE_OUT) },
            { _, _ -> selectedButton.colorTo(Theme.BgLight.color, 150, EasingType.EASE_IN) }
        )

        selectedButton.onClick { _, _, button ->
            when (button) {
                0 -> {
                    if (!isAnimating) {
                        if (expanded) collapse() else expand()
                    }
                    true
                }
                else -> false
            }
        }

        optionsContainer.visible = false
        createOptions()
    }

    private fun createOptions() {
        options.forEachIndexed { index, option ->
            val isFirst = index == 0
            val isLast = index == options.size - 1

            val topLeftRadius = if (isFirst) 5f else 0f
            val topRightRadius = if (isFirst) 5f else 0f
            val bottomLeftRadius = if (isLast) 5f else 0f
            val bottomRightRadius = if (isLast) 5f else 0f

            val optionRect = Rectangle(0x00000000, 0x00000000, 0f, 0f)
                .borderRadiusVarying(topRight = topRightRadius, topLeft = topLeftRadius, bottomRight = bottomRightRadius, bottomLeft = bottomLeftRadius)
                .setSizing(containerWidth, Size.Pixels, optionHeight, Size.Pixels)
                .setPositioning(0f, Pos.ParentPixels, 0f, Pos.AfterSibling)
                .childOf(optionsContainer)

            val checkbox = Rectangle(Theme.BgLight.color, Theme.Text.color, 3f, 1f)
                .setSizing(12f * scale, Size.Pixels, 12f * scale, Size.Pixels)
                .setPositioning(6f * scale, Pos.ParentPixels, 0f, Pos.ParentCenter)
                .childOf(optionRect)

            val checkmark = Rectangle(Theme.Primary.color, 0x00000000, 2f, 0f)
                .setSizing(8f * scale, Size.Pixels, 8f * scale, Size.Pixels)
                .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentCenter)
                .childOf(checkbox)

            val isSelected = selectedIndices.contains(index)
            checkmark.visible = isSelected

            Text(option, Theme.Text.color, 14f * scale)
                .setPositioning(24f * scale, Pos.ParentPixels, 0f, Pos.ParentCenter)
                .childOf(optionRect)

            optionRect.onHover(
                { _, _ -> optionRect.colorTo(Theme.Highlight.color, 150, EasingType.EASE_OUT) },
                { _, _ -> optionRect.colorTo(0x00000000, 150, EasingType.EASE_IN) }
            )

            optionRect.onClick { _, _, _ ->
                toggleOption(index, checkmark, checkbox)
                true
            }
        }
    }

    private fun updateButtonSize() {
        selectedButton.setSizing(100f * scale, Size.Pixels, 20f * scale, Size.Pixels)
    }

    private fun expand() {
        if (expanded) return

        closeAllMultiCheckboxes()
        openMultiCheckbox = this

        expanded = true
        isAnimating = true

        optionsContainer.visible = true
        val targetHeight = rowHeight + options.size * optionHeight + (6f * scale)
        val containerHeight = options.size * optionHeight

        animateSize(Panel.WIDTH, targetHeight, 200, EasingType.EASE_OUT) {
            isAnimating = false
            invalidateParentLayout()
        }
        optionsContainer.animateSize(containerWidth, containerHeight, 200, EasingType.EASE_OUT) {
            optionsContainer.children.forEachIndexed { index, optionRect ->
                optionRect.fadeIn(50, includeChildren = false)
                optionRect.children.forEach { child ->
                    when (child) {
                        is Text -> child.fadeIn(50, includeChildren = false)
                        is Rectangle -> {
                            child.fadeIn(50, includeChildren = false)
                            if (selectedIndices.contains(index)) {
                                child.children.forEach { it.fadeIn(50, includeChildren = false) }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun collapse() {
        if (!expanded) return

        expanded = false
        isAnimating = true
        openMultiCheckbox = null

        animateSize(Panel.WIDTH, rowHeight, 200, EasingType.EASE_IN) {
            optionsContainer.visible = false
            isAnimating = false
            invalidateParentLayout()
        }

        optionsContainer.children.forEach { child ->
            child.fadeOut(50)
            child.children.forEach { it.fadeOut(50) }
        }

        optionsContainer.animateSize(containerWidth, 0f, 200, EasingType.EASE_IN)
    }

    private fun toggleOption(index: Int, checkmark: VexelElement<*>, checkbox: Rectangle) {
        val wasSelected = selectedIndices.contains(index)

        if (wasSelected) {
            selectedIndices.remove(index)
            checkmark.visible = false
        } else {
            selectedIndices.add(index)
            checkmark.visible = true
            checkbox.colorTo(Theme.Primary.withAlpha(0.3f), 100, EasingType.EASE_OUT) {
                checkbox.colorTo(Theme.BgLight.color, 100, EasingType.EASE_IN)
            }
        }

        selectedText.text = "${selectedIndices.size} selected"
        onValueChange.forEach { it.invoke(selectedIndices.toSet()) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun invalidateParentLayout() {
        var current = parent as? VexelElement<SectionButton>

        while (current != null && current !is SectionButton) {
            current = current.parent as? VexelElement<SectionButton>
        }

        current?.recalculateHeight()
    }

    override fun onRender(mouseX: Float, mouseY: Float) {}
}
