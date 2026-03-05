package xenus.xenaddons.features.impl.misc;

import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.settings.ActionSetting;
import xenus.xenaddons.settings.ModeSetting;
import xenus.xenaddons.settings.NumberSetting;

import java.util.List;

public class CustomHyperionSoundModule extends Module {
    public final ModeSetting sound = new ModeSetting(
            "Sound",
            "Iron Xylophone",
            List.of(
                    "Blaze Hurt",
                    "XP Pickup",
                    "Vault Break",
                    "Elder Hurt",
                    "Totem Use",
                    "Sculk Hit",
                    "Ender Chest",
                    "Iron Xylophone"
            )
    );
    public final NumberSetting volume = new NumberSetting("Volume", 1.0, 0.0, 2.0, 0.1);
    public final NumberSetting pitch = new NumberSetting("Pitch", 1.0, 0.0, 2.0, 0.1);
    public final ActionSetting playSound = new ActionSetting("Play Sound", CustomHyperionSoundHandler::playPreviewSound);

    public CustomHyperionSoundModule() {
        super("Custom Hyperon Sound", "Changes Wither Blade explosion sound to a custom one", Category.MISC);
        this.registerSetting(sound);
        this.registerSetting(volume);
        this.registerSetting(pitch);
        this.registerSetting(playSound);
    }

    public static CustomHyperionSoundModule get() {
        return ModuleManager.getModule(CustomHyperionSoundModule.class);
    }

    public static boolean isActive() {
        CustomHyperionSoundModule module = get();
        return module != null && module.isEnabled();
    }
}
