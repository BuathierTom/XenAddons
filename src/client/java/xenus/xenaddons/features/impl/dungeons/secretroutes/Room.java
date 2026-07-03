package xenus.xenaddons.features.impl.dungeons.secretroutes;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import xenus.xenaddons.features.impl.dungeons.secretroutes.dungeon.DungeonScanner;
import xenus.xenaddons.features.impl.dungeons.secretroutes.multistorage.Triple;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

public class Room {
    public enum WAYPOINT_TYPES { LOCATIONS, ETHERWARPS, MINES, INTERACTS, TNTS, ENDERPEARLS }
    public enum SECRET_TYPES { INTERACT, ITEM, BAT, EXITROUTE }

    public String name;
    public JsonArray currentSecretRoute;
    public int currentSecretIndex = 0;
    public JsonObject currentSecretWaypoints;
    public ArrayList<JsonArray> arrays = new ArrayList<>();
    public Triple<String, Integer, Double> closest = null;
    int c = 0;

    public Room(String roomName) {
        currentSecretIndex = 0;
        try {
            name = roomName;
            if (roomName != null) {
                getData();
            } else {
                currentSecretRoute = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void lastSecretKeybind() {
        if (currentSecretIndex > 0) currentSecretIndex--;
        updateWaypoints();
    }

    public void nextSecret() {
        currentSecretIndex++;
        updateWaypoints();
    }

    public void nextSecretKeybind() {
        if (currentSecretRoute != null && currentSecretIndex < currentSecretRoute.size() - 1) {
            currentSecretIndex++;
        }
        updateWaypoints();
    }

    private void updateWaypoints() {
        if (currentSecretRoute != null && currentSecretIndex < currentSecretRoute.size()) {
            currentSecretWaypoints = currentSecretRoute.get(currentSecretIndex).getAsJsonObject();
        } else {
            currentSecretWaypoints = null;
            if (currentSecretRoute != null && currentSecretIndex >= currentSecretRoute.size()) {
                SecretRouteProgressTracker.markCurrentRoomCompleted();
            }
        }
    }

    public SECRET_TYPES getSecretType() {
        try {
            if (currentSecretWaypoints != null && currentSecretWaypoints.has("secret")) {
                String type = currentSecretWaypoints.get("secret").getAsJsonObject().get("type").getAsString();
                return switch (type) {
                    case "interact" -> SECRET_TYPES.INTERACT;
                    case "item" -> SECRET_TYPES.ITEM;
                    case "bat" -> SECRET_TYPES.BAT;
                    case "exitroute" -> SECRET_TYPES.EXITROUTE;
                    default -> null;
                };
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public BlockPos getSecretLocation() {
        if (currentSecretWaypoints == null || !currentSecretWaypoints.has("secret")) return null;
        JsonArray location = currentSecretWaypoints.get("secret").getAsJsonObject().get("location").getAsJsonArray();

        return RoomRotationUtils.relativeToActual(
                new BlockPos(location.get(0).getAsInt(), location.get(1).getAsInt(), location.get(2).getAsInt()),
                RoomDirectionUtils.roomDirection(),
                RoomDirectionUtils.roomCorner()
        );
    }

    public boolean isCompleted() {
        return currentSecretRoute != null && currentSecretIndex >= currentSecretRoute.size();
    }

    public void getData() {
        new Thread(() -> {
            try {
                if (DungeonScanner.currentRoom == null || DungeonScanner.currentRoom.getName() == null) {
                    System.out.println("[Room] Cannot load data: Current room is null");
                    return;
                }

                Gson gson = new GsonBuilder().create();
                String resourcePath = "/assets/xenaddons/dungeons/routes.json";
                java.io.InputStream stream = Room.class.getResourceAsStream(resourcePath);
                if (stream == null) {
                    System.err.println("[Room] Could not find /assets/xenaddons/dungeons/routes.json");
                    return;
                }
                Reader reader = new InputStreamReader(stream);
                JsonObject rawData = gson.fromJson(reader, JsonObject.class);
                reader.close();

                if (rawData == null || rawData.isJsonNull()) {
                    System.err.println("[Room] Failed to parse routes.json");
                    currentSecretRoute = null;
                    return;
                }

                Map<String, JsonElement> data = new HashMap<>();
                for (Map.Entry<String, JsonElement> entry : rawData.entrySet()) {
                    data.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
                }

                HashMap<String, Integer> map = new HashMap<>();

                for (int i = 0; i <= 10; i++) {
                    String path = (i == 0 ? name : name + ":" + i).toLowerCase(Locale.ROOT);
                    JsonElement element = data.get(path);
                    if (element == null || element.isJsonNull()) {
                        if (i == 0) currentSecretRoute = null;
                        continue;
                    }

                    JsonArray route = element.getAsJsonArray();
                    arrays.add(route);
                    JsonArray starPoseArray = route.get(0).getAsJsonObject().get("locations").getAsJsonArray().get(0).getAsJsonArray();
                    BlockPos startPos = new BlockPos(starPoseArray.get(0).getAsInt(), starPoseArray.get(1).getAsInt(), starPoseArray.get(2).getAsInt());
                    map.put(BlockUtils.blockPos(startPos), arrays.size() - 1);
                }

                for (Map.Entry<String, Integer> entry : map.entrySet()) {
                    LocalPlayer p = Minecraft.getInstance().player;
                    if (p == null) continue;
                    BlockPos pPos = p.blockPosition();
                    if (pPos == null) continue;

                    BlockPos relPos = RoomRotationUtils.actualToRelative(pPos, RoomDirectionUtils.roomDirection(), RoomDirectionUtils.roomCorner());

                    double dist1 = BlockUtils.blockDistance(relPos, entry.getKey());

                    if (closest != null) {
                        double dist2 = closest.getThree();
                        if (dist1 > dist2) continue;
                    }

                    closest = new Triple<>(entry.getKey(), entry.getValue(), dist1);
                }

                if (closest != null) {
                    currentSecretRoute = arrays.get(closest.getTwo());
                    currentSecretWaypoints = currentSecretRoute.get(currentSecretIndex).getAsJsonObject();
                    System.out.println("[Room] Loaded route for " + name + " with " + currentSecretRoute.size() + " steps");
                } else {
                    System.out.println("[Room] No route found for " + name);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
