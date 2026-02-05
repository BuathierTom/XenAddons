package xenus.xenaddons.features.impl.dungeons;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.mixin.client.AccessorInGameHud;

public class AutoPetRuleTitleModule extends Module {

    public AutoPetRuleTitleModule() {
        super("AutoPet Title", "Shows a title when AutoPet equips a pet", Category.DUNGEON);
    }

    public static void init() {
        // Hides original message and triggers our custom one
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            return !checkAndProcess(message.getString());
        });
        
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            return !checkAndProcess(message.getString());
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

    private void showSmallNotification(String petName, String colorCode) {
        if (mc.inGameHud == null) return;
        
        // Instant timers
        try {
            AccessorInGameHud accessor = (AccessorInGameHud) mc.inGameHud;
            accessor.setTitleFadeInTicks(0);
            accessor.setTitleStayTicks(8); // 400ms
            accessor.setTitleFadeOutTicks(0);
        } catch (Exception ignored) {}

        // Small notification trick:
        // Use subtitle for the pet name (smaller than title)
        // Use action bar for "(Autopet)" (even smaller)
        mc.inGameHud.setTitle(Text.of("")); 
        mc.inGameHud.setSubtitle(Text.of(colorCode + petName));
        mc.inGameHud.setOverlayMessage(Text.of("§cAutopet§r"), false);

        // New cleaner chat message
        xenus.xenaddons.utils.ChatUtils.info("Autopet > " + colorCode + petName);
    }
}
