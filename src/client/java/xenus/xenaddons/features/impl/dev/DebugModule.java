package xenus.xenaddons.features.impl.dev;

import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.BooleanSetting;

public class DebugModule extends Module {

    public final BooleanSetting enableTitleTest = new BooleanSetting("/xa title", true);
    public final BooleanSetting enablePetRuleTest = new BooleanSetting("/xa petrule", true);

    public DebugModule() {
        super("Debug", "Dev tools for testing features", Category.DEV);
        this.registerSetting(enableTitleTest);
        this.registerSetting(enablePetRuleTest);
    }
}
