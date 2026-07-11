package xenus.xenaddons;

import xenus.xenaddons.gui.GuiInvButtonMenu;
import xenus.xenaddons.render.nvg.XenNvgSpecialElementRenderer;
import xenus.xenaddons.utils.HypixelItemManager;
import xenus.xenaddons.utils.KotlinGuiBridge;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

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

		PictureInPictureRendererRegistry.register(context -> new XenNvgSpecialElementRenderer(context.bufferSource()));

        // Initialize Utilities
        xenus.xenaddons.utils.DungeonUtils.init();
        xenus.xenaddons.utils.UpdateChecker.init();
        xenus.xenaddons.features.impl.dungeons.AutoPetRuleTitleModule.init();
        xenus.xenaddons.features.impl.dungeons.BatDeathAlertModule.init();
        xenus.xenaddons.config.GoatedPlayersManager.init();
        xenus.xenaddons.config.CustomPlayerScaleManager.init();
        
		// Load Config
		xenus.xenaddons.config.ConfigManager.load();

		// Save Config on Shutdown
		Runtime.getRuntime().addShutdownHook(new Thread(xenus.xenaddons.config.ConfigManager::save));

		// Register commands
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

			// Command: /invbuttons
			var invbuttonsNode = dispatcher.register(literal("invbuttons")
					.executes(context -> {
						openMenuNextTick = true;
						return 1;
					})
			);
			dispatcher.register(literal("Invbuttons").redirect(invbuttonsNode));

			// Command: /pcc -> /chat party
			var pccNode = dispatcher.register(literal("pcc")
					.executes(context -> {
						net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
						if (mc.player == null || mc.player.connection == null) return 0;
						mc.player.connection.sendCommand("chat party");
						return 1;
					})
			);
			dispatcher.register(literal("Pcc").redirect(pccNode));

			// Command: /xenaddons or /xa
			var xenaddonsNode = dispatcher.register(literal("xenaddons")
					.executes(context -> {
						openClickGUINextTick = true;
						return 1;
					})
			);
			dispatcher.register(literal("Xenaddons").redirect(xenaddonsNode));
			var xaNode = dispatcher.register(literal("xa")
					.then(literal("hud").executes(context -> {
						openEditorNextTick = true;
						return 1;
					}))
					.then(xenus.xenaddons.features.impl.dungeons.RefillModule.createCommand())
					.then(literal("pf").executes(context -> {
                        xenus.xenaddons.features.impl.dev.DebugModule debug =
                                xenus.xenaddons.features.ModuleManager.getModule(xenus.xenaddons.features.impl.dev.DebugModule.class);
                        if (debug == null || !debug.isEnabled() || !debug.enablePfStatsTest.getValue()) {
                            return 0;
                        }
                        xenus.xenaddons.features.impl.dungeons.DungeonsStatsPFModule.debugTrigger("dtNyro");
                        xenus.xenaddons.utils.ChatUtils.info("PF debug launched for dtNyro");
                        return 1;
                    }).then(ClientCommands.argument("player", StringArgumentType.word()).executes(context -> {
                        xenus.xenaddons.features.impl.dev.DebugModule debug =
                                xenus.xenaddons.features.ModuleManager.getModule(xenus.xenaddons.features.impl.dev.DebugModule.class);
                        if (debug == null || !debug.isEnabled() || !debug.enablePfStatsTest.getValue()) {
                            return 0;
                        }
                        String player = StringArgumentType.getString(context, "player");
                        xenus.xenaddons.features.impl.dungeons.DungeonsStatsPFModule.debugTrigger(player);
                        xenus.xenaddons.utils.ChatUtils.info("PF debug launched for " + player);
                        return 1;
                    })))
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

                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        if (mc.getConnection() != null) {
                            xenus.xenaddons.utils.ChatUtils.info("§aScanning player classes (Debug Mode)...");
                            
                            // Force update to ensure latest data
                            xenus.xenaddons.utils.DungeonUtils.updateClasses(); 
                            xenus.xenaddons.utils.ChatUtils.info("In Dungeon: " + xenus.xenaddons.utils.DungeonUtils.inDungeon());

                            for (net.minecraft.client.multiplayer.PlayerInfo entry : mc.getConnection().getListedOnlinePlayers()) {
                                String name = entry.getProfile().name();
                                net.minecraft.network.chat.Component display = entry.getTabListDisplayName();
                                String rawDisplay = (display != null) ? display.getString() : "null";
                                
                                String clazz = xenus.xenaddons.utils.DungeonUtils.getPlayerClass(name);
                                String color = (clazz != null) ? xenus.xenaddons.utils.DungeonUtils.getClassColor(clazz) : "§7";
                                
                                xenus.xenaddons.utils.ChatUtils.info(color + name + " -> Class: " + clazz + " | Tab: " + rawDisplay);
                            }
                            
                            if (mc.player != null) {
                                String myName = mc.player.getName().getString();
                                String myClass = xenus.xenaddons.utils.DungeonUtils.getPlayerClass(mc.player);
                                int myClassLevel = xenus.xenaddons.utils.DungeonUtils.getPlayerClassLevel(mc.player);
                                boolean uniqueClass = xenus.xenaddons.utils.DungeonUtils.isUniqueClass();
                                xenus.xenaddons.features.impl.dungeons.PosTitlesModule pm = 
                                    xenus.xenaddons.features.ModuleManager.getModule(xenus.xenaddons.features.impl.dungeons.PosTitlesModule.class);

                                xenus.xenaddons.utils.ChatUtils.info(
                                    "Self -> Class: " + myClass
                                        + " | Level: " + myClassLevel
                                        + " | UniqueClass: " + uniqueClass
                                );
                                
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
                            
                            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
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
                    .then(literal("procs").executes(context -> {
                        xenus.xenaddons.features.impl.dungeons.InvisibilityTimerModule module =
                                xenus.xenaddons.features.ModuleManager.getModule(xenus.xenaddons.features.impl.dungeons.InvisibilityTimerModule.class);
                        if (module == null) return 0;
                        if (!module.isEnabled()) module.setEnabled(true);

                        module.debugTriggerProcAlert();
                        return 1;
                    }).then(ClientCommands.argument("type", StringArgumentType.word()).executes(context -> {
                        xenus.xenaddons.features.impl.dungeons.InvisibilityTimerModule module =
                                xenus.xenaddons.features.ModuleManager.getModule(xenus.xenaddons.features.impl.dungeons.InvisibilityTimerModule.class);
                        if (module == null) return 0;
                        if (!module.isEnabled()) module.setEnabled(true);

                        String type = StringArgumentType.getString(context, "type");
                        String normalized = type == null ? "" : type.toLowerCase(java.util.Locale.ROOT);
                        if (!normalized.equals("spirit") && !normalized.equals("bonzo")
                                && !normalized.equals("phoenix") && !normalized.equals("pheonix")) {
                            xenus.xenaddons.utils.ChatUtils.error("Usage: /xa procs <spirit|bonzo|phoenix>");
                            return 0;
                        }

                        module.debugTriggerProc(normalized);
                        return 1;
                    })))
                    .then(literal("gate").executes(context -> {
                        xenus.xenaddons.features.impl.dev.DebugModule debug =
                                xenus.xenaddons.features.ModuleManager.getModule(xenus.xenaddons.features.impl.dev.DebugModule.class);
                        if (debug == null || !debug.isEnabled() || !debug.enableGateTest.getValue()) {
                            return 0;
                        }
                        xenus.xenaddons.features.impl.dungeons.ReminderGateModule module =
                                xenus.xenaddons.features.ModuleManager.getModule(xenus.xenaddons.features.impl.dungeons.ReminderGateModule.class);
                        if (module == null) return 0;
                        if (!module.isEnabled()) module.setEnabled(true);
                        module.debugTrigger();
                        xenus.xenaddons.utils.ChatUtils.info("Reminder Gate test triggered.");
                        return 1;
                    }))
                    .then(literal("milestone").executes(context -> {
                        xenus.xenaddons.features.impl.dev.DebugModule debug =
                                xenus.xenaddons.features.ModuleManager.getModule(xenus.xenaddons.features.impl.dev.DebugModule.class);
                        if (debug == null || !debug.isEnabled() || !debug.enableMilestoneTest.getValue()) {
                            return 0;
                        }
                        xenus.xenaddons.features.impl.dungeons.MilestoneDisplayModule module =
                                xenus.xenaddons.features.ModuleManager.getModule(xenus.xenaddons.features.impl.dungeons.MilestoneDisplayModule.class);
                        if (module == null) return 0;
                        module.debugTriggerReminder();
                        xenus.xenaddons.utils.ChatUtils.info("Milestone Reminder test triggered.");
                        return 1;
                    }))
					.executes(context -> {
						openClickGUINextTick = true;
						return 1;
					})
			);
			dispatcher.register(literal("Xa").redirect(xaNode));
		});

		// Ticker to handle opening the screens safely on the client thread
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (openMenuNextTick) {
				openMenuNextTick = false;
				client.setScreen(new GuiInvButtonMenu());
			}
			if (openEditorNextTick) {
				openEditorNextTick = false;
				var hudEditor = KotlinGuiBridge.createHudEditorScreen(client.screen);
				if (hudEditor != null) {
					client.setScreen(hudEditor);
				}
			}
			if (openClickGUINextTick) {
				openClickGUINextTick = false;
				KotlinGuiBridge.openConfigScreen();
			}
		});

		XenAddons.LOGGER.info("XenAddons loaded successfully!");
	}
}
