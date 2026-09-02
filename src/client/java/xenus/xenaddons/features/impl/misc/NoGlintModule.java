package xenus.xenaddons.features.impl.misc;

import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;

public class NoGlintModule extends Module {

    public NoGlintModule() {
        super("No Glint", "Removes the enchantment glint from every item", Category.MISC);
    }

    public static boolean isActive() {
        NoGlintModule module = ModuleManager.getModule(NoGlintModule.class);
        return module != null && module.isEnabled();
    }
}
