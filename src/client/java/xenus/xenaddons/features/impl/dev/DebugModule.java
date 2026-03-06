package xenus.xenaddons.features.impl.dev;

import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.BooleanSetting;

public class DebugModule extends Module {

    public final BooleanSetting enableTitleTest = new BooleanSetting("/xa title", true);
    public final BooleanSetting enablePetRuleTest = new BooleanSetting("/xa petrule", true);
    public final BooleanSetting enableTiclassTest = new BooleanSetting("/xa ticlass", true);
    public final BooleanSetting enableSecretRoomTest = new BooleanSetting("/xa secretroom", true);
    public final BooleanSetting enableProcsTest = new BooleanSetting("/xa procs", true);
    public final BooleanSetting enablePfStatsTest = new BooleanSetting("/xa pf [pseudo]", true);

    public DebugModule() {
        super("Debug", "Dev tools for testing features", Category.DEV);
        this.registerSetting(enableTitleTest);
        this.registerSetting(enablePetRuleTest);
        this.registerSetting(enableTiclassTest);
        this.registerSetting(enableSecretRoomTest);
        this.registerSetting(enableProcsTest);
        this.registerSetting(enablePfStatsTest);
    }
}
