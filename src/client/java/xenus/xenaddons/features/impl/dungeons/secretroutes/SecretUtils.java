package xenus.xenaddons.features.impl.dungeons.secretroutes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import xenus.xenaddons.features.impl.dungeons.SecretRoutesModule;
import xenus.xenaddons.features.impl.dungeons.secretroutes.multistorage.Triple;

import java.awt.Color;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SecretUtils {
    public static JsonArray secrets = null;
    public static boolean renderLever = false;
    public static BlockPos currentLeverPos = null;
    public static BlockPos lastInteract;
    public static Long removeBannerTime = null;
    public static boolean first = true;
    public static String chestName = null;
    public static String leverName = null;
    public static String leverNumber = null;
    public static ArrayList<String> secretLocations = new ArrayList<>();

    public static void renderingCallback(JsonObject currentSecretWaypoints, int index2) {
        SecretRoutesModule config = SecretRoutesModule.INSTANCE;
        if (config == null || currentSecretWaypoints == null) return;

        int currentIndex = OnEnterNewRoom.currentRoom.currentSecretIndex;
        boolean isCurrentSecret = (index2 == currentIndex);
        boolean shouldRenderWaypoints = isCurrentSecret;

        // Player to waypoint line
        if (isCurrentSecret && config.playerWaypointLine.getValue()) {
            BlockPos secretPos = OnEnterNewRoom.currentRoom.getSecretLocation();
            if (secretPos != null) {
                Vec3 target = new Vec3(secretPos.getX() + 0.5, secretPos.getY() + 0.5, secretPos.getZ() + 0.5);
                AnotherRenderingUtil.addLineFromCursor(new RenderTypes.LineFromCursor(target, new Color(config.lineColor.getValue()), (float)config.lineWidth.getValue().doubleValue(), false));
            }
        }

        // Render the etherwarps
        if (config.renderEtherwarps.getValue() && shouldRenderWaypoints && currentSecretWaypoints.get("etherwarps") != null) {
            JsonArray etherwarpLocations = currentSecretWaypoints.get("etherwarps").getAsJsonArray();
            for (JsonElement etherwarpLocationElement : etherwarpLocations) {
                JsonArray etherwarpLocation = etherwarpLocationElement.getAsJsonArray();
                BlockPos pos = RoomRotationUtils.relativeToActual(new BlockPos(etherwarpLocation.get(0).getAsInt(), etherwarpLocation.get(1).getAsInt(), etherwarpLocation.get(2).getAsInt()), RoomDirectionUtils.roomDirection(), RoomDirectionUtils.roomCorner());
                Vec3 position = new Vec3(pos.getX(), pos.getY(), pos.getZ());
                Color color = new Color(128, 0, 128);
                if (config.etherwarpFullBlock.getValue()) {
                    AnotherRenderingUtil.addFilledBox(new RenderTypes.FilledBox(position, color, 1, 1, true));
                } else {
                    AnotherRenderingUtil.addOutlinedBox(new RenderTypes.OutlinedBox(position, color, 1, 1, true));
                }
            }
        }

        // Render the mines
        if (config.renderMines.getValue() && shouldRenderWaypoints && currentSecretWaypoints.get("mines") != null) {
            JsonArray mineLocations = currentSecretWaypoints.get("mines").getAsJsonArray();
            for (JsonElement mineLocationElement : mineLocations) {
                JsonArray mineLocation = mineLocationElement.getAsJsonArray();
                BlockPos pos = RoomRotationUtils.relativeToActual(new BlockPos(mineLocation.get(0).getAsInt(), mineLocation.get(1).getAsInt(), mineLocation.get(2).getAsInt()), RoomDirectionUtils.roomDirection(), RoomDirectionUtils.roomCorner());
                Vec3 position = new Vec3(pos.getX(), pos.getY(), pos.getZ());
                if (config.mineFullBlock.getValue()) {
                    AnotherRenderingUtil.addFilledBox(new RenderTypes.FilledBox(position, Color.YELLOW, 1, 1, true));
                } else {
                    AnotherRenderingUtil.addOutlinedBox(new RenderTypes.OutlinedBox(position, Color.YELLOW, 1, 1, true));
                }
            }
        }

        // Render the interacts
        if (config.renderInteracts.getValue() && shouldRenderWaypoints && currentSecretWaypoints.get("interacts") != null) {
            JsonArray interactLocations = currentSecretWaypoints.get("interacts").getAsJsonArray();
            for (JsonElement interactLocationElement : interactLocations) {
                JsonArray interactLocation = interactLocationElement.getAsJsonArray();
                BlockPos pos = RoomRotationUtils.relativeToActual(new BlockPos(interactLocation.get(0).getAsInt(), interactLocation.get(1).getAsInt(), interactLocation.get(2).getAsInt()), RoomDirectionUtils.roomDirection(), RoomDirectionUtils.roomCorner());
                Vec3 position = new Vec3(pos.getX(), pos.getY(), pos.getZ());
                if (config.interactsFullBlock.getValue()) {
                    AnotherRenderingUtil.addFilledBox(new RenderTypes.FilledBox(position, Color.BLUE, 1, 1, true));
                } else {
                    AnotherRenderingUtil.addOutlinedBox(new RenderTypes.OutlinedBox(position, Color.BLUE, 1, 1, true));
                }
            }
        }

        // Render the tnts (Superbooms)
        if (config.renderSuperboom.getValue() && shouldRenderWaypoints && currentSecretWaypoints.get("tnts") != null) {
            JsonArray tntLocations = currentSecretWaypoints.get("tnts").getAsJsonArray();
            for (JsonElement tntLocationElement : tntLocations) {
                JsonArray tntLocation = tntLocationElement.getAsJsonArray();
                BlockPos pos = RoomRotationUtils.relativeToActual(new BlockPos(tntLocation.get(0).getAsInt(), tntLocation.get(1).getAsInt(), tntLocation.get(2).getAsInt()), RoomDirectionUtils.roomDirection(), RoomDirectionUtils.roomCorner());
                Vec3 position = new Vec3(pos.getX(), pos.getY(), pos.getZ());
                if (config.superboomsFullBlock.getValue()) {
                    AnotherRenderingUtil.addFilledBox(new RenderTypes.FilledBox(position, Color.RED, 1, 1, true));
                } else {
                    AnotherRenderingUtil.addOutlinedBox(new RenderTypes.OutlinedBox(position, Color.RED, 1, 1, true));
                }
            }
        }

        // Render normal lines
        if (config.renderLines.getValue() && shouldRenderWaypoints && currentSecretWaypoints.get("locations") != null) {
            List<Triple<Double, Double, Double>> lines = new LinkedList<>();
            JsonArray lineLocations = currentSecretWaypoints.get("locations").getAsJsonArray();
            for (JsonElement lineLocationElement : lineLocations) {
                JsonArray lineLocation = lineLocationElement.getAsJsonArray();
                Triple<Double, Double, Double> linePos = RoomRotationUtils.relativeToActual(lineLocation.get(0).getAsDouble(), lineLocation.get(1).getAsDouble(), lineLocation.get(2).getAsDouble(), RoomDirectionUtils.roomDirection(), RoomDirectionUtils.roomCorner());
                linePos.setOne(linePos.getOne() + 0.5);
                linePos.setTwo(linePos.getTwo() + 0.5);
                linePos.setThree(linePos.getThree() + 0.5);
                lines.add(linePos);
            }

            Vec3[] linePoints = new Vec3[lines.size()];
            for (int j = 0; j < lines.size(); j++) {
                Triple<Double, Double, Double> line = lines.get(j);
                linePoints[j] = new Vec3(line.getOne(), line.getTwo(), line.getThree());
            }

            AnotherRenderingUtil.addLinesFromPoints(linePoints, new Color(config.lineColor.getValue()), (float)config.lineWidth.getValue().doubleValue(), false);
        }

        // Render "Start" on the first point of each rendered route (visible through blocks).
        if (shouldRenderWaypoints && currentSecretWaypoints.has("locations")) {
            JsonArray currentLocations = currentSecretWaypoints.get("locations").getAsJsonArray();
            if (!currentLocations.isEmpty()) {
                JsonArray startCoords = currentLocations.get(0).getAsJsonArray();
                Triple<Double, Double, Double> startPos = RoomRotationUtils.relativeToActual(
                        startCoords.get(0).getAsDouble(),
                        startCoords.get(1).getAsDouble(),
                        startCoords.get(2).getAsDouble(),
                        RoomDirectionUtils.roomDirection(),
                        RoomDirectionUtils.roomCorner()
                );
                AnotherRenderingUtil.addWorldText(new RenderTypes.WorldText(
                        Component.literal("Start"),
                        new Vec3(startPos.getOne() + 0.5, startPos.getTwo() + 1.2, startPos.getThree() + 0.5),
                        true,
                        1.8f
                ));
            }
        }

        // Render Secrets (Interact, Item, Bat)
        if (currentSecretWaypoints.get("secret") != null) {
            JsonObject secret = currentSecretWaypoints.get("secret").getAsJsonObject();
            String type = secret.get("type").getAsString();
            JsonArray location = secret.get("location").getAsJsonArray();
            BlockPos pos = RoomRotationUtils.relativeToActual(new BlockPos(location.get(0).getAsInt(), location.get(1).getAsInt(), location.get(2).getAsInt()), RoomDirectionUtils.roomDirection(), RoomDirectionUtils.roomCorner());

            boolean shouldRender = false;
            boolean isFullBlock = false;
            Color color = Color.CYAN;
            String textStr = "Secret";
            switch (type) {
                case "interact": 
                    shouldRender = config.renderSecretInteract.getValue();
                    isFullBlock = config.secretsInteractFullBlock.getValue();
                    color = Color.BLUE; textStr = "Interact"; break;
                case "item": 
                    shouldRender = config.renderSecretsItem.getValue();
                    isFullBlock = config.secretsItemFullBlock.getValue();
                    color = Color.GREEN; textStr = "Item"; break;
                case "bat": 
                    shouldRender = config.renderSecretBat.getValue();
                    isFullBlock = config.secretsBatFullBlock.getValue();
                    color = Color.GREEN; textStr = "Bat"; break;
            }
            
            if (shouldRender) {
                Vec3 position = new Vec3(pos.getX(), pos.getY(), pos.getZ());
                if (isFullBlock) {
                    AnotherRenderingUtil.addFilledBox(new RenderTypes.FilledBox(position, color, 1, 1, true));
                } else {
                    AnotherRenderingUtil.addOutlinedBox(new RenderTypes.OutlinedBox(position, color, 1, 1, true));
                }
                AnotherRenderingUtil.addWorldText(new RenderTypes.WorldText(Component.literal(textStr), position.add(0.5, 0.5, 0.5), true, 1.0f));
            }
        }
    }

    public static void renderSecrets() {
        SecretRoutesModule config = SecretRoutesModule.INSTANCE;
        if (config == null) return;
        
        secrets = getSecrets();
        if (secrets != null) {
            for (JsonElement secret : secrets) {
                JsonObject secretInfos = secret.getAsJsonObject();
                String name = secretInfos.get("secretName").getAsString();

                int xPos = secretInfos.get("x").getAsInt();
                int yPos = secretInfos.get("y").getAsInt();
                int zPos = secretInfos.get("z").getAsInt();
                
                if (secretLocations.contains(BlockUtils.blockPos(new BlockPos(xPos, yPos, zPos)))) {
                    continue;
                }

                boolean shouldRender = false;
                Color color = Color.WHITE;
                
                if (name.contains("Chest") || name.contains("Wither Essence") || name.contains("Lever")) {
                    shouldRender = config.renderSecretInteract.getValue();
                    color = Color.BLUE;
                } else if (name.contains("Bat")) {
                    shouldRender = config.renderSecretBat.getValue();
                    color = Color.GREEN;
                } else if (name.contains("Item")) {
                    shouldRender = config.renderSecretsItem.getValue();
                    color = Color.GREEN;
                }
                
                if (shouldRender) {
                    Triple<Double, Double, Double> abs = RoomRotationUtils.relativeToActual(xPos, yPos, zPos, RoomDirectionUtils.roomDirection(), RoomDirectionUtils.roomCorner());
                    Vec3 position = new Vec3(abs.getOne(), abs.getTwo(), abs.getThree());
                    AnotherRenderingUtil.addOutlinedBox(new RenderTypes.OutlinedBox(position, color, 1f, 1f, true));
                }
            }
        }
    }

    public static JsonArray getSecrets() {
        if (OnEnterNewRoom.currentRoom == null || OnEnterNewRoom.currentRoom.name == null) return null;
        String roomName = OnEnterNewRoom.currentRoom.name.toLowerCase();
        if (secrets == null) {
            try (Reader reader = new InputStreamReader(SecretUtils.class.getResourceAsStream("/assets/xenaddons/dungeons/secretlocations.json"))) {
                JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
                for (String key : object.keySet()) {
                    if (key.equalsIgnoreCase(roomName)) {
                        secrets = object.getAsJsonArray(key);
                        break;
                    }
                }
                return secrets;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        } else {
            return secrets;
        }
    }

    public static void resetValues() {
        renderLever = false;
        currentLeverPos = null;
        removeBannerTime = null;
        first = true;
        chestName = null;
        leverName = null;
        leverNumber = null;
    }

    public static void renderRouteStartLabel() {
        try {
            Room room = OnEnterNewRoom.currentRoom;
            if (room == null || room.currentSecretRoute == null || room.currentSecretRoute.isEmpty()) return;

            JsonObject firstStep = room.currentSecretRoute.get(0).getAsJsonObject();
            if (!firstStep.has("locations")) return;
            JsonArray locations = firstStep.get("locations").getAsJsonArray();
            if (locations.isEmpty()) return;

            JsonArray startCoords = locations.get(0).getAsJsonArray();
            Triple<Double, Double, Double> start = RoomRotationUtils.relativeToActual(
                startCoords.get(0).getAsDouble(),
                startCoords.get(1).getAsDouble(),
                startCoords.get(2).getAsDouble(),
                RoomDirectionUtils.roomDirection(),
                RoomDirectionUtils.roomCorner()
            );

            AnotherRenderingUtil.addWorldText(new RenderTypes.WorldText(
                Component.literal("Start"),
                new Vec3(start.getOne() + 0.5, start.getTwo() + 1.3, start.getThree() + 0.5),
                true,
                3.0f
            ));
        } catch (Exception ignored) {
        }
    }

    public static void renderLockedChestLever() {
        if (!renderLever || lastInteract == null) return;

        SecretRoutesModule config = SecretRoutesModule.INSTANCE;
        if (config == null) return;

        try {
            JsonArray roomSecrets = getSecrets();
            if (roomSecrets == null) {
                renderLever = false;
                return;
            }

            if (currentLeverPos == null) {
                BlockPos relativeLastInteract = RoomRotationUtils.actualToRelative(
                        lastInteract,
                        RoomDirectionUtils.roomDirection(),
                        RoomDirectionUtils.roomCorner()
                );

                String targetLeverNumbers = null;
                for (JsonElement element : roomSecrets) {
                    JsonObject secretInfos = element.getAsJsonObject();
                    if (!secretInfos.has("category") || !secretInfos.has("secretName")) continue;

                    String category = secretInfos.get("category").getAsString();
                    if (!"chest".equalsIgnoreCase(category)) continue;

                    int x = secretInfos.get("x").getAsInt();
                    int y = secretInfos.get("y").getAsInt();
                    int z = secretInfos.get("z").getAsInt();

                    if (relativeLastInteract.getX() == x && relativeLastInteract.getY() == y && relativeLastInteract.getZ() == z) {
                        chestName = secretInfos.get("secretName").getAsString();
                        targetLeverNumbers = chestName.split(" ")[0];
                        leverNumber = targetLeverNumbers;
                        break;
                    }
                }

                if (targetLeverNumbers != null) {
                    String[] requiredNumbers = targetLeverNumbers.split("/");
                    for (JsonElement element : roomSecrets) {
                        JsonObject secretInfos = element.getAsJsonObject();
                        if (!secretInfos.has("category") || !secretInfos.has("secretName")) continue;

                        String category = secretInfos.get("category").getAsString();
                        if (!"lever".equalsIgnoreCase(category)) continue;

                        String leverSecretName = secretInfos.get("secretName").getAsString();
                        boolean match = false;
                        for (String num : requiredNumbers) {
                            if (leverSecretName.contains(num)) {
                                match = true;
                                break;
                            }
                        }

                        if (match) {
                            currentLeverPos = new BlockPos(
                                    secretInfos.get("x").getAsInt(),
                                    secretInfos.get("y").getAsInt(),
                                    secretInfos.get("z").getAsInt()
                            );
                            leverName = leverSecretName;
                            break;
                        }
                    }
                }
            }

            if (currentLeverPos != null) {
                BlockPos absLeverPos = RoomRotationUtils.relativeToActual(
                        currentLeverPos,
                        RoomDirectionUtils.roomDirection(),
                        RoomDirectionUtils.roomCorner()
                );

                Vec3 position = new Vec3(absLeverPos.getX(), absLeverPos.getY(), absLeverPos.getZ());
                Color color = Color.BLUE;
                if (config.secretsInteractFullBlock.getValue()) {
                    AnotherRenderingUtil.addFilledBox(new RenderTypes.FilledBox(position, color, 1, 1, true));
                } else {
                    AnotherRenderingUtil.addOutlinedBox(new RenderTypes.OutlinedBox(position, color, 1, 1, true));
                }
                AnotherRenderingUtil.addWorldText(new RenderTypes.WorldText(
                        Component.literal("Locked chest lever"),
                        position.add(0.5, 1.0, 0.5),
                        true,
                        1.3f
                ));
            }
        } catch (Exception ignored) {
        }
    }
}
