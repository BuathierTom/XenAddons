package xenus.xenaddons.features.impl.slayer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.world.entity.Entity;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.utils.slayer.SlayerTracker;

/**
 * Alerts when the slayer boss you spawned shows up. There is no dedicated chat line for it, so this
 * watches the boss the tracker resolves from the nametags instead.
 */
public class SlayerBossAlertModule extends SlayerAlertModule {

    // The boss briefly disappearing (chunk reload, entity swap) must not re-trigger the alert.
    private static final long SPAWN_COOLDOWN_MS = 5_000L;

    private Entity lastAlertedBoss = null;
    private long lastAlertAt = 0L;

    public SlayerBossAlertModule() {
        super("Boss Alert", "Alerts when your slayer boss spawns",
                "slayer_boss_alert_hud", "&cBoss!", 140, 3.0);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!this.isEnabled()) {
                lastAlertedBoss = null;
                return;
            }

            Entity boss = SlayerTracker.getCurrentBoss();
            if (boss == null) {
                lastAlertedBoss = null;
                return;
            }
            if (boss == lastAlertedBoss) return;

            lastAlertedBoss = boss;
            long now = System.currentTimeMillis();
            if (now - lastAlertAt < SPAWN_COOLDOWN_MS) return;
            lastAlertAt = now;

            fireAlert();
        });
    }

    public static boolean isActive() {
        SlayerBossAlertModule module = ModuleManager.getModule(SlayerBossAlertModule.class);
        return module != null && module.isEnabled();
    }
}
