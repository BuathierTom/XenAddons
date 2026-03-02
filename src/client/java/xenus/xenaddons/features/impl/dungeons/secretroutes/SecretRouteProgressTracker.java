package xenus.xenaddons.features.impl.dungeons.secretroutes;

import xenus.xenaddons.features.impl.dungeons.secretroutes.dungeon.DungeonRoom;
import xenus.xenaddons.features.impl.dungeons.secretroutes.dungeon.DungeonScanner;
import xenus.xenaddons.features.impl.dungeons.secretroutes.dungeon.RoomComponent;

import java.util.HashSet;
import java.util.Set;

public final class SecretRouteProgressTracker {
    private static final Set<String> completedRoomKeys = new HashSet<>();

    private SecretRouteProgressTracker() {}

    public static void markCurrentRoomCompleted() {
        String key = getCurrentRoomKey();
        if (key != null) {
            completedRoomKeys.add(key);
        }
    }

    public static boolean isCurrentRoomCompleted() {
        String key = getCurrentRoomKey();
        return key != null && completedRoomKeys.contains(key);
    }

    public static void clear() {
        completedRoomKeys.clear();
    }

    private static String getCurrentRoomKey() {
        DungeonRoom room = DungeonScanner.currentRoom;
        if (room == null || room.data == null || room.roomComponents == null || room.roomComponents.isEmpty()) {
            return null;
        }

        long minHash = Long.MAX_VALUE;
        for (RoomComponent component : room.roomComponents) {
            long h = (((long) component.x()) << 32) ^ (component.z() & 0xffffffffL);
            if (h < minHash) minHash = h;
        }

        return room.getName() + "#" + minHash;
    }
}
