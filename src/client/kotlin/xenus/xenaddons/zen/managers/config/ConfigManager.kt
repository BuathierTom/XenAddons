@file:Suppress("UNCHECKED_CAST")

package xenus.xenaddons.zen.managers.config

import net.minecraft.client.MinecraftClient
import xenus.xenaddons.features.Category
import xenus.xenaddons.features.ModuleManager
import xenus.xenaddons.zen.config.ui.ClickGUI
import xenus.xenaddons.zen.config.ui.elements.base.ElementType
import xenus.xenaddons.settings.ActionSetting
import xenus.xenaddons.settings.BooleanSetting
import xenus.xenaddons.settings.ColorSetting
import xenus.xenaddons.settings.KeybindSetting
import xenus.xenaddons.settings.ModeSetting
import xenus.xenaddons.settings.NumberSetting
import xenus.xenaddons.settings.Setting
import java.awt.Color

object ConfigManager {
    val configValueMap: MutableMap<String, Any> = mutableMapOf()
    val configTree: MutableList<CategoryElement> = mutableListOf()

    private val settingRefs: MutableMap<String, Setting<*>> = mutableMapOf()
    private val moduleEnabledRefs: MutableMap<String, xenus.xenaddons.features.Module> = mutableMapOf()
    private val categoryOrder = listOf("misc", "dungeons", "dev")

    fun rebuildFromXenModules() {
        configValueMap.clear()
        configTree.clear()
        settingRefs.clear()
        moduleEnabledRefs.clear()
        val settingKeys: MutableMap<Setting<*>, String> = mutableMapOf()

        for (module in ModuleManager.modules.sortedBy { it.name.lowercase() }) {
            if (module.name.equals("Edit HUD", ignoreCase = true)) continue
            if (module.name.equals("Custom Keybinds", ignoreCase = true)) continue

            val categoryName = mapCategory(module.category)
            val featureName = if (module.name.equals("Colors", ignoreCase = true)) "Config GUI" else module.name
            val featureDescription = if (module.name.equals("Colors", ignoreCase = true)) {
                "Customize config GUI appearance"
            } else {
                module.description
            }
            val moduleKey = "module.${sanitize(module.name)}.enabled"
            val feature = addFeature(
                featureName,
                featureDescription,
                categoryName,
                ConfigElement(moduleKey, ElementType.Switch(module.isEnabled))
            )

            configValueMap[moduleKey] = module.isEnabled
            moduleEnabledRefs[moduleKey] = module

            for (setting in module.settings) {
                if (setting.name.startsWith("HUD ", ignoreCase = true)) continue

                val key = "module.${sanitize(module.name)}.setting.${sanitize(setting.name)}"
                val parentSetting = setting.parentSetting
                val parentKey = parentSetting?.let { settingKeys[it] }
                val sectionName = resolveSectionName(module.name, setting, parentSetting)
                val element = toElement(module.name, setting, key, parentKey) ?: continue
                feature.addFeatureOption(setting.name, sectionName, element)
                configValueMap[key] = readSettingValue(setting)
                settingRefs[key] = setting
                settingKeys[setting] = key
            }
        }
    }

    private fun mapCategory(category: Category): String {
        return when (category) {
            Category.MISC -> "Misc"
            Category.DUNGEON -> "Dungeons"
            Category.DEV -> "Dev"
        }
    }

    private fun sanitize(value: String): String {
        return value.lowercase().replace("[^a-z0-9]+".toRegex(), "_").trim('_')
    }

    private fun resolveSectionName(
        moduleName: String,
        setting: Setting<*>,
        parentSetting: Setting<*>?
    ): String {
        if (parentSetting != null) return parentSetting.name

        if (moduleName.equals("ZguegAddons", ignoreCase = true)) {
            return when {
                setting.name.equals("Crypts Notify", ignoreCase = true) ||
                    setting.name.equals("Class Notify", ignoreCase = true) -> "Notify"
                setting.name.equals("Radio Msg", ignoreCase = true) ||
                    setting.name.equals("Discord Warn", ignoreCase = true) -> "Chat Hider"
                setting.name.startsWith("!", ignoreCase = true) -> "Commands"
                else -> "Options"
            }
        }

        if (moduleName.equals("Colors", ignoreCase = true)) {
            return "Config GUI"
        }

        if (moduleName.equals("SecretRoutes", ignoreCase = true)) {
            return when {
                setting.name.equals("Etherwarps", ignoreCase = true) ||
                    setting.name.equals("Mines", ignoreCase = true) ||
                    setting.name.equals("Interacts", ignoreCase = true) ||
                    setting.name.equals("Superbooms", ignoreCase = true) ||
                    setting.name.equals("Enderpearls", ignoreCase = true) ||
                    setting.name.equals("Item secrets", ignoreCase = true) ||
                    setting.name.equals("Interact secrets", ignoreCase = true) ||
                    setting.name.equals("Bat secrets", ignoreCase = true) -> "Render Toggles"
                else -> "Options"
            }
        }

        return "Options"
    }

    private fun toElement(moduleName: String, setting: Setting<*>, key: String, parentKey: String?): ConfigElement? {
        val type = when (setting) {
            is BooleanSetting -> ElementType.Switch(setting.value)
            is NumberSetting -> ElementType.Slider(setting.min, setting.max, setting.value, false)
            is ModeSetting -> ElementType.Dropdown(setting.modes, setting.index.coerceAtLeast(0))
            is ColorSetting -> ElementType.ColorPicker(Color(setting.value, true))
            is KeybindSetting -> ElementType.Keybind(setting.value)
            is ActionSetting -> {
                val buttonText = when {
                    moduleName.equals("InvButtons", ignoreCase = true) &&
                        setting.name.equals("Edit Buttons Position", ignoreCase = true) -> "Edit buttons"
                    moduleName.equals("InvButtons", ignoreCase = true) &&
                        setting.name.equals("Profiles & Advanced", ignoreCase = true) -> "Configuration menu"
                    else -> setting.name
                }
                ElementType.Button(buttonText) { setting.run() }
            }
            else -> null
        } ?: return null
        val visibilityPredicate: (Map<String, Any>) -> Boolean = if (parentKey == null) {
            { setting.isVisible() }
        } else {
            { data -> setting.isVisible() && (data[parentKey] as? Boolean) == true }
        }
        return ConfigElement(key, type, shouldShow = visibilityPredicate)
    }

    private fun readSettingValue(setting: Setting<*>): Any {
        return when (setting) {
            is ColorSetting -> Color(setting.value, true)
            else -> setting.value as Any
        }
    }

    fun addFeature(featureName: String, description: String, categoryName: String, element: ConfigElement): FeatureElement {
        val category = configTree.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }
            ?: CategoryElement(categoryName).also { configTree.add(it) }

        configTree.sortWith(
            compareBy<CategoryElement> { cat ->
                categoryOrder.indexOf(cat.name.lowercase()).takeIf { it >= 0 } ?: Int.MAX_VALUE
            }.thenBy { it.name }
        )

        val featureElement = FeatureElement(featureName, description, element)
        featureElement.configElement.parent = featureElement

        if (!category.features.any { it.featureName == featureName }) {
            category.features.add(featureElement)
        }

        ensureDefaultValue(element)
        return featureElement
    }

    fun ensureDefaultValue(element: ConfigElement) {
        if (configValueMap.containsKey(element.configKey)) return
        val defaultValue = when (val type = element.type) {
            is ElementType.Switch -> type.default
            is ElementType.Slider -> type.default
            is ElementType.Dropdown -> type.default
            is ElementType.TextInput -> type.default
            is ElementType.ColorPicker -> type.default
            is ElementType.Keybind -> type.default
            is ElementType.MultiCheckbox -> type.default
            is ElementType.MCColorPicker -> type.default
            is ElementType.Button, is ElementType.TextParagraph -> null
        }
        if (defaultValue != null) {
            configValueMap[element.configKey] = defaultValue
        }
    }

    fun saveConfig(force: Boolean) {
        for ((key, value) in configValueMap) {
            val module = moduleEnabledRefs[key]
            if (module != null) {
                if (value is Boolean) module.setEnabled(value)
                continue
            }

            val setting = settingRefs[key] ?: continue
            applySettingValue(setting, value)
        }

        xenus.xenaddons.config.ConfigManager.save()
    }

    private fun applySettingValue(setting: Setting<*>, value: Any) {
        when (setting) {
            is BooleanSetting -> if (value is Boolean) setting.setValue(value)
            is NumberSetting -> when (value) {
                is Number -> setting.setValue(value.toDouble())
            }
            is ModeSetting -> when (value) {
                is Int -> {
                    val idx = value.coerceIn(0, setting.modes.size - 1)
                    setting.setValue(setting.modes[idx])
                }
                is String -> setting.setValue(value)
            }
            is ColorSetting -> when (value) {
                is Color -> setting.setValue(value.rgb)
                is Map<*, *> -> {
                    val r = (value["r"] as? Number)?.toInt() ?: 255
                    val g = (value["g"] as? Number)?.toInt() ?: 255
                    val b = (value["b"] as? Number)?.toInt() ?: 255
                    val a = (value["a"] as? Number)?.toInt() ?: 255
                    setting.setValue(Color(r, g, b, a).rgb)
                }
            }
            is KeybindSetting -> if (value is Number) setting.setValue(value.toInt())
        }
    }

    fun getConfigValue(configKey: String): Any? {
        val value = configValueMap[configKey]
        return when (value) {
            is List<*> -> value.mapNotNull { (it as? Number)?.toInt() }.toSet()
            else -> value
        }
    }

    @JvmStatic
    fun openConfig() {
        rebuildFromXenModules()
        MinecraftClient.getInstance().setScreen(ClickGUI)
    }
}
