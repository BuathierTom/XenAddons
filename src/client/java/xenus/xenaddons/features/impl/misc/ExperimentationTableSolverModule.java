package xenus.xenaddons.features.impl.misc;

import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.settings.BooleanSetting;

public class ExperimentationTableSolverModule extends Module {
    public final BooleanSetting highlightNextClick = new BooleanSetting("Highlight Next Click", true);
    public final BooleanSetting preventMisclicks = new BooleanSetting("Prevent Misclicks", true);
    public final BooleanSetting keepItemsVisible = new BooleanSetting("Keep Items Visible", true);
    private static boolean initialized = false;

    public ExperimentationTableSolverModule() {
        super("Exp Table Solver", "Solver for Chronomatron and Ultrasequencer", Category.MISC);
        this.registerSetting(highlightNextClick);
        this.registerSetting(preventMisclicks);
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
