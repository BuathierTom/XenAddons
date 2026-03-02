package xenus.xenaddons.features.impl.dungeons;

import xenus.xenaddons.hud.HudState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.settings.NumberSetting;
import xenus.xenaddons.utils.DungeonUtils;

public class AutoPetRuleTitleModule extends Module {
    private final BooleanSetting inDungeonsOnly = new BooleanSetting("In Dungeons Only", false);
    private final NumberSetting hudOffsetX = new NumberSetting("HUD Offset X", 0, -1000, 1000, 1);
    private final NumberSetting hudOffsetY = new NumberSetting("HUD Offset Y", -20, -1000, 1000, 1);
    private final NumberSetting hudScale = new NumberSetting("HUD Scale", 2.5, 0.5, 6.0, 0.05);

    public AutoPetRuleTitleModule() {
        super("AutoPet Title", "Shows a title when AutoPet equips a pet", Category.DUNGEON);
        hudOffsetX.setVisibility(() -> false);
        hudOffsetY.setVisibility(() -> false);
        hudScale.setVisibility(() -> false);
        this.registerSetting(inDungeonsOnly);
        this.registerSetting(hudOffsetX);
        this.registerSetting(hudOffsetY);
        this.registerSetting(hudScale);
    }

    public static void init() {
        // Hides original message and triggers our custom one
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            return !checkAndProcess(message.getString());
        });
        
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            return !checkAndProcess(message.getString());
        });

        // Tick Event for our Custom Notification
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            HudState.PET_NOTIFICATION.tick();
        });
    }

    private static boolean checkAndProcess(String msg) {
        AutoPetRuleTitleModule module = null;
        for (Module m : ModuleManager.modules) {
            if (m instanceof AutoPetRuleTitleModule) {
                module = (AutoPetRuleTitleModule) m;
                break;
            }
        }

        if (module != null && module.isEnabled()) {
            return module.handleAndFilter(msg);
        }
        return false;
    }

    private boolean handleAndFilter(String msg) {
        if (msg.contains("Autopet") && msg.contains("equipped your")) {
            String colorCode = "§a"; 
            String petName = "Pet";
            
            try {
                int equippedIndex = msg.indexOf("equipped your");
                if (equippedIndex != -1) {
                    String rest = msg.substring(equippedIndex + 13);
                    int bracketEnd = rest.indexOf("]");
                    if (bracketEnd != -1) {
                        int colorIndex = rest.indexOf("§", bracketEnd);
                        if (colorIndex != -1 && colorIndex + 2 <= rest.length()) {
                            colorCode = rest.substring(colorIndex, colorIndex + 2);
                            int nameStart = colorIndex + 2;
                            int exclamationIndex = rest.indexOf("!", nameStart);
                            if (exclamationIndex != -1) {
                                petName = rest.substring(nameStart, exclamationIndex).trim();
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            
            if (petName.endsWith(" ✦")) {
                petName = petName.substring(0, petName.length() - 2);
            }

            final String finalPetName = petName;
            final String finalColor = colorCode;
            
            mc.execute(() -> showSmallNotification(finalPetName, finalColor));
            
            return true; // Cancel original message
        }
        return false;
    }

    public void debugTrigger() {
        showSmallNotification("Golden Dragon", "§6");
    }

    private void showSmallNotification(String petName, String colorCode) {
        int duration = 8; // 400ms (8 ticks) 

        if (!inDungeonsOnly.getValue() || DungeonUtils.inDungeon()) {
            HudState.PET_NOTIFICATION.show(
                Text.of(colorCode + petName),
                Text.of("§cAutopet§r"),
                duration
            );
        }

        if (mc.player != null) {
            mc.player.sendMessage(Text.of("§cAutopet §7> " + colorCode + petName), false);
        }
    }

    public int getHudOffsetX() {
        return hudOffsetX.getValue().intValue();
    }

    public int getHudOffsetY() {
        return hudOffsetY.getValue().intValue();
    }

    public void setHudOffsetX(int value) {
        hudOffsetX.setValue((double) value);
    }

    public void setHudOffsetY(int value) {
        hudOffsetY.setValue((double) value);
    }

    public double getHudScale() {
        return hudScale.getValue();
    }

    public void setHudScale(double value) {
        hudScale.setValue(value);
    }
}
