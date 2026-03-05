package xenus.xenaddons.features.impl.misc;

import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.settings.BooleanSetting;

public class CooldownItemsModule extends Module {
    public final BooleanSetting showWhenReady = new BooleanSetting("Show When Ready", true);
    public final BooleanSetting cooldownBackground = new BooleanSetting("Cooldown Background", false);
    public final BooleanSetting seeInInventory = new BooleanSetting("See In Inventory", true);
    private static boolean initialized = false;

    public CooldownItemsModule() {
        super("Cooldown Items", "Show item ability cooldowns like SkyHanni", Category.MISC);
        this.registerSetting(showWhenReady);
        this.registerSetting(cooldownBackground);
        this.registerSetting(seeInInventory);
        if (!initialized) {
            initialized = true;
            CooldownItemsTracker.init();
        }
    }

    public static CooldownItemsModule get() {
        return ModuleManager.getModule(CooldownItemsModule.class);
    }

    public static boolean isActive() {
        CooldownItemsModule module = get();
        return module != null && module.isEnabled();
    }
}
