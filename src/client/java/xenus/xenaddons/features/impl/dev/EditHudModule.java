package xenus.xenaddons.features.impl.dev;

import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.gui.HudEditorScreen;

public class EditHudModule extends Module {

    public EditHudModule() {
        super("Edit HUD", "Open the HUD editor", Category.DEV);
    }

    @Override
    public void onClick() {
        if (mc.currentScreen == null) return;
        mc.setScreen(new HudEditorScreen(mc.currentScreen));
    }
}
