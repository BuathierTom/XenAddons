package xenus.xenaddons.features.impl.misc;

import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.BooleanSetting;

public class ItemPriceModule extends Module {
    public final BooleanSetting showAhPrice = new BooleanSetting("AH Price", true);
    public final BooleanSetting showBzPrice = new BooleanSetting("Bazaar Price", true);

    public ItemPriceModule() {
        super("Item Price", "Displays item prices in SkyBlock tooltips", Category.MISC);
        this.registerSetting(showAhPrice);
        this.registerSetting(showBzPrice);
    }
}
