package xenus.xenaddons.features.impl.dungeons;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.hud.HudState;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.utils.DungeonUtils;

public class LoadoutTitleModule extends Module {
    private static final Pattern EQUIPPED_LOADOUT_PATTERN = Pattern.compile("^You equipped (?!an?\\s)(.+)!$");
    private static final int NOTIFICATION_DURATION_TICKS = 8;

    private final BooleanSetting inDungeonsOnly = new BooleanSetting("In Dungeons Only", false);

    public LoadoutTitleModule() {
        super("Loadout Title", "Shows a title when you equip a loadout", Category.DUNGEON);
        registerSetting(inDungeonsOnly);
    }

    public static void init() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
            !checkAndProcess(message.getString())
        );
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
            !checkAndProcess(message.getString())
        );
    }

    private static boolean checkAndProcess(String message) {
        LoadoutTitleModule module = ModuleManager.getModule(LoadoutTitleModule.class);
        return module != null && module.isEnabled() && module.handleAndFilter(message);
    }

    private boolean handleAndFilter(String message) {
        Matcher matcher = EQUIPPED_LOADOUT_PATTERN.matcher(message.trim());
        if (!matcher.matches()) {
            return false;
        }

        String loadoutName = matcher.group(1).trim();
        if (loadoutName.isEmpty()) {
            return false;
        }

        mc.execute(() -> showNotification(loadoutName));
        return true;
    }

    private void showNotification(String loadoutName) {
        if (!inDungeonsOnly.getValue() || DungeonUtils.inDungeon()) {
            HudState.PET_NOTIFICATION.show(
                Component.literal("§a" + loadoutName),
                Component.literal("§bLoadout§r"),
                NOTIFICATION_DURATION_TICKS
            );
        }

        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("§bLoadout §7> §a" + loadoutName));
        }
    }

    public void debugTrigger() {
        showNotification("Farming");
    }
}
