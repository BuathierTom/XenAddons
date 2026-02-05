package xenus.xenaddons.features.impl.dev;

import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.ColorSetting;

public class ColorsModule extends Module {
    public final ColorSetting menuColor = new ColorSetting("Menu Color", 0xFFA020F0);

    public ColorsModule() {
        super("Colors", "Change the GUI theme color", Category.DEV);
        this.registerSetting(menuColor);
    }

    public int getColor() {
        return menuColor.getValue();
    }
}
