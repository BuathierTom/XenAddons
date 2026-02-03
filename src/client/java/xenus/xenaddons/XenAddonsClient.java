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

	@Override
	public void onInitializeClient() {
		XenAddons.load();

		// Fetch Hypixel items in background
		HypixelItemManager.loadAsync();

		// Register commands
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

			// Command: /xa
			dispatcher.register(literal("xa")
					.executes(context -> {
						openMenuNextTick = true;
						return 1;
					})
					.then(literal("buttons").executes(context -> {
						openMenuNextTick = true;
						return 1;
					}))
					.then(literal("edit").executes(context -> {
						openEditorNextTick = true;
						return 1;
					}))
			);

			// Command: /xenaddons
			dispatcher.register(literal("xenaddons")
					.executes(context -> {
						openMenuNextTick = true;
						return 1;
					})
					.then(literal("buttons").executes(context -> {
						openMenuNextTick = true;
						return 1;
					}))
					.then(literal("edit").executes(context -> {
						openEditorNextTick = true;
						return 1;
					}))
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
		});

		XenAddons.LOGGER.info("XenAddons loaded successfully!");
	}
}