package xenus.xenaddons.features.impl.dungeons.secretroutes.dungeon;

import java.util.List;

public record RoomData(
        String name,
        RoomType type,
        List<Integer> cores,
        int crypts,
        int secrets,
        int trappedChests
) {}
