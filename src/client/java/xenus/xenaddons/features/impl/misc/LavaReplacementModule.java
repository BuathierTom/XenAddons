package xenus.xenaddons.features.impl.misc;

import net.minecraft.client.MinecraftClient;
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.worldRenderer == null) return;
        client.worldRenderer.reload();
    }
}
