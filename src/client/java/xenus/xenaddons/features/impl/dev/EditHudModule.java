package xenus.xenaddons.features.impl.dev;

import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.utils.KotlinGuiBridge;

public class EditHudModule extends Module {

    public EditHudModule() {
        super("Edit HUD", "Open the HUD editor", Category.DEV);
    }

    @Override
    public void onClick() {
        if (mc.currentScreen == null) return;
        var hudEditor = KotlinGuiBridge.createHudEditorScreen(mc.currentScreen);
        if (hudEditor != null) mc.setScreen(hudEditor);
    }
}
