package xenus.xenaddons.features.impl.dungeons;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.awt.Point;

public class DungeonRoomManager {
    private static String roomName = "undefined";
    private static String rotation = "NW";
    private static Point corner = new Point(0, 0);

    public static void updateCurrentRoom(Vec3d pos) {
        // Hypixel Dungeons use a 32x32 grid shifted by 8 blocks
        double shiftedX = pos.x + 8.5; 
        double shiftedZ = pos.z + 8.5;
        
        int gridX = (int) (shiftedX - Math.floorMod((int) shiftedX, 32));
        int gridZ = (int) (shiftedZ - Math.floorMod((int) shiftedZ, 32));
        
        corner = new Point(gridX - 8, gridZ - 8);
    }

    public static String getRoomName() {
        return roomName;
    }

    public static void setRoomName(String name) {
        roomName = name;
    }

    public static String getRotation() {
        return rotation;
    }

    public static void setRotation(String rot) {
        rotation = rot;
    }

    public static Point getCorner() {
        return corner;
    }

    public static BlockPos relativeToActual(int relX, int relY, int relZ) {
        int x = 0;
        int z = 0;
        switch (rotation) {
            case "NW":
                x = relX + corner.x;
                z = relZ + corner.y;
                break;
            case "NE":
                x = -(relZ - corner.x);
                z = relX + corner.y;
                break;
            case "SE":
                x = -(relX - corner.x);
                z = -(relZ - corner.y);
                break;
            case "SW":
                x = relZ + corner.x;
                z = -(relX - corner.y);
                break;
        }
        return new BlockPos(x, relY, z);
    }

    public static Vec3d relativeToActual(double relX, double relY, double relZ) {
        double x = 0;
        double z = 0;
        switch (rotation) {
            case "NW":
                x = relX + corner.x;
                z = relZ + corner.y;
                break;
            case "NE":
                x = -(relZ - corner.x);
                z = relX + corner.y;
                break;
            case "SE":
                x = -(relX - corner.x);
                z = -(relZ - corner.y);
                break;
            case "SW":
                x = relZ + corner.x;
                z = -(relX - corner.y);
                break;
        }
        return new Vec3d(x, relY, z);
    }
}
