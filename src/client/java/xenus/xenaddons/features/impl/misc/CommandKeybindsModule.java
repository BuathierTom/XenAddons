package xenus.xenaddons.features.impl.misc;

import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.KeybindSetting;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;

public class CommandKeybindsModule extends Module {
    
    public final KeybindSetting wardrobe = new KeybindSetting("Wardrobe", -1);
    public final KeybindSetting equipment = new KeybindSetting("Equipment", -1);
    public final KeybindSetting trades = new KeybindSetting("Trades", -1);
    public final KeybindSetting garden = new KeybindSetting("Garden", -1);
    public final KeybindSetting pet = new KeybindSetting("Pets", -1);
    
    private boolean wardP = false;
    private boolean equipP = false;
    private boolean tradeP = false;
    private boolean gardP = false;
    private boolean petP = false;

    public CommandKeybindsModule() {
        super("Custom Keybinds", "Bind commands to keys", Category.MISC);
        this.registerSetting(wardrobe);
        this.registerSetting(equipment);
        this.registerSetting(trades);
        this.registerSetting(garden);
        this.registerSetting(pet);
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || !this.isEnabled()) return;
            
            boolean allowTrigger = client.screen == null;
            
            wardP = checkKey(wardrobe, "/wardrobe", wardP, allowTrigger);
            equipP = checkKey(equipment, "/equipment", equipP, allowTrigger);
            tradeP = checkKey(trades, "/trades", tradeP, allowTrigger);
            gardP = checkKey(garden, "/warp garden", gardP, allowTrigger);
            petP = checkKey(pet, "/pet", petP, allowTrigger);
        });
    }
    
    private boolean checkKey(KeybindSetting setting, String cmd, boolean wasPressed, boolean allowTrigger) {
        int key = setting.getKey();
        if (key == -1) return false;
        
        long handle = mc.getWindow().handle();
        boolean isPressed = false;
        try {
            if (key < 0) {
                // MouseHandler Button
                int btn = -100 - key;
                if (btn >= 0) {
                     isPressed = GLFW.glfwGetMouseButton(handle, btn) == GLFW.GLFW_PRESS;
                }
            } else {
                isPressed = GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
            }
        } catch (Exception e) {}
        
        // Trigger only if pressed now, wasn't pressed before, AND trigger is allowed (no screen open)
        if (isPressed && !wasPressed && allowTrigger) {
            if (mc.player != null) {
                String c = cmd.startsWith("/") ? cmd.substring(1) : cmd;
                mc.player.connection.sendCommand(c);
            }
        }
        
        return isPressed;
    }
}
