package xenus.xenaddons.features.impl.dungeons.secretroutes.dungeon;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.joml.Vector2i;
import xenus.xenaddons.features.impl.dungeons.secretroutes.LocationUtils;
import xenus.xenaddons.features.impl.dungeons.secretroutes.OnEnterNewRoom;
import xenus.xenaddons.features.impl.dungeons.secretroutes.Room;
import xenus.xenaddons.features.impl.dungeons.secretroutes.SecretRouteProgressTracker;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

public class DungeonScanner {

    private static final Minecraft client = Minecraft.getInstance();
    private static final Gson GSON = new Gson();

    private static final Map<Integer, RoomData> CORE_TO_ROOM_MAP = new HashMap<>();

    public static DungeonRoom currentRoom = null;
    public static final Set<DungeonRoom> passedRooms = new HashSet<>();
    private static Vector2i lastRoomCentre = new Vector2i(Integer.MIN_VALUE, Integer.MIN_VALUE);
    private static int scanRetryCooldown = 0;

    private static final int ROOM_SIZE_SHIFT = 5;
    private static final int START_COORDINATE = -185;
    private static final List<Direction> HORIZONTALS = Arrays.stream(Direction.values())
            .filter(d -> d.getAxis().isHorizontal()).toList();

    public static void init() {
        ClientLifecycleEvents.CLIENT_STARTED.register(c -> loadResources());
        ClientPlayConnectionEvents.JOIN.register((h, s, c) -> reset());
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> reset());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) return;
            if (!LocationUtils.isInDungeons()) {
                if (currentRoom != null || !passedRooms.isEmpty()) {
                    reset();
                }
                return;
            }
            tick();
        });
    }

    private static void loadResources() {
        CORE_TO_ROOM_MAP.clear();
        Identifier roomsId = Identifier.fromNamespaceAndPath("xenaddons", "dungeons/rooms.json");
        Optional<Resource> resource = client.getResourceManager().getResource(roomsId);
        
        resource.ifPresent(res -> {
            try (Reader reader = new InputStreamReader(res.open())) {
                List<RoomData> data = GSON.fromJson(reader, new TypeToken<List<RoomData>>(){}.getType());
                for (RoomData room : data) {
                    if (room.cores() != null) {
                        for (Integer core : room.cores()) CORE_TO_ROOM_MAP.put(core, room);
                    }
                }
                System.out.println("[DungeonScanner] Loaded " + CORE_TO_ROOM_MAP.size() + " cores from rooms.json");
            } catch (Exception e) { 
                System.err.println("[DungeonScanner] Failed to load rooms.json");
                e.printStackTrace(); 
            }
        });
        if (!resource.isPresent()) {
            System.err.println("[DungeonScanner] rooms.json NOT FOUND!");
        }
    }

    private static void tick() {
        int playerX = (int) client.player.getX();
        int playerZ = (int) client.player.getZ();
        Vector2i roomCentre = getRoomCentre(playerX, playerZ);

        boolean sameCentre = roomCentre.equals(lastRoomCentre);
        if (sameCentre && currentRoom != null && hasComponent(currentRoom, roomCentre)) return;

        if (!sameCentre) {
            lastRoomCentre = roomCentre;
            scanRetryCooldown = 0;
        } else if (scanRetryCooldown > 0) {
            scanRetryCooldown--;
            return;
        }

        for (DungeonRoom passed : passedRooms) {
            for (RoomComponent comp : passed.roomComponents) {
                if (comp.vec2().equals(roomCentre)) {
                    if (currentRoom == null || !hasComponent(currentRoom, roomCentre)) {
                        enterRoom(passed);
                    }
                    return;
                }
            }
        }

        if (!client.level.hasChunk(roomCentre.x >> 4, roomCentre.y >> 4)) {
            scanRetryCooldown = 5;
            return;
        }

        DungeonRoom newRoom = scanRoom(roomCentre);
        if (newRoom != null && newRoom.rotation != Rotations.NONE) {
            scanRetryCooldown = 0;
            enterRoom(newRoom);
        } else {
            scanRetryCooldown = 10;
        }
    }

    private static void enterRoom(DungeonRoom room) {
        System.out.println("[DungeonScanner] Entering room: " + room.data.name() + " rotation: " + room.rotation);
        currentRoom = room;
        boolean exists = false;
        for(DungeonRoom r : passedRooms) {
            if(r.data.name().equals(room.data.name())) { exists = true; break; }
        }
        if(!exists) passedRooms.add(room);

        Room newRoomObj = new Room(room.data.name());
        OnEnterNewRoom.currentRoom = newRoomObj;
        OnEnterNewRoom.onEnterNewRoom(newRoomObj);
    }

    private static boolean hasComponent(DungeonRoom room, Vector2i target) {
        for (RoomComponent comp : room.roomComponents) {
            if (comp.vec2().equals(target)) return true;
        }
        return false;
    }

    private static DungeonRoom scanRoom(Vector2i vec2) {
        int roomHeight = getTopLayerOfRoom(vec2);
        int core = getCore(vec2, roomHeight);
        RoomData data = CORE_TO_ROOM_MAP.get(core);
        if (data != null) {
            Set<RoomComponent> components = findRoomComponentsRecursively(vec2, data.cores(), roomHeight, new HashSet<>(), new HashSet<>());
            DungeonRoom room = new DungeonRoom(data, components);
            updateRotation(room, roomHeight);
            return room;
        }
        return null;
    }

    private static Set<RoomComponent> findRoomComponentsRecursively(Vector2i vec2, List<Integer> cores, int roomHeight, Set<Vector2i> visited, Set<RoomComponent> tiles) {
        if (visited.contains(vec2)) return tiles;
        visited.add(vec2);
        int core = getCore(vec2, roomHeight);
        if (!cores.contains(core)) return tiles;
        tiles.add(new RoomComponent(vec2.x, vec2.y, core));
        for (Direction facing : HORIZONTALS) {
            int dx = (facing.getAxis() == Direction.Axis.X ? facing.getStepX() : 0) << ROOM_SIZE_SHIFT;
            int dz = (facing.getAxis() == Direction.Axis.Z ? facing.getStepZ() : 0) << ROOM_SIZE_SHIFT;
            findRoomComponentsRecursively(new Vector2i(vec2.x + dx, vec2.y + dz), cores, roomHeight, visited, tiles);
        }
        return tiles;
    }

    private static void updateRotation(DungeonRoom room, int roomHeight) {
        if ("Fairy".equals(room.data.name())) {
            if (!room.roomComponents.isEmpty()) {
                RoomComponent first = room.roomComponents.iterator().next();
                room.clayPos = new BlockPos(first.x() - 15, roomHeight, first.z() - 15);
                room.rotation = Rotations.SOUTH;
            }
            return;
        }
        List<Rotations> rotations = Arrays.asList(Rotations.NORTH, Rotations.SOUTH, Rotations.WEST, Rotations.EAST);
        for (Rotations rot : rotations) {
            for (RoomComponent comp : room.roomComponents) {
                BlockPos checkPos = new BlockPos(comp.x() + rot.x, roomHeight, comp.z() + rot.z);
                if (isBlueTerracotta(checkPos)) {
                    boolean neighborsValid = true;
                    if (room.roomComponents.size() > 1) {
                        for (Direction facing : HORIZONTALS) {
                            BlockPos neighbor = checkPos.relative(facing);
                            if (!isBlueTerracottaOrAir(neighbor)) { neighborsValid = false; break; }
                        }
                    }
                    if (neighborsValid) {
                        room.clayPos = checkPos;
                        room.rotation = rot;
                        return;
                    }
                }
            }
        }
        room.rotation = Rotations.NONE;
    }

    private static boolean isBlueTerracotta(BlockPos pos) {
        if (client.level == null) return false;
        return client.level.getBlockState(pos).getBlock() == Blocks.BLUE_TERRACOTTA;
    }

    private static boolean isBlueTerracottaOrAir(BlockPos pos) {
        if (client.level == null) return false;
        Block b = client.level.getBlockState(pos).getBlock();
        return b == Blocks.AIR || b == Blocks.BLUE_TERRACOTTA;
    }

    public static Vector2i getRoomCentre(int posX, int posZ) {
        int roomX = (posX - START_COORDINATE + (1 << (ROOM_SIZE_SHIFT - 1))) >> ROOM_SIZE_SHIFT;
        int roomZ = (posZ - START_COORDINATE + (1 << (ROOM_SIZE_SHIFT - 1))) >> ROOM_SIZE_SHIFT;
        return new Vector2i((roomX << ROOM_SIZE_SHIFT) + START_COORDINATE, (roomZ << ROOM_SIZE_SHIFT) + START_COORDINATE);
    }

    private static int getCore(Vector2i vec2, Integer knownHeight) {
        if (client.level == null) return 0;
        int roomHeight = (knownHeight != null ? knownHeight : getTopLayerOfRoom(vec2));
        int clampedHeight = Math.max(11, Math.min(140, roomHeight));
        StringBuilder sb = new StringBuilder(150);
        for (int i = 0; i < 140 - clampedHeight; i++) sb.append('0');
        int bedrock = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = clampedHeight; y >= 12; y--) {
            pos.set(vec2.x, y, vec2.y);
            Block block = client.level.getBlockState(pos).getBlock();
            if (block == Blocks.AIR && bedrock >= 2 && y < 69) {
                int remaining = y - 11;
                for(int k=0; k < remaining; k++) sb.append('0');
                break;
            }
            if (block == Blocks.BEDROCK) bedrock++;
            else {
                bedrock = 0;
                if (block == Blocks.OAK_PLANKS || block == Blocks.TRAPPED_CHEST || block == Blocks.CHEST) continue;
            }
            sb.append(block);
        }
        return sb.toString().hashCode();
    }

    private static int getTopLayerOfRoom(Vector2i vec2) {
        if (client.level == null) return 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = 160; y >= 12; y--) {
            pos.set(vec2.x, y, vec2.y);
            if (!client.level.getBlockState(pos).isAir()) {
                if (client.level.getBlockState(pos).getBlock() == Blocks.GOLD_BLOCK) return y - 1;
                return y;
            }
        }
        return 0;
    }

    private static void reset() {
        lastRoomCentre = new Vector2i(Integer.MIN_VALUE, Integer.MIN_VALUE);
        scanRetryCooldown = 0;
        currentRoom = null;
        passedRooms.clear();
        OnEnterNewRoom.currentRoom = null;
        OnEnterNewRoom.lastKnownRoom = null;
        SecretRouteProgressTracker.clear();
    }
}
