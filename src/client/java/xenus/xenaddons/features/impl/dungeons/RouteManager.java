package xenus.xenaddons.features.impl.dungeons;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RouteManager {
    private static final Gson GSON = new Gson();
    private static final Map<String, JsonArray> routes = new HashMap<>();
    private static String currentRoom = null;
    private static int currentStep = 0;

    public static void loadRoutes() {
        try {
            Identifier id = Identifier.of("xenaddons", "dungeons/routes.json");
            InputStream stream = MinecraftClient.getInstance().getResourceManager().getResource(id).get().getInputStream();
            JsonObject json = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            
            routes.clear();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                if (entry.getValue().isJsonArray()) {
                    routes.put(entry.getKey(), entry.getValue().getAsJsonArray());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static JsonArray getRoutesForRoom(String roomName) {
        return routes.get(roomName);
    }

    public static String getCurrentRoom() {
        return currentRoom;
    }

    public static void setCurrentRoom(String room) {
        currentRoom = room;
        currentStep = 0;
    }

    public static int getCurrentStep() {
        return currentStep;
    }

    public static void setCurrentStep(int step) {
        currentStep = step;
    }
}
