package xenus.xenaddons.features.impl.fishing;

import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;

public class HideOtherBobbersModule extends Module {

    public HideOtherBobbersModule() {
        super("Hide Other Bobbers", "Hides other players' fishing bobbers", Category.FISHING);
    }

    public static boolean isActive() {
        HideOtherBobbersModule module = ModuleManager.getModule(HideOtherBobbersModule.class);
        return module != null && module.isEnabled();
    }
}
