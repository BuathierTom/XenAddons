package xenus.xenaddons;

import xenus.xenaddons.gui.GuiInvButtonEditor;
import xenus.xenaddons.gui.GuiInvButtonMenu;
import xenus.xenaddons.utils.HypixelItemManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class XenAddonsClient implements ClientModInitializer {

	private static boolean openMenuNextTick = false;
	private static boolean openEditorNextTick = false;
	private static boolean openClickGUINextTick = false;

	@Override
	public void onInitializeClient() {
		XenAddons.load();

		// Fetch Hypixel items in background
		HypixelItemManager.loadAsync();

		// Initialize Modules
		xenus.xenaddons.features.ModuleManager.init();

		// Load Config
		xenus.xenaddons.config.ConfigManager.load();

		// Save Config on Shutdown
		Runtime.getRuntime().addShutdownHook(new Thread(xenus.xenaddons.config.ConfigManager::save));

		// Register commands
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

			// Command: /invbuttons
			dispatcher.register(literal("invbuttons")
					.executes(context -> {
						openMenuNextTick = true;
						return 1;
					})
			);

			// Command: /xenaddons or /xa
			dispatcher.register(literal("xenaddons")
					.executes(context -> {
						openClickGUINextTick = true;
						return 1;
					})
			);
			dispatcher.register(literal("xa")
					.executes(context -> {
						openClickGUINextTick = true;
						return 1;
					})
			);
		});

		// Ticker to handle opening the screens safely on the client thread
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (openMenuNextTick) {
				openMenuNextTick = false;
				client.setScreen(new GuiInvButtonMenu());
			}
			if (openEditorNextTick) {
				openEditorNextTick = false;
				client.setScreen(new GuiInvButtonEditor(null));
			}
			if (openClickGUINextTick) {
				openClickGUINextTick = false;
				client.setScreen(new xenus.xenaddons.gui.clickgui.ClickGUI());
			}
		});

		XenAddons.LOGGER.info("XenAddons loaded successfully!");
	}
}