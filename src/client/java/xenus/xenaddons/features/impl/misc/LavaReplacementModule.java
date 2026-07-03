package xenus.xenaddons.features.impl.misc;

import net.minecraft.client.Minecraft;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;

public class LavaReplacementModule extends Module {
    public LavaReplacementModule() {
        super("Lava Remplacement", "Render lava like water", Category.MISC);
    }

    public static boolean isActive() {
        LavaReplacementModule module = ModuleManager.getModule(LavaReplacementModule.class);
        return module != null && module.isEnabled();
    }

    @Override
    public void onEnable() {
        refreshWorldRenderer();
    }

    @Override
    public void onDisable() {
        refreshWorldRenderer();
    }

    private static void refreshWorldRenderer() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.levelRenderer == null) return;
        client.levelRenderer.allChanged();
    }
}
