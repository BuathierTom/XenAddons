package xenus.xenaddons.gui.managers.config

import xenus.xenaddons.gui.config.ui.ConfigData
import xenus.xenaddons.gui.config.ui.elements.base.ElementType

data class ConfigElement(
    val configKey: String,
    val type: ElementType,
    val shouldShow: (ConfigData) -> Boolean = { true },
    val value: Any? = null,
) {
    var parent: ConfigContainer? = null
}