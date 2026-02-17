package xenus.xenaddons.features.impl.misc;

import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.BooleanSetting;

public class CustomSizeModule extends Module {
    public final BooleanSetting inDungeonOnly = new BooleanSetting("In Dungeon Only", false);

    public CustomSizeModule() {
        super("Custom Size", "Apply custom player scale", Category.MISC);
        this.registerSetting(inDungeonOnly);
    }
}
