package xenus.xenaddons.gui.config.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW
import xenus.xenaddons.gui.HudEditorScreen
import xenus.xenaddons.gui.config.ui.elements.MCColorCode
import xenus.xenaddons.gui.config.ui.elements.base.ElementType
import xenus.xenaddons.gui.managers.config.CategoryElement
import xenus.xenaddons.gui.managers.config.ConfigElement
import xenus.xenaddons.gui.managers.config.ConfigManager
import xenus.xenaddons.gui.managers.config.FeatureElement
import xenus.xenaddons.gui.managers.config.OptionElement
import xenus.xenaddons.gui.odin.ui.HoverHandler
import xenus.xenaddons.gui.odin.ui.TextInputHandler
import xenus.xenaddons.gui.odin.ui.animations.EaseInOutAnimation
import xenus.xenaddons.gui.odin.ui.animations.EaseOutAnimation
import xenus.xenaddons.render.XenGradient
import xenus.xenaddons.render.XenImage
import xenus.xenaddons.render.nvg.XenNvgGuiRenderer
import xenus.xenaddons.render.XenRenderer
import xenus.xenaddons.gui.ui.Theme
import java.awt.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

typealias ConfigData = Map<String, Any>

object ClickGUI : Screen(Text.literal("Click GUI")) {
    private const val PANEL_WIDTH = 240f
    private const val PANEL_SPACING = 18f
    private const val PANEL_HEADER_HEIGHT = 32f
    private const val FEATURE_ROW_HEIGHT = 32f
    private const val FEATURE_GAP = 0f
    private const val FEATURE_PADDING = 8f
    private const val SECTION_HEADER_HEIGHT = 20f
    private const val SECTION_GAP = 6f
    private const val WIDGET_SPACING = 4f
    private const val STACKED_FIELD_OFFSET_Y = 24f
    private const val STACKED_FIELD_BASE_HEIGHT = 50f
    private const val SEARCH_WIDTH = 350f
    private const val SEARCH_HEIGHT = 40f
    private const val TOP_MARGIN = 28f
    private const val BOTTOM_MARGIN = 28f
    private const val SIDE_MARGIN = 36f
    private const val PANEL_TOP_OFFSET = 36f
    private const val BASE_LAYOUT_HEIGHT = 640f
    private const val MAX_UI_SCALE = 0.82f
    private const val UI_SCALE_MULTIPLIER = 0.6f
    private const val PANEL_RADIUS = 15f
    private const val FIELD_CUT = 6f
    private const val CHOICE_ROW_HEIGHT = 22f

    private val categoryOrder = listOf("misc", "dungeons", "dev")
    private val configListeners = mutableMapOf<String, MutableList<(Any) -> Unit>>()
    private val closeListeners = mutableListOf<() -> Unit>()
    private val elementRefs = mutableMapOf<String, ConfigElement>()

    private val movementImage: XenImage = XenRenderer.createImage("/assets/xenaddons/render/movement.svg")
    private val hueImage: XenImage = XenRenderer.createImage("/assets/xenaddons/render/hue_gradient.png")
    private val chevronImage: XenImage = XenRenderer.createImage("/assets/xenaddons/render/chevron.svg")

    private val panels = mutableListOf<PanelState>()
    private var searchBar = SearchBar()
    private var openAnim = EaseOutAnimation(350)

    private var tooltip = Tooltip("", 0f, 0f, HoverHandler(150))
    private var lastMouseX = 0f
    private var lastMouseY = 0f
    private var uiScale = 1f
    private var layoutWidth = 0f
    private var layoutHeight = 0f

    override fun init() {
        updateResponsiveMetrics()
        rebuildPanels()
        openAnim = EaseOutAnimation(350).also { it.start() }
        super.init()
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        updateResponsiveMetrics()
        lastMouseX = toLayoutCoordinate(mouseX.toFloat())
        lastMouseY = toLayoutCoordinate(mouseY.toFloat())
        tooltip = Tooltip("", lastMouseX + 14f, lastMouseY + 14f, HoverHandler(150))

        XenNvgGuiRenderer.draw(context, 0, 0, width, height) {
            XenRenderer.rect(0f, 0f, width.toFloat(), height.toFloat(), 0xB0000000.toInt())
            XenRenderer.push()
            XenRenderer.scale(uiScale, uiScale)

            if (openAnim.isAnimating()) {
                val scale = openAnim.get(0.92f, 1f)
                val centerX = layoutWidth / 2f
                val centerY = layoutHeight / 2f
                XenRenderer.push()
                XenRenderer.translate(centerX, centerY)
                XenRenderer.scale(scale, scale)
                XenRenderer.translate(-centerX, -centerY)
            }

            searchBar.render(lastMouseX, lastMouseY)

            val draggedPanel = panels.firstOrNull { it.dragging }
            for (panel in panels) {
                if (panel !== draggedPanel) panel.render(lastMouseX, lastMouseY)
            }
            draggedPanel?.render(lastMouseX, lastMouseY)

            renderTooltip()
            if (openAnim.isAnimating()) {
                XenRenderer.pop()
            }
            XenRenderer.pop()
        }
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        updateResponsiveMetrics()
        val mouseX = toLayoutCoordinate(click.x().toFloat())
        val mouseY = toLayoutCoordinate(click.y().toFloat())
        panels.forEach { it.globalClick(mouseX, mouseY, click.button()) }
        searchBar.globalClick(mouseX, mouseY, click.button())

        if (searchBar.mouseClicked(mouseX, mouseY, click.button())) {
            return true
        }

        for (i in panels.size - 1 downTo 0) {
            val panel = panels[i]
            if (panel.mouseClicked(mouseX, mouseY, click.button())) {
                panels.removeAt(i)
                panels.add(panel)
                return true
            }
        }

        return super.mouseClicked(click, doubled)
    }

    override fun mouseReleased(click: Click): Boolean {
        searchBar.mouseReleased()
        panels.forEach { it.mouseReleased(click.button()) }
        return super.mouseReleased(click)
    }

    override fun mouseDragged(click: Click, deltaX: Double, deltaY: Double): Boolean {
        return panels.any { it.dragging || it.hasDraggingWidget() } || super.mouseDragged(click, deltaX, deltaY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        updateResponsiveMetrics()
        val actualAmount = (verticalAmount.sign * 16.0).toFloat()
        val scaledMouseX = toLayoutCoordinate(mouseX.toFloat())
        val scaledMouseY = toLayoutCoordinate(mouseY.toFloat())
        if (searchBar.mouseScrolled(scaledMouseX, scaledMouseY, actualAmount)) {
            return true
        }

        for (i in panels.size - 1 downTo 0) {
            if (panels[i].mouseScrolled(scaledMouseX, scaledMouseY, actualAmount)) {
                return true
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (searchBar.keyPressed(input)) return true

        for (i in panels.size - 1 downTo 0) {
            if (panels[i].keyPressed(input)) return true
        }

        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            close()
            return true
        }

        return super.keyPressed(input)
    }

    override fun charTyped(input: CharInput): Boolean {
        if (searchBar.charTyped(input)) return true

        for (i in panels.size - 1 downTo 0) {
            if (panels[i].charTyped(input)) return true
        }

        return super.charTyped(input)
    }

    override fun shouldPause(): Boolean = false

    override fun close() {
        ConfigManager.saveConfig(true)
        closeListeners.forEach { it() }
        client?.setScreen(null)
    }

    fun updateTooltip(description: String) {
        setTooltip(description, lastMouseX + 14f, lastMouseY + 14f, HoverHandler(150))
    }

    fun updateConfig(configKey: String, newValue: Any) {
        val serializedValue = when (newValue) {
            is Color -> mapOf(
                "r" to newValue.red,
                "g" to newValue.green,
                "b" to newValue.blue,
                "a" to newValue.alpha
            )
            is Set<*> -> newValue.toList()
            is MCColorCode -> newValue.code
            else -> newValue
        }

        ConfigManager.configValueMap[configKey] = serializedValue
        ConfigManager.saveConfig(false)
        configListeners[configKey]?.forEach { it(newValue) }
    }

    fun registerListener(configKey: String, listener: (Any) -> Unit): ClickGUI {
        configListeners.getOrPut(configKey) { mutableListOf() }.add(listener)

        (ConfigManager.getConfigValue(configKey) ?: getDefaultValue(elementRefs[configKey]?.type))?.let { currentValue ->
            listener(resolveValue(currentValue))
        }

        return this
    }

    fun registerCloseListener(listener: () -> Unit): ClickGUI {
        closeListeners.add(listener)
        return this
    }

    fun getConfigValue(configKey: String): Any? {
        return ConfigManager.getConfigValue(configKey)?.let(::resolveValue)
    }

    private fun updateResponsiveMetrics() {
        val categoryCount = panels.size.takeIf { it > 0 } ?: max(ConfigManager.configTree.size, 1)
        val panelRowWidth = categoryCount * PANEL_WIDTH + max(categoryCount - 1, 0) * PANEL_SPACING
        val designWidth = max(panelRowWidth + SIDE_MARGIN * 2f, SEARCH_WIDTH + 96f)
        val widthScale = width.toFloat() / designWidth
        val heightScale = height.toFloat() / BASE_LAYOUT_HEIGHT
        val candidate = min(MAX_UI_SCALE, min(widthScale, heightScale)) * UI_SCALE_MULTIPLIER

        uiScale = if (candidate.isFinite() && candidate > 0f) candidate else 1f
        layoutWidth = width.toFloat() / uiScale
        layoutHeight = height.toFloat() / uiScale
    }

    private fun toLayoutCoordinate(value: Float): Float = value / uiScale

    private fun searchBarY(): Float = layoutHeight - SEARCH_HEIGHT - BOTTOM_MARGIN

    private fun rebuildPanels() {
        elementRefs.clear()
        panels.clear()
        searchBar = SearchBar()

        val sortedCategories = ConfigManager.configTree.sortedWith(
            compareBy<CategoryElement> { category ->
                categoryOrder.indexOf(category.name.lowercase()).takeIf { it >= 0 } ?: Int.MAX_VALUE
            }.thenBy { it.name }
        )
        val panelRowWidth = sortedCategories.size * PANEL_WIDTH + max(sortedCategories.size - 1, 0) * PANEL_SPACING
        val startX = SIDE_MARGIN

        sortedCategories.forEachIndexed { index, category ->
            val x = startX + index * (PANEL_WIDTH + PANEL_SPACING)
            panels += PanelState(category, x, TOP_MARGIN + PANEL_TOP_OFFSET)
        }
    }

    private fun renderTooltip() {
        if (tooltip.text.isBlank() || tooltip.hover.percent() < 100f) return

        val bounds = XenRenderer.wrappedTextBounds(tooltip.text, 300f, 15f)
        val width = bounds[2] - bounds[0] + 16f
        val height = bounds[3] - bounds[1] + 16f
        val x = tooltip.x.coerceAtMost(layoutWidth - width - 12f)
        val y = tooltip.y.coerceAtMost(layoutHeight - height - 12f)

        XenRenderer.rect(x, y, width, height, Theme.BgLight.color, FIELD_CUT)
        XenRenderer.hollowRect(x, y, width, height, 1.25f, Theme.Primary.color, FIELD_CUT)
        XenRenderer.drawWrappedString(tooltip.text, x + 8f, y + 8f, 300f, 15f, Theme.Text.color)
    }

    private fun setTooltip(text: String, x: Float, y: Float, hover: HoverHandler) {
        tooltip = Tooltip(text, x, y, hover)
    }

    private fun getDefaultValue(type: ElementType?): Any? = when (type) {
        is ElementType.Switch -> type.default
        is ElementType.Slider -> type.default
        is ElementType.Dropdown -> type.default
        is ElementType.TextInput -> type.default
        is ElementType.ColorPicker -> type.default
        is ElementType.Keybind -> type.default
        is ElementType.MultiCheckbox -> type.default
        is ElementType.MCColorPicker -> type.default
        else -> null
    }

    private fun resolveValue(value: Any): Any {
        return when (value) {
            is Map<*, *> -> {
                val r = (value["r"] as? Number)?.toInt() ?: 255
                val g = (value["g"] as? Number)?.toInt() ?: 255
                val b = (value["b"] as? Number)?.toInt() ?: 255
                val a = (value["a"] as? Number)?.toInt() ?: 255
                Color(r, g, b, a)
            }
            is List<*> -> value.mapNotNull { (it as? Number)?.toInt() }.toSet()
            is String -> MCColorCode.entries.firstOrNull { it.code == value } ?: value
            else -> value
        }
    }

    private data class Tooltip(
        val text: String,
        val x: Float,
        val y: Float,
        val hover: HoverHandler
    )

    private class SearchBar {
        private var query = ""
        private val inputHandler = TextInputHandler(
            textProvider = { query },
            textSetter = { query = it.take(16) },
            maxLength = 16
        )
        private val hudHover = HoverHandler(150)

        private var x = 0f
        private var y = 0f

        fun render(mouseX: Float, mouseY: Float) {
            x = ClickGUI.layoutWidth / 2f - SEARCH_WIDTH / 2f
            y = ClickGUI.searchBarY()

            val searchRadius = SEARCH_HEIGHT / 2f
            XenRenderer.dropShadow(x, y, SEARCH_WIDTH, SEARCH_HEIGHT, 7f, 0.5f, searchRadius)
            XenRenderer.rect(x, y, SEARCH_WIDTH, SEARCH_HEIGHT, Theme.BgLight.color, searchRadius)
            XenRenderer.hollowRect(x, y, SEARCH_WIDTH, SEARCH_HEIGHT, 1.25f, Theme.Primary.withAlpha(0.72f), searchRadius)

            val hudButtonX = x + SEARCH_WIDTH - SEARCH_HEIGHT
            val searchInputX = x + 18f
            hudHover.handle(mouseX, mouseY, hudButtonX, y, SEARCH_HEIGHT, SEARCH_HEIGHT)
            if (hudHover.isHovered) {
                ClickGUI.setTooltip("HUD Editor", hudButtonX - 6f, y + SEARCH_HEIGHT + 8f, hudHover)
            }

            XenRenderer.line(hudButtonX, y + 7f, hudButtonX, y + SEARCH_HEIGHT - 7f, 1f, Theme.Border.color)
            XenRenderer.rect(
                hudButtonX + 6f,
                y + 6f,
                SEARCH_HEIGHT - 12f,
                SEARCH_HEIGHT - 12f,
                if (hudHover.isHovered) Theme.shift(Theme.BgLight.color, 1.16f) else Theme.Bg.color,
                (SEARCH_HEIGHT - 12f) / 2f
            )
            XenRenderer.image(movementImage, hudButtonX + 11f, y + 11f, 18f, 18f)

            val placeholder = "Search here..."
            val currentText = query
            if (currentText.isEmpty()) {
                XenRenderer.text(
                    placeholder,
                    searchInputX + 4f,
                    y + 10f,
                    20f,
                    Theme.TextMuted.color
                )
            }

            inputHandler.x = searchInputX
            inputHandler.y = y + 9f
            inputHandler.width = hudButtonX - searchInputX - 14f
            inputHandler.height = 22f
            inputHandler.draw(mouseX, mouseY, Theme.Primary.withAlpha(0.28f), Theme.Text.color, Theme.Text.color)
        }

        fun globalClick(mouseX: Float, mouseY: Float, button: Int) {
            if (button != 0 && !isHoveringHudButton(mouseX, mouseY)) {
                inputHandler.stopListening()
            }
        }

        fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
            if (isHoveringHudButton(mouseX, mouseY)) {
                if (button == 0) {
                    val mc = MinecraftClient.getInstance()
                    mc.setScreen(HudEditorScreen(mc.currentScreen))
                    return true
                }
                return false
            }
            return inputHandler.mouseClicked(mouseX, mouseY, button)
        }

        fun mouseReleased() {
            inputHandler.mouseReleased()
        }

        fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float): Boolean = false

        fun keyPressed(input: KeyInput): Boolean = inputHandler.keyPressed(input)

        fun charTyped(input: CharInput): Boolean = inputHandler.charTyped(input)

        private fun isHoveringHudButton(mouseX: Float, mouseY: Float): Boolean {
            val hudButtonX = x + SEARCH_WIDTH - SEARCH_HEIGHT
            return mouseX in hudButtonX..(hudButtonX + SEARCH_HEIGHT) && mouseY in y..(y + SEARCH_HEIGHT)
        }

        val currentSearch: String
            get() = query
    }

    private class PanelState(
        private val category: CategoryElement,
        initialX: Float,
        initialY: Float
    ) {
        private val features = category.features.sortedBy { it.featureName.lowercase() }.map { FeatureEntry(it) }

        var x = initialX
        var y = initialY
        var expanded = true
        var dragging = false
            private set

        private var dragOffsetX = 0f
        private var dragOffsetY = 0f
        private var scrollOffset = 0f
        private var maxScroll = 0f
        private var totalHeight = PANEL_HEADER_HEIGHT
        private var headerHover = false

        fun render(mouseX: Float, mouseY: Float) {
            if (dragging) {
                x = (mouseX + dragOffsetX).coerceIn(12f, ClickGUI.layoutWidth - PANEL_WIDTH - 12f)
                y = (mouseY + dragOffsetY).coerceIn(
                    12f,
                    ClickGUI.searchBarY() - PANEL_HEADER_HEIGHT - 12f
                )
            }

            headerHover = mouseX in x..(x + PANEL_WIDTH) && mouseY in y..(y + PANEL_HEADER_HEIGHT)

            val visibleFeatures = features.filter { it.matchesSearch(ClickGUI.searchBar.currentSearch) }
            val contentHeight = if (expanded && visibleFeatures.isNotEmpty()) {
                visibleFeatures.sumOf { it.getHeight().toDouble() }.toFloat() + FEATURE_GAP * (visibleFeatures.size - 1)
            } else {
                0f
            }
            val maxBodyHeight = (ClickGUI.searchBarY() - y - 16f - PANEL_HEADER_HEIGHT).coerceAtLeast(0f)
            val bodyHeight = min(contentHeight, maxBodyHeight)
            maxScroll = (contentHeight - bodyHeight).coerceAtLeast(0f)
            scrollOffset = scrollOffset.coerceIn(-maxScroll, 0f)
            totalHeight = PANEL_HEADER_HEIGHT + bodyHeight

            XenRenderer.dropShadow(x, y, PANEL_WIDTH, totalHeight, 8f, 1.5f, PANEL_RADIUS)
            XenRenderer.rect(x, y, PANEL_WIDTH, totalHeight, Theme.Bg.color, PANEL_RADIUS)
            XenRenderer.hollowRect(x, y, PANEL_WIDTH, totalHeight, 1f, Theme.Border.withAlpha(0.9f), PANEL_RADIUS)

            val headerColor = if (headerHover) Theme.shift(Theme.Bg.color, 1.12f) else Theme.Bg.color
            XenRenderer.rect(x, y, PANEL_WIDTH, PANEL_HEADER_HEIGHT, headerColor, PANEL_RADIUS)
            XenRenderer.rect(x + 1f, y + PANEL_HEADER_HEIGHT - PANEL_RADIUS, PANEL_WIDTH - 2f, PANEL_RADIUS + 1f, headerColor)
            XenRenderer.line(x + 12f, y + PANEL_HEADER_HEIGHT - 2f, x + PANEL_WIDTH - 12f, y + PANEL_HEADER_HEIGHT - 2f, 1f, Theme.BorderMuted.color)

            val categoryTitle = category.name.uppercase()
            val categoryTitleWidth = XenRenderer.textWidth(categoryTitle, 16f)
            XenRenderer.text(
                categoryTitle,
                x + PANEL_WIDTH / 2f - categoryTitleWidth / 2f,
                y + PANEL_HEADER_HEIGHT / 2f - 8f,
                16f,
                Theme.Text.color
            )

            if (!expanded || bodyHeight <= 0f) return

            XenRenderer.pushScissor(x, y + PANEL_HEADER_HEIGHT, PANEL_WIDTH, bodyHeight)
            var drawY = y + PANEL_HEADER_HEIGHT + scrollOffset
            for ((index, feature) in visibleFeatures.withIndex()) {
                drawY += feature.render(x, drawY, PANEL_WIDTH, mouseX, mouseY)
                if (index != visibleFeatures.lastIndex) {
                    drawY += FEATURE_GAP
                }
            }
            XenRenderer.popScissor()
        }

        fun globalClick(mouseX: Float, mouseY: Float, button: Int) {
            features.forEach { it.globalClick(mouseX, mouseY, button) }
        }

        fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
            if (mouseX in x..(x + PANEL_WIDTH) && mouseY in y..(y + PANEL_HEADER_HEIGHT)) {
                when (button) {
                    0 -> {
                        dragOffsetX = x - mouseX
                        dragOffsetY = y - mouseY
                        dragging = true
                        return true
                    }
                    1 -> {
                        expanded = !expanded
                        return true
                    }
                }
            }

            if (!expanded || mouseX !in x..(x + PANEL_WIDTH) || mouseY !in y..(y + totalHeight)) {
                return false
            }

            for (i in features.size - 1 downTo 0) {
                if (!features[i].matchesSearch(ClickGUI.searchBar.currentSearch)) continue
                if (features[i].mouseClicked(mouseX, mouseY, button)) {
                    return true
                }
            }

            return false
        }

        fun mouseReleased(button: Int) {
            dragging = false
            features.forEach { it.mouseReleased(button) }
        }

        fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float): Boolean {
            if (mouseX !in x..(x + PANEL_WIDTH) || mouseY !in y..(y + totalHeight)) return false
            if (expanded && maxScroll > 0f) {
                scrollOffset = (scrollOffset + amount).coerceIn(-maxScroll, 0f)
                return true
            }

            for (feature in features) {
                if (!feature.matchesSearch(ClickGUI.searchBar.currentSearch)) continue
                if (feature.mouseScrolled(mouseX, mouseY, amount)) return true
            }

            return false
        }

        fun keyPressed(input: KeyInput): Boolean {
            if (!expanded) return false
            for (feature in features) {
                if (!feature.matchesSearch(ClickGUI.searchBar.currentSearch)) continue
                if (feature.keyPressed(input)) return true
            }
            return false
        }

        fun charTyped(input: CharInput): Boolean {
            if (!expanded) return false
            for (feature in features) {
                if (!feature.matchesSearch(ClickGUI.searchBar.currentSearch)) continue
                if (feature.charTyped(input)) return true
            }
            return false
        }

        fun hasDraggingWidget(): Boolean = features.any { it.hasDraggingWidget() }
    }

    private class FeatureEntry(
        val feature: FeatureElement
    ) {
        private val descriptionHover = HoverHandler(150)
        private val expandAnim = EaseInOutAnimation(180)
        private val sections = feature.options.entries.map { (sectionName, options) ->
            SectionBlock(sectionName, options.map { createWidget(it) })
        }
        private val hasSettings = sections.any { it.widgets.isNotEmpty() }
        private val searchContent = buildSearchContent(feature)

        private var expanded = false
        private var lastX = 0f
        private var lastY = 0f
        private var lastWidth = 0f
        private var lastHeight = FEATURE_ROW_HEIGHT

        init {
            elementRefs[feature.configElement.configKey] = feature.configElement
            sections.flatMap { it.widgets }.forEach { elementRefs[it.option.configElement.configKey] = it.option.configElement }
        }

        fun matchesSearch(query: String): Boolean {
            val terms = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            return terms.isEmpty() || terms.all { term ->
                searchContent.any { it.contains(term, ignoreCase = true) }
            }
        }

        fun getHeight(): Float {
            val settingsHeight = settingsHeight()
            val progress = if (expandAnim.isAnimating()) expandAnim.get(0f, 1f, !expanded) else if (expanded) 1f else 0f
            return FEATURE_ROW_HEIGHT + settingsHeight * progress
        }

        fun render(x: Float, y: Float, width: Float, mouseX: Float, mouseY: Float): Float {
            lastX = x
            lastY = y
            lastWidth = width
            lastHeight = getHeight()

            val enabled = getFeatureEnabled()
            val hovered = mouseX in x..(x + width) && mouseY in y..(y + FEATURE_ROW_HEIGHT)
            val rowX = x
            val rowY = y
            val rowWidth = width
            val rowHeight = FEATURE_ROW_HEIGHT
            val rowColor = if (hovered) Theme.shift(Theme.BgLight.color, 1.06f) else Theme.BgLight.color
            val stateColor = if (enabled) Theme.Primary.color else Theme.Border.color

            descriptionHover.handle(mouseX, mouseY, x, y, width, FEATURE_ROW_HEIGHT)
            if (hovered && feature.description.isNotBlank()) {
                ClickGUI.setTooltip(feature.description, x + width + 10f, y, descriptionHover)
            }

            XenRenderer.rect(rowX, rowY, rowWidth, rowHeight, rowColor)
            XenRenderer.rect(rowX, rowY, 5f, rowHeight, stateColor)
            XenRenderer.line(rowX, rowY + rowHeight - 1f, rowX + rowWidth, rowY + rowHeight - 1f, 1f, Theme.BorderMuted.color)

            val titleWidth = XenRenderer.textWidth(feature.featureName, 16f)
            XenRenderer.pushScissor(rowX + 12f, rowY, rowWidth - 40f, rowHeight)
            XenRenderer.text(
                feature.featureName,
                rowX + rowWidth / 2f - titleWidth / 2f,
                y + FEATURE_ROW_HEIGHT / 2f - 8f,
                16f,
                if (enabled) Theme.Text.color else Theme.TextMuted.color
            )
            XenRenderer.popScissor()

            if (hasSettings) {
                val rotation = ((if (expandAnim.isAnimating()) expandAnim.get(0f, 1f, !expanded) else if (expanded) 1f else 0f) * (PI.toFloat() / 2f))
                XenRenderer.push()
                XenRenderer.translate(rowX + rowWidth - 15f, y + FEATURE_ROW_HEIGHT / 2f)
                XenRenderer.rotate(rotation)
                XenRenderer.image(chevronImage, -6f, -6f, 12f, 12f)
                XenRenderer.pop()
            }

            val progress = if (expandAnim.isAnimating()) expandAnim.get(0f, 1f, !expanded) else if (expanded) 1f else 0f
            if (progress <= 0f || !hasSettings) return FEATURE_ROW_HEIGHT

            val settingsHeight = settingsHeight()
            val animatedHeight = settingsHeight * progress
            XenRenderer.pushScissor(x, y + FEATURE_ROW_HEIGHT, width, animatedHeight)
            XenRenderer.rect(x, y + FEATURE_ROW_HEIGHT, width, settingsHeight, Theme.BgDark.color)
            XenRenderer.line(x, y + FEATURE_ROW_HEIGHT, x + width, y + FEATURE_ROW_HEIGHT, 1f, Theme.BorderMuted.color)
            XenRenderer.line(x, y + FEATURE_ROW_HEIGHT + settingsHeight - 1f, x + width, y + FEATURE_ROW_HEIGHT + settingsHeight - 1f, 1f, Theme.BorderMuted.color)

            var drawY = y + FEATURE_ROW_HEIGHT + FEATURE_PADDING
            for (section in sections) {
                val visibleWidgets = section.widgets.filter { it.isVisible() }
                if (visibleWidgets.isEmpty()) continue

                XenRenderer.text(section.title.uppercase(), x + 16f, drawY + 2f, 11f, Theme.TextMuted.color)
                drawY += SECTION_HEADER_HEIGHT

                for ((index, widget) in visibleWidgets.withIndex()) {
                    val widgetHeight = widget.render(x + 6f, drawY, width - 12f, mouseX, mouseY)
                    drawY += widgetHeight
                    if (index != visibleWidgets.lastIndex) {
                        drawY += WIDGET_SPACING
                    }
                }
                drawY += SECTION_GAP
            }

            XenRenderer.popScissor()
            return FEATURE_ROW_HEIGHT + animatedHeight
        }

        fun globalClick(mouseX: Float, mouseY: Float, button: Int) {
            sections.flatMap { it.widgets }.forEach { it.globalClick(mouseX, mouseY, button) }
        }

        fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
            if (mouseX in lastX..(lastX + lastWidth) && mouseY in lastY..(lastY + FEATURE_ROW_HEIGHT)) {
                when (button) {
                    0 -> {
                        ClickGUI.updateConfig(feature.configElement.configKey, !getFeatureEnabled())
                        return true
                    }
                    1 -> {
                        if (hasSettings) {
                            expandAnim.start()
                            expanded = !expanded
                            return true
                        }
                    }
                }
            }

            if (!expanded || mouseX !in lastX..(lastX + lastWidth) || mouseY !in (lastY + FEATURE_ROW_HEIGHT)..(lastY + lastHeight)) {
                return false
            }

            for (widget in sections.flatMap { it.widgets }.asReversed()) {
                if (!widget.isVisible()) continue
                if (widget.mouseClicked(mouseX, mouseY, button)) return true
            }

            return false
        }

        fun mouseReleased(button: Int) {
            sections.flatMap { it.widgets }.forEach { it.mouseReleased(button) }
        }

        fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float): Boolean {
            if (!expanded) return false
            for (widget in sections.flatMap { it.widgets }.asReversed()) {
                if (!widget.isVisible()) continue
                if (widget.mouseScrolled(mouseX, mouseY, amount)) return true
            }
            return false
        }

        fun keyPressed(input: KeyInput): Boolean {
            if (!expanded) return false
            for (widget in sections.flatMap { it.widgets }.asReversed()) {
                if (!widget.isVisible()) continue
                if (widget.keyPressed(input)) return true
            }
            return false
        }

        fun charTyped(input: CharInput): Boolean {
            if (!expanded) return false
            for (widget in sections.flatMap { it.widgets }.asReversed()) {
                if (!widget.isVisible()) continue
                if (widget.charTyped(input)) return true
            }
            return false
        }

        fun hasDraggingWidget(): Boolean = sections.flatMap { it.widgets }.any { it.isDragging() }

        private fun settingsHeight(): Float {
            var total = FEATURE_PADDING
            for (section in sections) {
                val visibleWidgets = section.widgets.filter { it.isVisible() }
                if (visibleWidgets.isEmpty()) continue
                total += SECTION_HEADER_HEIGHT
                total += visibleWidgets.sumOf { it.getHeight().toDouble() }.toFloat()
                total += max(visibleWidgets.size - 1, 0) * WIDGET_SPACING
                total += SECTION_GAP
            }
            return total
        }

        private fun buildSearchContent(feature: FeatureElement): List<String> {
            val content = mutableListOf(
                feature.featureName,
                feature.description,
                feature.configElement.configKey
            )

            for ((sectionName, options) in feature.options) {
                content += sectionName

                for (option in options) {
                    content += option.optionName
                    content += option.configElement.configKey

                    when (val type = option.configElement.type) {
                        is ElementType.Button -> content += type.text
                        is ElementType.Dropdown -> content += type.options
                        is ElementType.TextInput -> {
                            content += type.default
                            content += type.placeholder
                        }
                        is ElementType.TextParagraph -> content += type.text
                        is ElementType.MultiCheckbox -> content += type.options
                        is ElementType.MCColorPicker -> content += type.default.name
                        is ElementType.Slider,
                        is ElementType.Switch,
                        is ElementType.ColorPicker,
                        is ElementType.Keybind -> Unit
                    }
                }
            }

            return content.filter { it.isNotBlank() }
        }

        private fun getFeatureEnabled(): Boolean {
            return (ClickGUI.getConfigValue(feature.configElement.configKey) as? Boolean)
                ?: (ConfigManager.getConfigValue(feature.configElement.configKey) as? Boolean)
                ?: false
        }
    }

    private data class SectionBlock(
        val title: String,
        val widgets: List<SettingWidget>
    )

    private abstract class SettingWidget(
        val option: OptionElement
    ) {
        protected var x = 0f
        protected var y = 0f
        protected var width = 0f
        protected var boundsHeight = 0f

        open fun getHeight(): Float = boundsHeight

        fun isVisible(): Boolean = option.configElement.shouldShow(ConfigManager.configValueMap)

        abstract fun render(x: Float, y: Float, width: Float, mouseX: Float, mouseY: Float): Float

        open fun globalClick(mouseX: Float, mouseY: Float, button: Int) {}

        open fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean = false

        open fun mouseReleased(button: Int) {}

        open fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float): Boolean = false

        open fun keyPressed(input: KeyInput): Boolean = false

        open fun charTyped(input: CharInput): Boolean = false

        open fun isDragging(): Boolean = false

        protected fun currentValue(): Any? = ClickGUI.getConfigValue(option.configElement.configKey)

        protected fun update(value: Any) = ClickGUI.updateConfig(option.configElement.configKey, value)

        protected fun isHovered(mouseX: Float, mouseY: Float): Boolean =
            mouseX in x..(x + width) && mouseY in y..(y + boundsHeight)

        protected fun labelY(localY: Float, labelHeight: Float = 15f): Float =
            localY + boundsHeight / 2f - labelHeight / 2f

        protected fun field(x: Float, y: Float, width: Float, height: Float, active: Boolean = false, hovered: Boolean = false) {
            val fill = if (hovered) Theme.shift(Theme.BgLight.color, 1.08f) else Theme.BgLight.color
            val border = if (active) Theme.Primary.color else Theme.Primary.withAlpha(0.65f)
            XenRenderer.rect(x, y, width, height, fill, FIELD_CUT)
            XenRenderer.hollowRect(x, y, width, height, 1.2f, border, FIELD_CUT)
        }

        protected fun accentField(x: Float, y: Float, width: Float, height: Float, active: Boolean = false, hovered: Boolean = false) {
            field(x, y, width, height, active, hovered)
        }
    }

    private class SwitchWidget(option: OptionElement) : SettingWidget(option) {
        override fun render(x: Float, y: Float, width: Float, mouseX: Float, mouseY: Float): Float {
            this.x = x
            this.y = y
            this.width = width
            this.boundsHeight = 34f

            val enabled = currentValue() as? Boolean ?: (option.configElement.type as ElementType.Switch).default
            XenRenderer.text(option.optionName, x + 2f, y + 9f, 16f, Theme.Text.color)

            val trackX = x + width - 46f
            val trackY = y + 7f
            XenRenderer.rect(trackX, trackY, 38f, 20f, if (enabled) Theme.Primary.withAlpha(0.82f) else Theme.BgLight.color, 10f)
            XenRenderer.hollowRect(trackX, trackY, 38f, 20f, 1f, if (enabled) Theme.Primary.color else Theme.Primary.withAlpha(0.65f), 10f)
            XenRenderer.circle(if (enabled) trackX + 28f else trackX + 10f, trackY + 10f, 6.5f, Theme.Text.color)
            return boundsHeight
        }

        override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
            if (button != 0 || !isHovered(mouseX, mouseY)) return false
            val current = currentValue() as? Boolean ?: false
            update(!current)
            return true
        }
    }

    private class SliderWidget(option: OptionElement) : SettingWidget(option) {
        private var dragging = false

        override fun getHeight(): Float = 44f

        override fun render(x: Float, y: Float, width: Float, mouseX: Float, mouseY: Float): Float {
            this.x = x
            this.y = y
            this.width = width
            this.boundsHeight = getHeight()

            val type = option.configElement.type as ElementType.Slider
            val value = (currentValue() as? Number)?.toDouble() ?: type.default
            val trackWidth = max(96f, width - 36f).coerceAtMost(width - 12f)
            val trackX = x + (width - trackWidth) / 2f
            if (dragging) {
                val percent = ((mouseX - trackX) / trackWidth).coerceIn(0f, 1f)
                val newValue = type.min + (type.max - type.min) * percent
                update(if (type.showDouble) roundToStep(newValue, 0.01) else newValue.roundToInt().toDouble())
            }

            val normalized = ((value - type.min) / (type.max - type.min)).toFloat().coerceIn(0f, 1f)
            XenRenderer.text(option.optionName, x + 2f, y + 2f, 16f, Theme.Text.color)
            XenRenderer.text(formatSliderValue(value, type.showDouble), x + width - 48f, y + 2f, 16f, Theme.TextMuted.color)

            val trackY = y + 27f
            XenRenderer.rect(trackX, trackY, trackWidth, 8f, Theme.BgLight.color, 4f)
            XenRenderer.rect(trackX, trackY, max(8f, trackWidth * normalized), 8f, Theme.Primary.color, 4f)
            XenRenderer.circle(trackX + trackWidth * normalized, trackY + 4f, 6.5f, Theme.Text.color)
            return boundsHeight
        }

        override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
            if (button != 0 || !isHovered(mouseX, mouseY)) return false
            dragging = true
            return true
        }

        override fun mouseReleased(button: Int) {
            dragging = false
        }

        override fun isDragging(): Boolean = dragging
    }

    private class DropdownWidget(option: OptionElement) : SettingWidget(option) {
        private var expanded = false

        override fun getHeight(): Float {
            val type = option.configElement.type as ElementType.Dropdown
            return STACKED_FIELD_BASE_HEIGHT + if (expanded) type.options.size * CHOICE_ROW_HEIGHT else 0f
        }

        override fun render(x: Float, y: Float, width: Float, mouseX: Float, mouseY: Float): Float {
            val type = option.configElement.type as ElementType.Dropdown
            this.x = x
            this.y = y
            this.width = width
            this.boundsHeight = getHeight()

            val selectedIndex = resolveDropdownIndex(type)
            val boxY = y + STACKED_FIELD_OFFSET_Y
            XenRenderer.text(option.optionName, x + 2f, y + 2f, 16f, Theme.Text.color)
            accentField(x + 2f, boxY, width - 4f, 22f, expanded)
            XenRenderer.text(type.options[selectedIndex], x + 10f, boxY + 4f, 15f, Theme.Text.color)

            XenRenderer.push()
            XenRenderer.translate(x + width - 14f, boxY + 11f)
            XenRenderer.rotate(if (expanded) PI.toFloat() / 2f else 0f)
            XenRenderer.image(chevronImage, -5f, -5f, 10f, 10f)
            XenRenderer.pop()

            if (expanded) {
                val listX = x + 2f
                val listY = boxY + 26f
                val listWidth = width - 4f
                val listHeight = type.options.size * CHOICE_ROW_HEIGHT

                XenRenderer.rect(listX, listY, listWidth, listHeight, Theme.BgLight.color, FIELD_CUT)
                for ((index, value) in type.options.withIndex()) {
                    val optionY = listY + index * CHOICE_ROW_HEIGHT
                    val optionHovered = mouseX in listX..(listX + listWidth) && mouseY in optionY..(optionY + CHOICE_ROW_HEIGHT)
                    if (optionHovered) {
                        XenRenderer.rect(listX, optionY, listWidth, CHOICE_ROW_HEIGHT, Theme.shift(Theme.BgLight.color, 1.08f))
                    } else if (index == selectedIndex) {
                        XenRenderer.rect(listX, optionY, listWidth, CHOICE_ROW_HEIGHT, Theme.Primary.withAlpha(0.14f))
                    }
                    if (index < type.options.lastIndex) {
                        XenRenderer.line(
                            listX + 8f,
                            optionY + CHOICE_ROW_HEIGHT,
                            listX + listWidth - 8f,
                            optionY + CHOICE_ROW_HEIGHT,
                            1f,
                            Theme.BorderMuted.color
                        )
                    }
                    XenRenderer.text(value, x + 10f, optionY + 4f, 15f, if (index == selectedIndex) Theme.Primary.color else Theme.Text.color)
                }
            }

            return boundsHeight
        }

        override fun globalClick(mouseX: Float, mouseY: Float, button: Int) {
            if (expanded && !isHovered(mouseX, mouseY)) {
                expanded = false
            }
        }

        override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
            if (button != 0) return false
            val type = option.configElement.type as ElementType.Dropdown
            val boxY = y + STACKED_FIELD_OFFSET_Y
            if (mouseX in (x + 2f)..(x + width - 2f) && mouseY in boxY..(boxY + 22f)) {
                expanded = !expanded
                return true
            }
            if (!expanded) return false

            var optionY = boxY + 26f
            for ((index, value) in type.options.withIndex()) {
                if (mouseX in (x + 2f)..(x + width - 2f) && mouseY in optionY..(optionY + CHOICE_ROW_HEIGHT)) {
                    update(value)
                    expanded = false
                    return true
                }
                optionY += CHOICE_ROW_HEIGHT
            }
            return false
        }

        private fun resolveDropdownIndex(type: ElementType.Dropdown): Int {
            return when (val stored = currentValue()) {
                is Number -> stored.toInt().coerceIn(0, type.options.lastIndex)
                is String -> type.options.indexOf(stored).takeIf { it >= 0 } ?: type.default.coerceIn(0, type.options.lastIndex)
                else -> type.default.coerceIn(0, type.options.lastIndex)
            }
        }
    }

    private class TextInputWidget(option: OptionElement) : SettingWidget(option) {
        private val type = option.configElement.type as ElementType.TextInput
        private val input = TextInputHandler(
            textProvider = { (currentValue() as? String) ?: type.default },
            textSetter = { update(it) },
            maxLength = type.maxLength
        )

        override fun getHeight(): Float = STACKED_FIELD_BASE_HEIGHT

        override fun render(x: Float, y: Float, width: Float, mouseX: Float, mouseY: Float): Float {
            this.x = x
            this.y = y
            this.width = width
            this.boundsHeight = getHeight()

            XenRenderer.text(option.optionName, x + 2f, y + 2f, 16f, Theme.Text.color)
            accentField(x + 2f, y + STACKED_FIELD_OFFSET_Y, width - 4f, 22f, input.listening)
            input.x = x + 6f
            input.y = y + STACKED_FIELD_OFFSET_Y + 1f
            input.width = width - 12f
            input.height = 20f
            input.draw(mouseX, mouseY, Theme.Primary.withAlpha(0.28f), Theme.Text.color, Theme.Text.color)

            if (((currentValue() as? String) ?: "").isEmpty()) {
                XenRenderer.text(type.placeholder, x + 8f, y + STACKED_FIELD_OFFSET_Y + 4f, 14f, Theme.TextMuted.color)
            }

            return boundsHeight
        }

        override fun globalClick(mouseX: Float, mouseY: Float, button: Int) {
            if (!isHovered(mouseX, mouseY)) {
                input.stopListening()
            }
        }

        override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean =
            input.mouseClicked(mouseX, mouseY, button)

        override fun mouseReleased(button: Int) {
            input.mouseReleased()
        }

        override fun keyPressed(input: KeyInput): Boolean = this.input.keyPressed(input)

        override fun charTyped(input: CharInput): Boolean = this.input.charTyped(input)
    }

    private class ColorWidget(option: OptionElement) : SettingWidget(option) {
        private var expanded = false
        private var dragMode: DragMode? = null

        private var hue = 0f
        private var saturation = 1f
        private var brightness = 1f
        private var alpha = 1f
        private var lastArgb = Int.MIN_VALUE

        override fun getHeight(): Float = 40f + if (expanded) pickerSize() + 44f else 0f

        override fun render(x: Float, y: Float, width: Float, mouseX: Float, mouseY: Float): Float {
            this.x = x
            this.y = y
            this.width = width
            this.boundsHeight = getHeight()

            syncColorState()

            val previewX = x + width - 56f
            val previewY = y + 8f
            val currentColor = buildCurrentColor()
            val hueColor = Color.HSBtoRGB(hue, 1f, 1f) or 0xFF000000.toInt()

            if (dragMode != null) {
                updateFromMouse(mouseX, mouseY)
                val newColor = buildCurrentColor()
                if (newColor.rgb != currentColor.rgb) {
                    update(newColor)
                }
            }

            XenRenderer.text(option.optionName, x + 2f, y + 11f, 16f, Theme.Text.color)
            field(previewX, previewY, 48f, 22f, expanded)
            XenRenderer.rect(previewX + 20f, previewY + 4f, 22f, 14f, currentColor.rgb, 4f)
            XenRenderer.text(if (expanded) "Hide" else "Edit", previewX + 4f, previewY + 4f, 12f, Theme.TextMuted.color)

            if (!expanded) return boundsHeight

            val pickerX = x + 2f
            val pickerY = y + 40f
            val pickerSize = pickerSize()
            val infoX = pickerX + pickerSize + 10f
            val infoWidth = (x + width - 2f - infoX).coerceAtLeast(0f)
            val hueY = pickerY + pickerSize + 8f
            val alphaY = hueY + 22f
            val cardHeight = alphaY + 14f - pickerY

            XenRenderer.rect(x + 2f, pickerY, width - 4f, cardHeight, Theme.Bg.color, FIELD_CUT)
            XenRenderer.hollowRect(x + 2f, pickerY, width - 4f, cardHeight, 1f, Theme.BorderMuted.color, FIELD_CUT)

            XenRenderer.rect(pickerX, pickerY, pickerSize, pickerSize, hueColor, FIELD_CUT)
            XenRenderer.gradientRect(pickerX, pickerY, pickerSize, pickerSize, 0xFFFFFFFF.toInt(), 0x00FFFFFF, XenGradient.LeftToRight, 4f)
            XenRenderer.gradientRect(pickerX, pickerY, pickerSize, pickerSize, 0x00000000, 0xFF000000.toInt(), XenGradient.TopToBottom, 4f)
            XenRenderer.hollowRect(pickerX, pickerY, pickerSize, pickerSize, 1.5f, Theme.Primary.withAlpha(0.35f), FIELD_CUT)

            val selectorX = pickerX + saturation * pickerSize
            val selectorY = pickerY + (1f - brightness) * pickerSize
            XenRenderer.circle(selectorX, selectorY, 5f, 0xFFFFFFFF.toInt())
            XenRenderer.circle(selectorX, selectorY, 3f, currentColor.rgb)

            if (infoWidth >= 56f) {
                XenRenderer.text("Preview", infoX, pickerY + 2f, 12f, Theme.TextMuted.color)
                field(infoX, pickerY + 18f, infoWidth, 30f)
                XenRenderer.rect(infoX + 6f, pickerY + 24f, infoWidth - 12f, 18f, currentColor.rgb, 4f)

                XenRenderer.text("Hex", infoX, pickerY + 58f, 12f, Theme.TextMuted.color)
                XenRenderer.text(colorHex(currentColor), infoX, pickerY + 72f, 14f, Theme.Text.color)
                XenRenderer.text("Alpha ${alphaPercent()}%", infoX, pickerY + 92f, 12f, Theme.TextMuted.color)
            }

            XenRenderer.image(hueImage, x + 2f, hueY, width - 4f, 10f, 3f)
            XenRenderer.hollowRect(x + 2f, hueY, width - 4f, 10f, 1f, Theme.Primary.withAlpha(0.3f), 3f)
            XenRenderer.circle(x + 2f + hue * (width - 4f), hueY + 5f, 4f, 0xFFFFFFFF.toInt())

            val alphaStart = currentColor.rgb and 0x00FFFFFF
            val alphaEnd = currentColor.rgb or 0xFF000000.toInt()
            XenRenderer.gradientRect(x + 2f, alphaY, width - 4f, 10f, alphaStart, alphaEnd, XenGradient.LeftToRight, 3f)
            XenRenderer.hollowRect(x + 2f, alphaY, width - 4f, 10f, 1f, Theme.Primary.withAlpha(0.3f), 3f)
            XenRenderer.circle(x + 2f + alpha * (width - 4f), alphaY + 5f, 4f, 0xFFFFFFFF.toInt())

            return boundsHeight
        }

        override fun globalClick(mouseX: Float, mouseY: Float, button: Int) {
            if (dragMode == null && expanded && !isHovered(mouseX, mouseY)) {
                expanded = false
            }
        }

        override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
            if (button != 0) return false

            val previewX = x + width - 56f
            val previewY = y + 8f
            if (mouseX in previewX..(previewX + 48f) && mouseY in previewY..(previewY + 22f)) {
                expanded = !expanded
                return true
            }
            if (!expanded) return false

            val pickerX = x + 2f
            val pickerY = y + 40f
            val pickerSize = pickerSize()
            val hueY = pickerY + pickerSize + 8f
            val alphaY = hueY + 22f

            if (mouseX in pickerX..(pickerX + pickerSize) && mouseY in pickerY..(pickerY + pickerSize)) {
                dragMode = DragMode.PICKER
                return true
            }
            if (mouseX in (x + 2f)..(x + width - 2f) && mouseY in hueY..(hueY + 10f)) {
                dragMode = DragMode.HUE
                return true
            }
            if (mouseX in (x + 2f)..(x + width - 2f) && mouseY in alphaY..(alphaY + 10f)) {
                dragMode = DragMode.ALPHA
                return true
            }

            return false
        }

        override fun mouseReleased(button: Int) {
            dragMode = null
        }

        override fun isDragging(): Boolean = dragMode != null

        private fun syncColorState() {
            val color = currentValue() as? Color ?: (option.configElement.type as ElementType.ColorPicker).default
            if (color.rgb == lastArgb) return

            val hsb = Color.RGBtoHSB(color.red, color.green, color.blue, null)
            hue = hsb[0]
            saturation = hsb[1]
            brightness = hsb[2]
            alpha = color.alpha / 255f
            lastArgb = color.rgb
        }

        private fun updateFromMouse(mouseX: Float, mouseY: Float) {
            val pickerX = x + 2f
            val pickerY = y + 40f
            val pickerSize = pickerSize()
            val hueY = pickerY + pickerSize + 8f
            val alphaY = hueY + 22f

            when (dragMode) {
                DragMode.PICKER -> {
                    saturation = ((mouseX - pickerX) / pickerSize).coerceIn(0f, 1f)
                    brightness = (1f - (mouseY - pickerY) / pickerSize).coerceIn(0f, 1f)
                }
                DragMode.HUE -> {
                    hue = ((mouseX - (x + 2f)) / (width - 4f)).coerceIn(0f, 1f)
                }
                DragMode.ALPHA -> {
                    alpha = ((mouseX - (x + 2f)) / (width - 4f)).coerceIn(0f, 1f)
                }
                null -> Unit
            }
        }

        private fun pickerSize(): Float {
            val availableWidth = (if (width > 0f) width else ClickGUI.PANEL_WIDTH - 12f) - 4f
            return min(availableWidth, 112f).coerceAtLeast(84f)
        }

        private fun colorHex(color: Color): String =
            "#%02X%02X%02X".format(color.red, color.green, color.blue)

        private fun alphaPercent(): Int = (alpha * 100f).roundToInt().coerceIn(0, 100)

        private fun buildCurrentColor(): Color {
            val rgb = Color.HSBtoRGB(hue, saturation, brightness)
            return Color((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF, (alpha * 255f).roundToInt().coerceIn(0, 255))
        }

        private enum class DragMode {
            PICKER,
            HUE,
            ALPHA
        }
    }

    private class KeybindWidget(option: OptionElement) : SettingWidget(option) {
        private var listening = false

        override fun getHeight(): Float = 34f

        override fun render(x: Float, y: Float, width: Float, mouseX: Float, mouseY: Float): Float {
            this.x = x
            this.y = y
            this.width = width
            this.boundsHeight = getHeight()

            val value = (currentValue() as? Number)?.toInt() ?: (option.configElement.type as ElementType.Keybind).default
            val text = if (listening) "..." else getKeyName(value)
            val buttonWidth = max(62f, XenRenderer.textWidth(text, 15f) + 16f)

            XenRenderer.text(option.optionName, x + 2f, y + 9f, 16f, Theme.Text.color)
            field(x + width - buttonWidth, y + 6f, buttonWidth, 22f, listening)
            XenRenderer.text(text, x + width - buttonWidth + 8f, y + 10f, 15f, Theme.Text.color)
            return boundsHeight
        }

        override fun globalClick(mouseX: Float, mouseY: Float, button: Int) {
            if (listening && !isHovered(mouseX, mouseY)) {
                listening = false
            }
        }

        override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
            if (!isHovered(mouseX, mouseY)) return false
            if (listening) {
                update(-100 + button)
                listening = false
                return true
            }
            if (button == 0) {
                listening = true
                return true
            }
            return false
        }

        override fun keyPressed(input: KeyInput): Boolean {
            if (!listening) return false
            val value = when (input.key()) {
                GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_BACKSPACE -> -1
                GLFW.GLFW_KEY_ENTER -> {
                    listening = false
                    return true
                }
                else -> input.key()
            }
            update(value)
            listening = false
            return true
        }

        private fun getKeyName(keyCode: Int): String = when (keyCode) {
            0, -1 -> "Not Bound"
            in -100..-1 -> "Mouse ${keyCode + 100}"
            else -> InputUtil.fromKeyCode(KeyInput(keyCode, 0, 0)).localizedText.string
        }
    }

    private class ButtonWidget(option: OptionElement) : SettingWidget(option) {
        private val type = option.configElement.type as ElementType.Button

        override fun getHeight(): Float = 36f

        override fun render(x: Float, y: Float, width: Float, mouseX: Float, mouseY: Float): Float {
            this.x = x
            this.y = y
            this.width = width
            this.boundsHeight = getHeight()

            val hovered = isHovered(mouseX, mouseY)
            accentField(x + 2f, y + 6f, width - 4f, 24f, active = true, hovered = hovered)

            val textWidth = XenRenderer.textWidth(type.text, 16f)
            XenRenderer.text(type.text, x + width / 2f - textWidth / 2f, y + 10f, 16f, Theme.Text.color)
            return boundsHeight
        }

        override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
            if (button != 0 || !isHovered(mouseX, mouseY)) return false
            type.onClick()
            return true
        }
    }

    private class TextParagraphWidget(option: OptionElement) : SettingWidget(option) {
        private val type = option.configElement.type as ElementType.TextParagraph

        override fun getHeight(): Float {
            val bounds = XenRenderer.wrappedTextBounds(type.text, max(width - 4f, 10f), 14f)
            return (bounds[3] - bounds[1] + 12f).coerceAtLeast(24f)
        }

        override fun render(x: Float, y: Float, width: Float, mouseX: Float, mouseY: Float): Float {
            this.x = x
            this.y = y
            this.width = width
            this.boundsHeight = getHeight()
            XenRenderer.drawWrappedString(type.text, x + 2f, y + 4f, width - 4f, 14f, Theme.TextMuted.color)
            return boundsHeight
        }
    }

    private class MultiCheckboxWidget(option: OptionElement) : SettingWidget(option) {
        private var expanded = false

        override fun getHeight(): Float {
            val type = option.configElement.type as ElementType.MultiCheckbox
            return STACKED_FIELD_BASE_HEIGHT + if (expanded) type.options.size * CHOICE_ROW_HEIGHT else 0f
        }

        override fun render(x: Float, y: Float, width: Float, mouseX: Float, mouseY: Float): Float {
            val type = option.configElement.type as ElementType.MultiCheckbox
            this.x = x
            this.y = y
            this.width = width
            this.boundsHeight = getHeight()

            val selected = (currentValue() as? Set<*>)?.mapNotNull { (it as? Number)?.toInt() }?.toSet() ?: type.default
            XenRenderer.text(option.optionName, x + 2f, y + 2f, 16f, Theme.Text.color)
            accentField(x + 2f, y + STACKED_FIELD_OFFSET_Y, width - 4f, 22f, expanded)
            XenRenderer.text("${selected.size} selected", x + 10f, y + STACKED_FIELD_OFFSET_Y + 4f, 15f, Theme.Text.color)

            if (expanded) {
                val listX = x + 2f
                val listY = y + STACKED_FIELD_BASE_HEIGHT
                val listWidth = width - 4f
                val listHeight = type.options.size * CHOICE_ROW_HEIGHT

                XenRenderer.rect(listX, listY, listWidth, listHeight, Theme.BgLight.color, FIELD_CUT)
                for ((index, value) in type.options.withIndex()) {
                    val optionY = listY + index * CHOICE_ROW_HEIGHT
                    val selectedOption = index in selected
                    val optionHovered = mouseX in listX..(listX + listWidth) && mouseY in optionY..(optionY + CHOICE_ROW_HEIGHT)
                    if (optionHovered) {
                        XenRenderer.rect(listX, optionY, listWidth, CHOICE_ROW_HEIGHT, Theme.shift(Theme.BgLight.color, 1.08f))
                    } else if (selectedOption) {
                        XenRenderer.rect(listX, optionY, listWidth, CHOICE_ROW_HEIGHT, Theme.Primary.withAlpha(0.14f))
                    }
                    if (index < type.options.lastIndex) {
                        XenRenderer.line(
                            listX + 8f,
                            optionY + CHOICE_ROW_HEIGHT,
                            listX + listWidth - 8f,
                            optionY + CHOICE_ROW_HEIGHT,
                            1f,
                            Theme.BorderMuted.color
                        )
                    }
                    XenRenderer.rect(x + 8f, optionY + 4f, 12f, 12f, if (selectedOption) Theme.Primary.color else Theme.Bg.color, 3f)
                    XenRenderer.text(value, x + 26f, optionY + 3f, 14f, Theme.Text.color)
                }
            }

            return boundsHeight
        }

        override fun globalClick(mouseX: Float, mouseY: Float, button: Int) {
            if (expanded && !isHovered(mouseX, mouseY)) {
                expanded = false
            }
        }

        override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
            if (button != 0) return false
            val type = option.configElement.type as ElementType.MultiCheckbox
            if (mouseX in (x + 2f)..(x + width - 2f) && mouseY in (y + STACKED_FIELD_OFFSET_Y)..(y + STACKED_FIELD_OFFSET_Y + 22f)) {
                expanded = !expanded
                return true
            }
            if (!expanded) return false

            val current = (currentValue() as? Set<*>)?.mapNotNull { (it as? Number)?.toInt() }?.toMutableSet() ?: type.default.toMutableSet()
            var optionY = y + STACKED_FIELD_BASE_HEIGHT
            for (index in type.options.indices) {
                if (mouseX in (x + 2f)..(x + width - 2f) && mouseY in optionY..(optionY + CHOICE_ROW_HEIGHT)) {
                    if (!current.add(index)) current.remove(index)
                    update(current.toSet())
                    return true
                }
                optionY += CHOICE_ROW_HEIGHT
            }
            return false
        }
    }

    private class MCColorWidget(option: OptionElement) : SettingWidget(option) {
        override fun getHeight(): Float = 64f

        override fun render(x: Float, y: Float, width: Float, mouseX: Float, mouseY: Float): Float {
            this.x = x
            this.y = y
            this.width = width
            this.boundsHeight = getHeight()

            val selected = currentValue() as? MCColorCode ?: (option.configElement.type as ElementType.MCColorPicker).default
            XenRenderer.text(option.optionName, x + 2f, y + 2f, 16f, Theme.Text.color)

            var colorX = x + 2f
            var colorY = y + 24f
            for ((index, mcColor) in MCColorCode.entries.withIndex()) {
                val border = if (mcColor == selected) Theme.Primary.color else 0x00000000
                XenRenderer.rect(colorX, colorY, 13f, 13f, mcColor.color.rgb, 3f)
                if (border != 0) {
                    XenRenderer.hollowRect(colorX, colorY, 13f, 13f, 1.5f, border, 3f)
                }
                colorX += 15f
                if ((index + 1) % 8 == 0) {
                    colorX = x + 2f
                    colorY += 15f
                }
            }

            return boundsHeight
        }

        override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
            if (button != 0 || !isHovered(mouseX, mouseY)) return false

            var colorX = x + 2f
            var colorY = y + 24f
            for ((index, mcColor) in MCColorCode.entries.withIndex()) {
                if (mouseX in colorX..(colorX + 13f) && mouseY in colorY..(colorY + 13f)) {
                    update(mcColor)
                    return true
                }
                colorX += 15f
                if ((index + 1) % 8 == 0) {
                    colorX = x + 2f
                    colorY += 15f
                }
            }
            return false
        }
    }

    private fun createWidget(option: OptionElement): SettingWidget {
        return when (option.configElement.type) {
            is ElementType.Switch -> SwitchWidget(option)
            is ElementType.Slider -> SliderWidget(option)
            is ElementType.Dropdown -> DropdownWidget(option)
            is ElementType.TextInput -> TextInputWidget(option)
            is ElementType.ColorPicker -> ColorWidget(option)
            is ElementType.Keybind -> KeybindWidget(option)
            is ElementType.Button -> ButtonWidget(option)
            is ElementType.TextParagraph -> TextParagraphWidget(option)
            is ElementType.MultiCheckbox -> MultiCheckboxWidget(option)
            is ElementType.MCColorPicker -> MCColorWidget(option)
        }
    }

    private fun formatSliderValue(value: Double, showDouble: Boolean): String {
        return if (showDouble) {
            if (value % 1.0 == 0.0) "%.1f".format(value) else "%.2f".format(value)
        } else {
            value.roundToInt().toString()
        }
    }

    private fun roundToStep(value: Double, step: Double): Double =
        (value / step).roundToInt() * step

    private fun Int.withAlpha(alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255f).roundToInt()
        return (this and 0x00FFFFFF) or (a shl 24)
    }
}
