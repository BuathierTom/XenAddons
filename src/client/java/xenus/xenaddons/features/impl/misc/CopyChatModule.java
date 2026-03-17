package xenus.xenaddons.features.impl.misc;

import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.settings.ModeSetting;

public class CopyChatModule extends Module {
    private final BooleanSetting requireCtrlClick = new BooleanSetting("Ctrl Click", false);
    private final ModeSetting copyMode = new ModeSetting("Copy Mode", "Right Copy", "Right Copy", "Left Copy");

    public CopyChatModule() {
        super("Copy Chat", "Copy chat messages with a configurable mouse click", Category.MISC);
        this.registerSetting(requireCtrlClick);
        this.registerSetting(copyMode);
    }

    public boolean isRequireCtrlClickEnabled() {
        return requireCtrlClick.getValue();
    }

    public boolean isCopyOnLeftClickEnabled() {
        return "Left Copy".equals(copyMode.getValue());
    }
}
