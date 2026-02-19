package xenus.xenaddons.zen.managers.config

import xenus.xenaddons.zen.config.ui.ConfigData
import xenus.xenaddons.zen.config.ui.elements.base.ElementType

data class ConfigElement(
    val configKey: String,
    val type: ElementType,
    val shouldShow: (ConfigData) -> Boolean = { true },
    val value: Any? = null,
) {
    var parent: ConfigContainer? = null
}