package xenus.xenaddons.features.impl.misc;

import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;

public class PrivateMessageModule extends Module {

    public PrivateMessageModule() {
        super("Private Message Format", "Rewrites private messages as \"Msg > To/From <player>: message\" with the message in white", Category.MISC);
    }

    public static boolean isActive() {
        PrivateMessageModule module = ModuleManager.getModule(PrivateMessageModule.class);
        return module != null && module.isEnabled();
    }
}
