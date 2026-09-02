package xenus.xenaddons.utils.slayer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import xenus.xenaddons.features.impl.slayer.SlayerBossAlertModule;
import xenus.xenaddons.features.impl.slayer.SlayerBossHudModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keeps track of the slayer boss the local player spawned, and of the nametag lines to show for it.
 *
 * Ported from Athen's SlayerAPI (BSD 3-Clause, Copyright (c) 2025-2026 Starred), simplified: we
 * only need the boss the player owns, and we read ownership off the attached nametags rather than
 * off entity-id offsets.
 */
public final class SlayerTracker {

    // Weak keys: when the boss entity is unloaded or killed the entry disappears on its own.
    private static final Map<Entity, Boolean> TRACKED = Collections.synchronizedMap(new WeakHashMap<>());

    private static volatile Entity current;
    private static volatile List<Component> currentLines = Collections.emptyList();

    private SlayerTracker() {
    }

    /** Registered once from ModuleManager: the tracker is shared by every module that needs it. */
    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!isTrackingWanted()) {
                if (current != null || !TRACKED.isEmpty()) reset();
                return;
            }
            tick();
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
    }

    /** Nothing reads the nametag links unless one of these is on, so this gates the whole thing. */
    public static boolean isTrackingWanted() {
        return SlayerBossHudModule.isActive() || SlayerBossAlertModule.isActive();
    }

    /** Called once a nametag has been linked to a mob; records the mob if the tag names a boss. */
    public static void onAttach(Entity stand, Entity mob) {
        Component name = stand.getCustomName();
        if (name == null || mob == null) return;
        if (!SlayerTypes.isBossNametag(SlayerTypes.stripFormatting(name.getString()))) return;
        TRACKED.put(mob, Boolean.TRUE);
    }

    /**
     * Re-picks the owned boss every tick. Ownership is re-evaluated rather than cached because the
     * "Spawned by:" stand often attaches a few ticks after the name stand.
     */
    private static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            reset();
            return;
        }

        String self = client.player.getGameProfile().name();
        Entity owned = null;
        double ownedDistance = Double.MAX_VALUE;

        synchronized (TRACKED) {
            Iterator<Entity> iterator = TRACKED.keySet().iterator();
            while (iterator.hasNext()) {
                Entity boss = iterator.next();
                if (boss == null || !boss.isAlive() || boss.isRemoved()) {
                    iterator.remove();
                    continue;
                }
                if (!isOwnedBy(boss, self)) continue;

                double distance = client.player.distanceToSqr(boss);
                if (owned == null || distance < ownedDistance) {
                    owned = boss;
                    ownedDistance = distance;
                }
            }
        }

        current = owned;
        currentLines = owned == null ? Collections.emptyList() : displayLines(owned);
    }

    public static Entity getCurrentBoss() {
        return current;
    }

    /** Timer line first, then the "☠ Name Tier 500❤" line, mirroring how Athen displays them. */
    public static List<Component> getDisplayLines() {
        return currentLines;
    }

    public static void reset() {
        TRACKED.clear();
        current = null;
        currentLines = Collections.emptyList();
    }

    private static boolean isOwnedBy(Entity boss, String playerName) {
        if (playerName == null) return false;
        for (Component line : SlayerNametags.attachedNames(boss)) {
            String owner = SlayerTypes.ownerOf(SlayerTypes.stripFormatting(line.getString()));
            if (owner != null && owner.equalsIgnoreCase(playerName)) return true;
        }
        return false;
    }

    private static List<Component> displayLines(Entity boss) {
        Component timer = null;
        Component name = null;

        for (Component line : SlayerNametags.attachedNames(boss)) {
            String plain = SlayerTypes.stripFormatting(line.getString());
            if (SlayerTypes.isSpawnedByTag(plain)) continue;

            // The timer stand is the only remaining one shaped like "02:46".
            if (timer == null && plain.contains(":")) timer = line;
            if (name == null && SlayerTypes.containsBossName(plain)) name = line;
            if (timer != null && name != null) break;
        }

        List<Component> lines = new ArrayList<>(2);
        if (timer != null) lines.add(timer);
        if (name != null) lines.add(name);
        return lines;
    }
}
