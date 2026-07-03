package xenus.xenaddons.features.impl.dungeons.secretroutes;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import xenus.xenaddons.compat.XenCameraCompat;
import xenus.xenaddons.features.impl.dungeons.secretroutes.multistorage.Triple;

public class BlockUtils {

    public static String blockPos(BlockPos pos) {
        if (pos == null) {
            return ":::";
        }
        return pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
    }

    public static BlockPos blockPos(String pos) {
        try {
            String[] parts = pos.split(":");
            return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (Exception e) {
            return null;
        }
    }


    public static boolean compareBlocks(BlockPos pos1, BlockPos pos2, int dist) {
        return pos1.getX() >= pos2.getX() - dist && pos1.getX() <= pos2.getX() + dist && pos1.getY() >= pos2.getY() - dist && pos1.getY() <= pos2.getY() + dist && pos1.getZ() >= pos2.getZ() - dist && pos1.getZ() <= pos2.getZ() + dist;
    }

    public static double blockDistance(BlockPos pos1, BlockPos pos2) {
        if (pos1 == null || pos2 == null) {
            return Integer.MAX_VALUE;
        }
        double xdiff = Math.abs(pos1.getX() - pos2.getX());
        double ydiff = Math.abs(pos1.getY() - pos2.getY());
        double zdiff = Math.abs(pos1.getZ() - pos2.getZ());
        return Math.sqrt(Math.pow(xdiff, 2) + Math.pow(ydiff, 2) + Math.pow(zdiff, 2));
    }

    public static double blockDistance(BlockPos pos1, String pos2) {
        BlockPos pos2Block = blockPos(pos2);
        if (pos2Block == null) {
            return Integer.MAX_VALUE;
        }
        return blockDistance(pos1, pos2Block);
    }

    public static BlockPos playerToWorld(BlockPos pos) {
        Triple<Integer, Integer, Integer> rel_pos = playerToWorld(pos.getX(), pos.getY(), pos.getZ());
        return new BlockPos(rel_pos.getOne(), rel_pos.getTwo(), rel_pos.getThree());
    }

    public static Triple<Integer, Integer, Integer> playerToWorld(int worldX, int worldY, int worldZ) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 cameraPos = XenCameraCompat.getPos(mc.gameRenderer.getMainCamera());
        int playerX = (int) cameraPos.x;
        int playerY = (int) cameraPos.y;
        int playerZ = (int) cameraPos.z;

        return new Triple<>(worldX - playerX, worldY - playerY, worldZ - playerZ);
    }

}
