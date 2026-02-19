package xenus.xenaddons.gui.managers.config

class FeatureElement(
    val featureName: String,
    val description: String,
    val configElement: ConfigElement
) : ConfigContainer {
    val options: MutableMap<String, MutableList<OptionElement>> = mutableMapOf()

    fun addFeatureOption(
        optionName: String,
        sectionName: String = "Options",
        element: ConfigElement
    ): FeatureElement {
        val option = OptionElement(optionName, element)
        option.configElement.parent = option

        val optionsList = options.getOrPut(sectionName) { mutableListOf() }
        if (!optionsList.any { it.optionName == optionName }) optionsList.add(option)

        ConfigManager.ensureDefaultValue(element)

        return this
    }
}
