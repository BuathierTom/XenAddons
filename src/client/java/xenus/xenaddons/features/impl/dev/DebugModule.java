package xenus.xenaddons.features.impl.dev;

import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.utils.CommandTreeRefresher;

import java.util.function.Predicate;

public class DebugModule extends Module {

    public final BooleanSetting enableTitleTest = new BooleanSetting("/xa title", true);
    public final BooleanSetting enablePetRuleTest = new BooleanSetting("/xa petrule", true);
    public final BooleanSetting enableTiclassTest = new BooleanSetting("/xa ticlass", true);
    public final BooleanSetting enableSecretRoomTest = new BooleanSetting("/xa secretroom", true);
    public final BooleanSetting enableProcsTest = new BooleanSetting("/xa procs", true);
    public final BooleanSetting enableGateTest = new BooleanSetting("/xa gate", true);
    public final BooleanSetting enableMilestoneTest = new BooleanSetting("/xa milestone", true);
    public final BooleanSetting enablePfStatsTest = new BooleanSetting("/xa pf [pseudo]", true);

    public DebugModule() {
        super("Debug", "Dev tools for testing features", Category.DEV);
        this.registerSetting(enableTitleTest);
        this.registerSetting(enablePetRuleTest);
        this.registerSetting(enableTiclassTest);
        this.registerSetting(enableSecretRoomTest);
        this.registerSetting(enableProcsTest);
        this.registerSetting(enableGateTest);
        this.registerSetting(enableMilestoneTest);
        this.registerSetting(enablePfStatsTest);

        // Each toggle changes which subcommands /xa should suggest, so rebuild the tree on change.
        for (xenus.xenaddons.settings.Setting<?> setting : this.settings) {
            setting.setListener(value -> CommandTreeRefresher.refresh());
        }
    }

    @Override
    public void onEnable() {
        CommandTreeRefresher.refresh();
    }

    @Override
    public void onDisable() {
        CommandTreeRefresher.refresh();
    }

    /**
     * Whether a dev subcommand should exist at all: the module has to be on, and so does its own
     * toggle. Used as the commands' requires() predicate, so a disabled one is neither suggested
     * nor runnable.
     */
    public static boolean allows(Predicate<DebugModule> toggle) {
        DebugModule debug = ModuleManager.getModule(DebugModule.class);
        return debug != null && debug.isEnabled() && toggle.test(debug);
    }
}
