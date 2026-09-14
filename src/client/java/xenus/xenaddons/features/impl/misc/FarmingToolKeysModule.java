package xenus.xenaddons.features.impl.misc;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.settings.KeybindSetting;
import xenus.xenaddons.utils.ChatUtils;
import xenus.xenaddons.utils.GardenUtils;
import xenus.xenaddons.utils.ItemPriceManager;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class FarmingToolKeysModule extends Module {

    private static final String[] TOOL_ID_PREFIXES = {
        "THEORETICAL_HOE_",
        "PUMPKIN_DICER",
        "MELON_DICER",
        "COCO_CHOPPER",
        "CACTUS_KNIFE",
        "FUNGI_CUTTER"
    };

    private static final int UNBOUND = -1;
    private static final int MOUSE_BASE = -100;
    private static final int LOWEST_MOUSE_CODE = -100;
    private static final int HIGHEST_MOUSE_CODE = -93;

    public final KeybindSetting toggleKey = new KeybindSetting("Toggle Key", UNBOUND);
    public final KeybindSetting forwardKey = new KeybindSetting("Forward", UNBOUND);
    public final KeybindSetting backKey = new KeybindSetting("Back", UNBOUND);
    public final KeybindSetting leftKey = new KeybindSetting("Left", UNBOUND);
    public final KeybindSetting rightKey = new KeybindSetting("Right", UNBOUND);
    public final KeybindSetting breakKey = new KeybindSetting("Break", UNBOUND);

    private final Set<Integer> consumedClicks = new HashSet<>();

    private boolean toggleWasDown = false;
    private volatile boolean active = false;

    public FarmingToolKeysModule() {
        super("Farming Tools Keys", "Custom movement and break keys while holding a farming tool in the Garden", Category.MISC);

        this.registerSetting(forwardKey);
        this.registerSetting(backKey);
        this.registerSetting(leftKey);
        this.registerSetting(rightKey);
        this.registerSetting(breakKey);
        this.registerSetting(toggleKey);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            handleToggleKey();
            updateActiveState();
        });
    }

    public static Boolean resolveIsDown(KeyMapping mapping, int ownKeyCode) {
        FarmingToolKeysModule module = ModuleManager.getModule(FarmingToolKeysModule.class);
        return module == null ? null : module.overrideIsDown(mapping, ownKeyCode);
    }

    public static Boolean resolveConsumeClick(KeyMapping mapping, int ownKeyCode) {
        FarmingToolKeysModule module = ModuleManager.getModule(FarmingToolKeysModule.class);
        return module == null ? null : module.overrideConsumeClick(mapping, ownKeyCode);
    }

    @Override
    public void onDisable() {
        active = false;
        consumedClicks.clear();
    }

    private Boolean overrideIsDown(KeyMapping mapping, int ownKeyCode) {
        if (!active) return null;

        Integer override = overrideFor(mapping);
        if (override == null) return isOverrideKey(ownKeyCode) ? Boolean.FALSE : null;

        return isKeyHeld(override);
    }

    private Boolean overrideConsumeClick(KeyMapping mapping, int ownKeyCode) {
        if (!active) return null;

        Integer override = overrideFor(mapping);
        if (override == null) return isOverrideKey(ownKeyCode) ? Boolean.FALSE : null;

        return consumeClick(override);
    }

    private boolean consumeClick(int keyCode) {
        if (!isKeyHeld(keyCode)) {
            consumedClicks.remove(keyCode);
            return false;
        }
        return consumedClicks.add(keyCode);
    }

    private Integer overrideFor(KeyMapping mapping) {
        Options options = mc.options;
        if (options == null || mapping == null) return null;

        if (mapping == options.keyUp) return boundKey(forwardKey);
        if (mapping == options.keyDown) return boundKey(backKey);
        if (mapping == options.keyLeft) return boundKey(leftKey);
        if (mapping == options.keyRight) return boundKey(rightKey);
        if (mapping == options.keyAttack) return boundKey(breakKey);
        return null;
    }

    private Integer boundKey(KeybindSetting setting) {
        int key = setting.getKey();
        return key == UNBOUND ? null : key;
    }

    private boolean isOverrideKey(int keyCode) {
        if (keyCode == UNBOUND) return false;

        return matches(forwardKey, keyCode)
            || matches(backKey, keyCode)
            || matches(leftKey, keyCode)
            || matches(rightKey, keyCode)
            || matches(breakKey, keyCode);
    }

    private boolean matches(KeybindSetting setting, int keyCode) {
        int key = setting.getKey();
        return key != UNBOUND && key == keyCode;
    }

    private void updateActiveState() {
        boolean nowActive = computeActive();
        if (nowActive == active) return;

        active = nowActive;
        consumedClicks.clear();
    }

    private boolean computeActive() {
        if (!this.isEnabled()) return false;
        if (mc.player == null || mc.level == null || mc.screen != null) return false;
        if (!GardenUtils.inGarden()) return false;

        return isHoldingFarmingTool();
    }

    private boolean isHoldingFarmingTool() {
        ItemStack held = mc.player.getMainHandItem();
        if (held == null || held.isEmpty()) return false;

        return isFarmingToolId(ItemPriceManager.extractSkyBlockId(held));
    }

    static boolean isFarmingToolId(String skyBlockId) {
        if (skyBlockId == null || skyBlockId.isBlank()) return false;

        String id = skyBlockId.toUpperCase(Locale.ROOT);
        for (String prefix : TOOL_ID_PREFIXES) {
            if (id.startsWith(prefix)) return true;
        }
        return false;
    }

    private void handleToggleKey() {
        boolean down = isKeyHeld(toggleKey.getKey());
        if (down && !toggleWasDown) {
            this.toggle();
            announceToggle();
        }
        toggleWasDown = down;
    }

    private void announceToggle() {
        if (this.isEnabled()) {
            ChatUtils.sendMessage("Farming Tools Keys enabled", ChatUtils.MessageType.SUCCESS);
        } else {
            ChatUtils.sendMessage("Farming Tools Keys disabled", ChatUtils.MessageType.ERROR);
        }
    }

    private boolean isKeyHeld(int keyCode) {
        if (keyCode == UNBOUND || mc == null || mc.getWindow() == null) return false;

        try {
            long handle = mc.getWindow().handle();
            if (keyCode >= LOWEST_MOUSE_CODE && keyCode <= HIGHEST_MOUSE_CODE) {
                return GLFW.glfwGetMouseButton(handle, keyCode - MOUSE_BASE) == GLFW.GLFW_PRESS;
            }
            return GLFW.glfwGetKey(handle, keyCode) == GLFW.GLFW_PRESS;
        } catch (Exception ignored) {
            return false;
        }
    }
}
