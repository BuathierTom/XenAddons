package xenus.xenaddons.features.impl.dungeons.secretroutes;

import java.util.ArrayList;

public class OnEnterNewRoom {
    public static String lastKnownRoom = null;
    public static Room currentRoom = null;

    public static void checkForNewRoom() {
        if (!LocationUtils.isInDungeons()) return;

        String roomName = RoomDirectionUtils.roomName();

        if (roomName.equals("UNKNOWN")) return;

        if (lastKnownRoom == null || !lastKnownRoom.equals(roomName)) {
            lastKnownRoom = roomName;
            onEnterNewRoom(new Room(lastKnownRoom));
        }
    }

    public static void onEnterNewRoom(Room room) {
        try {
            if (!LocationUtils.isInDungeons()) {
                return;
            }

            currentRoom = room;
            SecretUtils.secrets = null;
            SecretUtils.secretLocations = new ArrayList<>();
            SecretUtils.resetValues();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
