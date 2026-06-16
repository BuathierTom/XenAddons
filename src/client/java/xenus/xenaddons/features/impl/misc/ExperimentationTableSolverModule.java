package xenus.xenaddons.features.impl.misc;

import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.settings.BooleanSetting;

public class ExperimentationTableSolverModule extends Module {
    public final BooleanSetting keepItemsVisible = new BooleanSetting("Keep Items Visible", true);
    private static boolean initialized = false;

    public ExperimentationTableSolverModule() {
        super("Exp Table Addons", "Quality of life additions for the Experimentation Table", Category.MISC);
        this.registerSetting(keepItemsVisible);
        if (!initialized) {
            initialized = true;
            ExperimentationTableSolverTracker.init();
        }
    }

    public static ExperimentationTableSolverModule get() {
        return ModuleManager.getModule(ExperimentationTableSolverModule.class);
    }

    public static boolean isActive() {
        ExperimentationTableSolverModule module = get();
        return module != null && module.isEnabled();
    }
}
