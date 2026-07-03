package xenus.xenaddons.features.impl.dungeons;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.network.chat.Component;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.utils.DungeonUtils;

public class BatDeathAlertModule extends Module {
    private static long lastAlertAt = 0L;
    private static final long ALERT_COOLDOWN_MS = 300L;
    private static final double MAX_ALERT_DISTANCE = 6.0D;
    private static final double MAX_ALERT_DISTANCE_SQ = MAX_ALERT_DISTANCE * MAX_ALERT_DISTANCE;

    public BatDeathAlertModule() {
        super("Bat Death Alert", "Shows a title when bats die in dungeons", Category.DUNGEON);
    }

    public static void init() {
        // No periodic polling needed; we react to true entity death events from mixin hooks.
    }

    public static void onBatDeath(Bat bat) {
        BatDeathAlertModule module = ModuleManager.getModule(BatDeathAlertModule.class);
        Minecraft client = Minecraft.getInstance();
        if (module == null || !module.isEnabled() || client == null || client.level == null || client.player == null) return;
        if (!DungeonUtils.inDungeon() || DungeonUtils.currentPhase > 0) return;
        if (bat == null) return;
        if (bat.isPassenger() && bat.getVehicle() instanceof ArmorStand) return;
        if (client.player.distanceToSqr(bat) > MAX_ALERT_DISTANCE_SQ) return;

        long now = System.currentTimeMillis();
        if (now - lastAlertAt < ALERT_COOLDOWN_MS) return;
        lastAlertAt = now;

        if (client.gui == null) return;
        client.gui.setTitle(Component.literal("§cBat Dead!"));
        client.gui.setTimes(5, 20, 5);
    }
}
