@file:Suppress("UNCHECKED_CAST")

package xenus.xenaddons.zen.config.ui.elements.base

import xyz.meowing.vexel.components.base.VexelElement
import xenus.xenaddons.zen.managers.config.ConfigElement
import xenus.xenaddons.zen.managers.config.ConfigManager
import xenus.xenaddons.zen.managers.config.OptionElement
import xenus.xenaddons.zen.config.ui.elements.MCColorCode
import xenus.xenaddons.zen.config.ui.elements.base.ElementType
import xenus.xenaddons.zen.config.ui.elements.ButtonElement
import xenus.xenaddons.zen.config.ui.elements.ColorPickerElement
import xenus.xenaddons.zen.config.ui.elements.DropdownElement
import xenus.xenaddons.zen.config.ui.elements.KeybindElement
import xenus.xenaddons.zen.config.ui.elements.MCColorPickerElement
import xenus.xenaddons.zen.config.ui.elements.MultiCheckboxElement
import xenus.xenaddons.zen.config.ui.elements.SliderElement
import xenus.xenaddons.zen.config.ui.elements.SwitchElement
import xenus.xenaddons.zen.config.ui.elements.TextInputElement
import xenus.xenaddons.zen.config.ui.elements.TextParagraphElement
import java.awt.Color

class ElementRenderer(
    private val element: ConfigElement,
    private val onConfigUpdate: (String, Any) -> Unit
) {
    var currentElement: VexelElement<*>? = null
        private set

    fun createAndAttach(parent: VexelElement<*>): VexelElement<*>? {
        val optionName = (element.parent as? OptionElement)?.optionName ?: ""
        currentElement = when (val type = element.type) {
            is ElementType.Switch -> {
                SwitchElement(
                    optionName,
                    ConfigManager.getConfigValue(element.configKey) as? Boolean ?: type.default
                ).apply {
                    switch.onValueChange { newValue ->
                        onConfigUpdate(element.configKey, newValue)
                    }
                }
            }
            is ElementType.Slider -> {
                SliderElement(
                    optionName,
                    ConfigManager.getConfigValue(element.configKey) as? Double ?: type.default,
                    type.min,
                    type.max,
                    type.showDouble
                ).apply {
                    slider.onValueChange { newValue ->
                        val actualValue = type.min + (newValue as Float) * (type.max - type.min)
                        onConfigUpdate(element.configKey, actualValue)
                    }
                }
            }
            is ElementType.Dropdown -> {
                DropdownElement(
                    optionName,
                    type.options,
                    ConfigManager.getConfigValue(element.configKey) as? Int ?: type.default
                ).apply {
                    onValueChange { newValue ->
                        onConfigUpdate(element.configKey, newValue)
                    }
                }
            }
            is ElementType.TextInput -> {
                TextInputElement(
                    optionName,
                    ConfigManager.getConfigValue(element.configKey) as? String ?: type.default,
                    type.placeholder
                ).apply {
                    input.onValueChange { newValue ->
                        onConfigUpdate(element.configKey, newValue)
                    }
                }
            }
            is ElementType.ColorPicker -> {
                ColorPickerElement(
                    optionName,
                    ConfigManager.getConfigValue(element.configKey) as? Color ?: type.default
                ).apply {
                    onValueChange { newValue ->
                        onConfigUpdate(element.configKey, newValue)
                    }
                }
            }
            is ElementType.Keybind -> {
                KeybindElement(
                    optionName,
                    ConfigManager.getConfigValue(element.configKey) as? Int ?: type.default
                ).apply {
                    onValueChange { newValue ->
                        onConfigUpdate(element.configKey, newValue)
                    }
                }
            }
            is ElementType.MultiCheckbox -> {
                MultiCheckboxElement(
                    optionName,
                    type.options,
                    ConfigManager.getConfigValue(element.configKey) as? Set<Int> ?: type.default
                ).apply {
                    onValueChange { newValue ->
                        onConfigUpdate(element.configKey, newValue)
                    }
                }
            }
            is ElementType.MCColorPicker -> {
                MCColorPickerElement(
                    optionName,
                    ConfigManager.getConfigValue(element.configKey) as? MCColorCode ?: type.default
                ).apply {
                    onValueChange { newValue ->
                        onConfigUpdate(element.configKey, newValue)
                    }
                }
            }
            is ElementType.Button -> {
                ButtonElement(type.text) { type.onClick() }
            }
            is ElementType.TextParagraph -> {
                TextParagraphElement(type.text)
            }
        }
        currentElement?.childOf(parent)
        return currentElement
    }

    fun shouldShow(): Boolean {
        return element.shouldShow(ConfigManager.configValueMap)
    }
}