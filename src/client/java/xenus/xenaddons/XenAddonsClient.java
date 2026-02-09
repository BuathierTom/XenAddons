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

        // Initialize Utilities
        xenus.xenaddons.utils.DungeonUtils.init();
        xenus.xenaddons.features.impl.dungeons.AutoPetRuleTitleModule.init();
        
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
					.then(literal("title").executes(context -> {
                        xenus.xenaddons.features.impl.dev.DebugModule debug = 
                            xenus.xenaddons.features.ModuleManager.getModule(xenus.xenaddons.features.impl.dev.DebugModule.class);
                        
                        if (debug != null && debug.isEnabled() && debug.enableTitleTest.getValue()) {
                            xenus.xenaddons.features.impl.dungeons.PosTitlesModule pm = 
                                xenus.xenaddons.features.ModuleManager.getModule(xenus.xenaddons.features.impl.dungeons.PosTitlesModule.class);
                            if (pm != null) pm.debugTrigger();
                            return 1;
                        }
                        return 0;
                    }))
                    .then(literal("petrule").executes(context -> {
                        xenus.xenaddons.features.impl.dev.DebugModule debug = 
                            xenus.xenaddons.features.ModuleManager.getModule(xenus.xenaddons.features.impl.dev.DebugModule.class);

                        if (debug != null && debug.isEnabled() && debug.enablePetRuleTest.getValue()) {
                            xenus.xenaddons.features.impl.dungeons.AutoPetRuleTitleModule ap = 
                                xenus.xenaddons.features.ModuleManager.getModule(xenus.xenaddons.features.impl.dungeons.AutoPetRuleTitleModule.class);
                            if (ap != null) ap.debugTrigger();
                            return 1;
                        }
                        return 0;
                    }))
                    .then(literal("ticlass").executes(context -> {
                        xenus.xenaddons.features.impl.dev.DebugModule debug = 
                            xenus.xenaddons.features.ModuleManager.getModule(xenus.xenaddons.features.impl.dev.DebugModule.class);

                        if (debug == null || !debug.isEnabled() || !debug.enableTiclassTest.getValue()) {
                            return 0;
                        }

                        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
                        if (mc.getNetworkHandler() != null) {
                            xenus.xenaddons.utils.ChatUtils.info("§aScanning player classes (Debug Mode)...");
                            
                            // Force update to ensure latest data
                            xenus.xenaddons.utils.DungeonUtils.updateClasses(); 
                            xenus.xenaddons.utils.ChatUtils.info("In Dungeon: " + xenus.xenaddons.utils.DungeonUtils.inDungeon());

                            for (net.minecraft.client.network.PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                                String name = entry.getProfile().name();
                                net.minecraft.text.Text display = entry.getDisplayName();
                                String rawDisplay = (display != null) ? display.getString() : "null";
                                
                                String clazz = xenus.xenaddons.utils.DungeonUtils.getPlayerClass(name);
                                String color = (clazz != null) ? xenus.xenaddons.utils.DungeonUtils.getClassColor(clazz) : "§7";
                                
                                xenus.xenaddons.utils.ChatUtils.info(color + name + " -> Class: " + clazz + " | Tab: " + rawDisplay);
                            }
                            
                            if (mc.player != null) {
                                String myName = mc.player.getName().getString();
                                String myClass = xenus.xenaddons.utils.DungeonUtils.getPlayerClass(mc.player);
                                xenus.xenaddons.features.impl.dungeons.PosTitlesModule pm = 
                                    xenus.xenaddons.features.ModuleManager.getModule(xenus.xenaddons.features.impl.dungeons.PosTitlesModule.class);
                                
                                if (pm != null && pm.isEnabled()) {
                                    // Pass the detected class, or if null, indicate failure in the alert text instead of faking Mage
                                    if (myClass != null) {
                                        pm.showAlert(myName, myClass, "Class Detected via UUID!");
                                    } else {
                                        pm.showAlert(myName, "Mage", "Class NOT Detected (Still Defaulting Mage)");
                                    }
                                }
                            }
                        }
                        return 1;
                    }))
                    .then(literal("secretroom")
                        .executes(context -> {
                            xenus.xenaddons.features.impl.dev.DebugModule debugModule = 
                                xenus.xenaddons.features.ModuleManager.getModule(xenus.xenaddons.features.impl.dev.DebugModule.class);
                            
                            if (debugModule == null || !debugModule.isEnabled() || !debugModule.enableSecretRoomTest.getValue()) {
                                return 0;
                            }
                            
                            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
                            if (mc.player == null) return 0;
                            
                            int px = (int) mc.player.getX();
                            int pz = (int) mc.player.getZ();
                            org.joml.Vector2i center = xenus.xenaddons.features.impl.dungeons.secretroutes.dungeon.DungeonScanner.getRoomCentre(px, pz);
                            
                            xenus.xenaddons.utils.ChatUtils.info("§b[SecretRoutes Debug]");
                            xenus.xenaddons.utils.ChatUtils.info("Pos: " + px + ", " + pz);
                            xenus.xenaddons.utils.ChatUtils.info("Room Center: " + center.x + ", " + center.y);
                            xenus.xenaddons.utils.ChatUtils.info("In Dungeon: " + xenus.xenaddons.utils.DungeonUtils.inDungeon());
                            
                            xenus.xenaddons.features.impl.dungeons.secretroutes.dungeon.DungeonRoom room = xenus.xenaddons.features.impl.dungeons.secretroutes.dungeon.DungeonScanner.currentRoom;
                            if (room != null) {
                                xenus.xenaddons.utils.ChatUtils.info("Current Room: §a" + room.data.name());
                                xenus.xenaddons.utils.ChatUtils.info("Rotation: §e" + room.rotation);
                            } else {
                                xenus.xenaddons.utils.ChatUtils.info("Current Room: §cNone (Scan failed or not triggered)");
                            }
                            
                            return 1;
                        }))
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