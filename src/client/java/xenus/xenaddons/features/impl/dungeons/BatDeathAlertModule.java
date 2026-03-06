package xenus.xenaddons.features.impl.dungeons;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.text.Text;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.utils.DungeonUtils;

public class BatDeathAlertModule extends Module {
    private static long lastAlertAt = 0L;
    private static final long ALERT_COOLDOWN_MS = 300L;

    public BatDeathAlertModule() {
        super("Bat Death Alert", "Shows a title when bats die in dungeons", Category.DUNGEON);
    }

    public static void init() {
        // No periodic polling needed; we react to true entity death events from mixin hooks.
    }

    public static void onBatDeath(BatEntity bat) {
        BatDeathAlertModule module = ModuleManager.getModule(BatDeathAlertModule.class);
        MinecraftClient client = MinecraftClient.getInstance();
        if (module == null || !module.isEnabled() || client == null || client.world == null || client.player == null) return;
        if (!DungeonUtils.inDungeon() || DungeonUtils.currentPhase > 0) return;
        if (bat == null) return;
        if (bat.hasVehicle() && bat.getVehicle() instanceof ArmorStandEntity) return;

        long now = System.currentTimeMillis();
        if (now - lastAlertAt < ALERT_COOLDOWN_MS) return;
        lastAlertAt = now;

        if (client.inGameHud == null) return;
        client.inGameHud.setTitle(Text.of("§cBat Dead!"));
        client.inGameHud.setTitleTicks(5, 20, 5);
    }
}
