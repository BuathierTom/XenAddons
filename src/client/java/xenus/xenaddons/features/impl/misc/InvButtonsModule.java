package xenus.xenaddons.features.impl.misc;

import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.gui.GuiInvButtonMenu;

public class InvButtonsModule extends Module {
    public InvButtonsModule() {
        super("InvButtons", "Open the Inventory Buttons menu", Category.MISC);
    }

    @Override
    public void onEnable() {
        if (mc.currentScreen == null || !(mc.currentScreen instanceof GuiInvButtonMenu)) {
            // We need to schedule this because we might be in the ClickGUI drawing loop
             mc.execute(() -> mc.setScreen(new GuiInvButtonMenu()));
        }
        // Immediately disable efficiently so it acts like a button
        this.setEnabled(false);
    }
}
