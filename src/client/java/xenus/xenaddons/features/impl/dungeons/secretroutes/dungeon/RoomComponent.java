package xenus.xenaddons.features.impl.dungeons.secretroutes.dungeon;

import net.minecraft.core.BlockPos;
import org.joml.Vector2i;

public record RoomComponent(int x, int z, int core) {
    public Vector2i vec2() {
        return new Vector2i(x, z);
    }

    public BlockPos blockPos() {
        return new BlockPos(x, 70, z);
    }
}
