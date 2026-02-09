package xenus.xenaddons.features.impl.dungeons.secretroutes.dungeon;

import net.minecraft.util.math.BlockPos;
import java.util.Set;

public class DungeonRoom {
    public final RoomData data;
    public final Set<RoomComponent> roomComponents;
    public Rotations rotation = Rotations.NONE;
    public BlockPos clayPos = new BlockPos(0, 0, 0);

    public DungeonRoom(RoomData data, Set<RoomComponent> components) {
        this.data = data;
        this.roomComponents = components;
    }

    public String getName() {
        return data.name();
    }
}
