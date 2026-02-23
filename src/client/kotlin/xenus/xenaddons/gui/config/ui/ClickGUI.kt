package xenus.xenaddons.gui.config.ui

import xyz.meowing.vexel.core.VexelScreen
import xenus.xenaddons.gui.managers.config.ConfigManager
import xenus.xenaddons.gui.config.ui.elements.base.ElementType
import xenus.xenaddons.gui.managers.config.ConfigElement
import xenus.xenaddons.gui.managers.config.CategoryElement
import xenus.xenaddons.gui.utils.toColorFromMap
import xenus.xenaddons.gui.config.ui.panels.Panel
import xenus.xenaddons.gui.config.ui.elements.MCColorCode
import xenus.xenaddons.gui.config.ui.elements.FeatureTooltip
import xenus.xenaddons.gui.ui.Theme
import java.awt.Color
import kotlin.math.sign

typealias ConfigData = Map<String, Any>

object ClickGUI : VexelScreen("Zen Config") {
    private val panels = mutableListOf<Panel>()

    private val categoryOrder = listOf(
        "misc", "dungeons", "dev"
    )

    private lateinit var searchBar: SearchBar
    lateinit var featureTooltip: FeatureTooltip

    private val configListeners = mutableMapOf<String, MutableList<(Any) -> Unit>>()
    private val closeListeners = mutableListOf<() -> Unit>()
    private val elementRefs = mutableMapOf<String, ConfigElement>()

    override fun afterInitialization() {
        panels.clear()

        val sortedCategories = ConfigManager.configTree.sortedWith(
            compareBy<CategoryElement> { cat ->
                categoryOrder.indexOf(cat.name.lowercase()).takeIf { it >= 0 } ?: Int.MAX_VALUE
            }.thenBy { it.name }
        )

        sortedCategories.forEachIndexed { index, category ->
            val col = index % 7
            val row = index / 7

            val x = if (row > 0) 50f + (col + 1) * 260f else 50f + col * 260f
            val y = 50f + row * 400f

            val panel = Panel(category, x, y, ::updateConfig)
            panel.childOf(window)
            panels.add(panel)
        }

        searchBar = SearchBar { query ->
            filterPanels(query)
        }

        searchBar.childOf(window)

        featureTooltip = FeatureTooltip()
        featureTooltip.childOf(window)
    }

    fun updateTooltip(description: String) {
        if (::featureTooltip.isInitialized) featureTooltip.setText(description)
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
            val resolvedValue = when (currentValue) {
                is Map<*, *> -> currentValue.toColorFromMap()
                is List<*> -> currentValue.mapNotNull { (it as? Number)?.toInt() }.toSet()
                else -> currentValue
            }
            resolvedValue?.let { listener(it) }
        }
        return this
    }

    fun registerCloseListener(listener: () -> Unit): ClickGUI {
        closeListeners.add(listener)
        return this
    }

    fun getConfigValue(configKey: String): Any? {
        val value = ConfigManager.getConfigValue(configKey) ?: return null
        return when (value) {
            is Map<*, *> -> value.toColorFromMap()
            is List<*> -> value.mapNotNull { (it as? Number)?.toInt() }.toSet()
            else -> value
        }
    }

    private fun filterPanels(query: String) {
        if (query.isEmpty()) {
            panels.forEach { it.visible = true }
        } else {
            panels.forEach { panel ->
                panel.visible = panel.matchesSearch(query)
            }
        }
    }

    override fun onMouseScroll(horizontal: Double, vertical: Double) {
        val actualAmount = (vertical.sign * 16f).toFloat()
        for (i in panels.size - 1 downTo 0) {
            if (panels[i].handleScroll(actualAmount)) {
                return
            }
        }
        super.onMouseScroll(horizontal, vertical)
    }

    override fun onCloseGui() {
        ConfigManager.saveConfig(true)
        closeListeners.forEach { it() }
        super.onCloseGui()
    }
}
