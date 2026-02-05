package xenus.xenaddons.features.impl.dev;

import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.NumberSetting;

public class ColorsModule extends Module {
    public final NumberSetting red = new NumberSetting("Red", 0, 0, 255, 1);
    public final NumberSetting green = new NumberSetting("Green", 170, 0, 255, 1);
    public final NumberSetting blue = new NumberSetting("Blue", 255, 0, 255, 1);

    public ColorsModule() {
        super("Colors", "Change the GUI theme color", Category.DEV);
        this.registerSetting(red);
        this.registerSetting(green);
        this.registerSetting(blue);
    }

    public int getColor() {
        int r = red.getValue().intValue();
        int g = green.getValue().intValue();
        int b = blue.getValue().intValue();
        return (255 << 24) | (r << 16) | (g << 8) | b;
    }
}
