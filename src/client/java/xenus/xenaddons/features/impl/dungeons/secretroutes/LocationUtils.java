package xenus.xenaddons.features.impl.dungeons.secretroutes;

import xenus.xenaddons.utils.DungeonUtils;

public class LocationUtils {
    public static void init() {
        // Already handled by DungeonUtils
    }

    public static boolean isInDungeons() {
        return DungeonUtils.inDungeon();
    }

    public static boolean isOnSkyblock() {
        // XenAddons is primarily for Skyblock Hypixel anyway
        return true; 
    }

    public static void reset() {
        // Handled by DungeonUtils
    }
}
