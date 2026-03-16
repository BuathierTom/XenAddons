package xenus.xenaddons.features.impl.misc;

import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.BooleanSetting;

public class CopyChatModule extends Module {
    private final BooleanSetting requireCtrlClick = new BooleanSetting("Ctrl Click", false);

    public CopyChatModule() {
        super("Copy Chat", "Right-click a chat message to copy it", Category.MISC);
        this.registerSetting(requireCtrlClick);
    }

    public boolean isRequireCtrlClickEnabled() {
        return requireCtrlClick.getValue();
    }
}
