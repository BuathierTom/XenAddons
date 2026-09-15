package xenus.xenaddons.features.impl.misc;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.utils.CommandTreeRefresher;
import xenus.xenaddons.utils.WarpShortcutsManager;

import java.util.Map;

public class WarpShortcutsModule extends Module {

    private static volatile CommandDispatcher<FabricClientCommandSource> activeDispatcher;

    public WarpShortcutsModule() {
        super("Warps Shortcuts", "Use /<warp> instead of /warp <warp>", Category.MISC);
    }

    @Override
    public void onEnable() {
        CommandTreeRefresher.refresh();
    }

    @Override
    public void onDisable() {
        CommandTreeRefresher.refresh();
    }

    public static boolean isActive() {
        WarpShortcutsModule module = ModuleManager.getModule(WarpShortcutsModule.class);
        return module != null && module.isEnabled();
    }

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            activeDispatcher = dispatcher;
            registerShortcuts(dispatcher);
        });

        WarpShortcutsManager.loadAsync(WarpShortcutsModule::onShortcutsLoaded);
    }

    private static void onShortcutsLoaded() {
        CommandDispatcher<FabricClientCommandSource> dispatcher = activeDispatcher;
        if (dispatcher == null) return;

        registerShortcuts(dispatcher);
        CommandTreeRefresher.refresh();
    }

    private static void registerShortcuts(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        for (Map.Entry<String, String> shortcut : WarpShortcutsManager.getShortcuts().entrySet()) {
            String alias = shortcut.getKey();
            if (dispatcher.getRoot().getChild(alias) != null) continue;

            dispatcher.register(ClientCommands.literal(alias)
                .requires(source -> isActive())
                .executes(context -> warp(alias)));
        }
    }

    private static int warp(String alias) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.player.connection == null) return 0;

        String target = WarpShortcutsManager.getWarpTarget(alias);
        client.player.connection.sendCommand(target == null ? alias : "warp " + target);
        return 1;
    }
}
