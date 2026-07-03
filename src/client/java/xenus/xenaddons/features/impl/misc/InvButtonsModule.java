package xenus.xenaddons.features.impl.misc;

import xenus.xenaddons.XenAddons;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.gui.GuiInvButtonConfig;
import xenus.xenaddons.gui.GuiInvButtonEditor;
import xenus.xenaddons.settings.ActionSetting;
import xenus.xenaddons.settings.BooleanSetting;

public class InvButtonsModule extends Module {
    
    public final BooleanSetting showTooltips = new BooleanSetting("Show Tooltips", XenAddons.instance.showTooltips);
    public final BooleanSetting hideRecipeBook = new BooleanSetting("Hide Recipe Book", XenAddons.instance.hideRecipeBook);
    
    public final ActionSetting editButtons = new ActionSetting("Edit Buttons Position", () -> {
        mc.execute(() -> mc.setScreen(new GuiInvButtonEditor(mc.screen)));
    });
    
    public final ActionSetting openProfiles = new ActionSetting("Profiles & Advanced", () -> {
        mc.execute(() -> mc.setScreen(new GuiInvButtonConfig(mc.screen)));
    });

    public InvButtonsModule() {
        super("Inventory Buttons", "Add custom buttons to your inventory", Category.MISC);
        
        showTooltips.setListener(v -> { XenAddons.instance.showTooltips = v; XenAddons.save(); });
        hideRecipeBook.setListener(v -> { XenAddons.instance.hideRecipeBook = v; XenAddons.save(); });
        
        this.registerSetting(showTooltips);
        this.registerSetting(hideRecipeBook);
        this.registerSetting(editButtons);
        this.registerSetting(openProfiles);
        
        // Load initial state
        this.setEnabled(XenAddons.instance.enabled);
    }

    @Override
    public void onEnable() {
        XenAddons.instance.enabled = true;
        XenAddons.save();
    }

    @Override
    public void onDisable() {
        XenAddons.instance.enabled = false;
        XenAddons.save();
    }
}
