package xenus.xenaddons.features.impl.misc;

import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;

public class CustomSizeModule extends Module {
    public CustomSizeModule() {
        super("Custom Size", "Apply custom player scale", Category.DEV);
        super.setEnabled(true);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(true);
    }

    @Override
    public void toggle() {
        super.setEnabled(true);
    }
}
