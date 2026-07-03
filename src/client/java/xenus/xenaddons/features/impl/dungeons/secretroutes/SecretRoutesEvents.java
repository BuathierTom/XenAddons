package xenus.xenaddons.features.impl.dungeons.secretroutes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import xenus.xenaddons.features.impl.dungeons.SecretRoutesModule;

import java.util.HashMap;
import java.util.Map;

public class SecretRoutesEvents {

    private static final long INTERACT_COOLDOWN = 110;
    private static long lastInteractTime = 0;
    private static BlockPos lastInteractPos = null;

    private static final Map<String, Integer> previousInventory = new HashMap<>();
    private static int tickCounter = 0;
    private static long itemProximityStartMillis = 0L;
    private static BlockPos itemProximityTarget = null;
    private static long interactProximityStartMillis = 0L;
    private static BlockPos interactProximityTarget = null;

    public static final String[] VALID_SECRET_ITEMS = {
            "Decoy", "Defuse Kit", "Dungeon Chest Key", "Healing VIII",
            "Inflatable Jerry", "Spirit Leap", "Training Weights", "Trap", "Treasure Talisman"
    };

    public static void init() {
        UseBlockCallback.EVENT.register(SecretRoutesEvents::onUseBlock);
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> handleLockedChestMessage(message.getString()));
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            handleLockedChestMessage(message.getString());
            return true;
        });
    }

    public static InteractionResult onUseBlock(net.minecraft.world.entity.player.Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        if (!SecretRoutesModule.INSTANCE.isEnabled()) return InteractionResult.PASS;
        if (!LocationUtils.isInDungeons()) return InteractionResult.PASS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        long currentTime = System.currentTimeMillis();
        BlockPos pos = hitResult.getBlockPos();

        if (lastInteractPos != null && lastInteractPos.equals(pos) && (currentTime - lastInteractTime) < INTERACT_COOLDOWN) {
            return InteractionResult.PASS;
        }

        lastInteractTime = currentTime;
        lastInteractPos = pos;
        SecretUtils.lastInteract = pos;

        Block block = world.getBlockState(pos).getBlock();

        // Check if interactable block
        if (block != Blocks.CHEST && block != Blocks.TRAPPED_CHEST && block != Blocks.LEVER && block != Blocks.PLAYER_HEAD && block != Blocks.SKELETON_SKULL) {
            return InteractionResult.PASS;
        }

        Room currentRoom = OnEnterNewRoom.currentRoom;
        if (currentRoom != null && currentRoom.getSecretType() == Room.SECRET_TYPES.INTERACT) {
            BlockPos interactPos = currentRoom.getSecretLocation();
            if (interactPos != null && pos.equals(interactPos)) {
                System.out.println("[SecretRoutes] Interacted with correct secret at " + pos);
                currentRoom.nextSecret();
            }
        }

        if (block == Blocks.LEVER && SecretUtils.renderLever && SecretUtils.currentLeverPos != null) {
            BlockPos targetLever = RoomRotationUtils.relativeToActual(
                    SecretUtils.currentLeverPos,
                    RoomDirectionUtils.roomDirection(),
                    RoomDirectionUtils.roomCorner()
            );
            if (pos.equals(targetLever)) {
                SecretUtils.resetValues();
            }
        }

        return InteractionResult.PASS;
    }

    private static void handleLockedChestMessage(String message) {
        if (!SecretRoutesModule.INSTANCE.isEnabled()) return;
        if (!LocationUtils.isInDungeons()) return;
        if (message == null) return;

        String clean = message.replaceAll("§[0-9a-fk-or]", "");
        if (!clean.toLowerCase().contains("that chest is locked")) return;

        SecretUtils.renderLever = true;

        Room currentRoom = OnEnterNewRoom.currentRoom;
        if (currentRoom != null && currentRoom.currentSecretRoute != null && currentRoom.currentSecretIndex > 0) {
            try {
                JsonObject previousStep = currentRoom.currentSecretRoute
                        .get(currentRoom.currentSecretIndex - 1)
                        .getAsJsonObject();
                if (previousStep.has("secret")) {
                    JsonObject secret = previousStep.get("secret").getAsJsonObject();
                    if (secret.has("type") && "interact".equalsIgnoreCase(secret.get("type").getAsString()) && SecretUtils.lastInteract != null) {
                        JsonArray loc = secret.get("location").getAsJsonArray();
                        BlockPos previousRelativeSecretPos = new BlockPos(
                                loc.get(0).getAsInt(),
                                loc.get(1).getAsInt(),
                                loc.get(2).getAsInt()
                        );
                        BlockPos relativeLastInteract = RoomRotationUtils.actualToRelative(
                                SecretUtils.lastInteract,
                                RoomDirectionUtils.roomDirection(),
                                RoomDirectionUtils.roomCorner()
                        );
                        if (previousRelativeSecretPos.equals(relativeLastInteract)) {
                            currentRoom.lastSecretKeybind();
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static void onTick(Minecraft client) {
        if (!SecretRoutesModule.INSTANCE.isEnabled()) return;
        LocalPlayer player = client.player;
        if (player == null) return;

        if (!LocationUtils.isInDungeons()) {
            resetState();
            return;
        }

        Room currentRoom = OnEnterNewRoom.currentRoom;
        if (currentRoom != null) {
            Room.SECRET_TYPES secretType = currentRoom.getSecretType();
            BlockPos secretPos = currentRoom.getSecretLocation();
            
            if (secretPos != null) {
                BlockPos pPos = player.blockPosition();
                
                // BAT proximity check (slightly wider so high/offset bats don't get stuck)
                if (secretType == Room.SECRET_TYPES.BAT) {
                    if (Math.abs(pPos.getX() - secretPos.getX()) <= 5 &&
                        Math.abs(pPos.getY() - secretPos.getY()) <= 5 &&
                        Math.abs(pPos.getZ() - secretPos.getZ()) <= 5) {
                        System.out.println("[SecretRoutes] Near bat! Moving to next secret.");
                        currentRoom.nextSecret();
                    }
                }

                // INTERACT proximity fallback: if use-block callback is missed, advance after short delay near target.
                if (secretType == Room.SECRET_TYPES.INTERACT) {
                    boolean nearInteract = Math.abs(pPos.getX() - secretPos.getX()) <= 2 &&
                        Math.abs(pPos.getY() - secretPos.getY()) <= 2 &&
                        Math.abs(pPos.getZ() - secretPos.getZ()) <= 2;

                    if (nearInteract) {
                        long now = System.currentTimeMillis();
                        if (interactProximityTarget == null || !interactProximityTarget.equals(secretPos)) {
                            interactProximityTarget = secretPos;
                            interactProximityStartMillis = now;
                        } else if (now - interactProximityStartMillis >= 1200L) {
                            BlockPos stillTarget = currentRoom.getSecretLocation();
                            if (stillTarget != null && stillTarget.equals(interactProximityTarget) && currentRoom.getSecretType() == Room.SECRET_TYPES.INTERACT) {
                                System.out.println("[SecretRoutes] Interact proximity fallback triggered at " + stillTarget);
                                currentRoom.nextSecret();
                                interactProximityStartMillis = 0L;
                                interactProximityTarget = null;
                            }
                        }
                    } else {
                        interactProximityStartMillis = 0L;
                        interactProximityTarget = null;
                    }
                } else {
                    interactProximityStartMillis = 0L;
                    interactProximityTarget = null;
                }
                
                // ITEM proximity fallback: if pickup event is missed, advance after short delay near target.
                if (secretType == Room.SECRET_TYPES.ITEM) {
                    boolean nearItem = Math.abs(pPos.getX() - secretPos.getX()) <= 2 &&
                        Math.abs(pPos.getY() - secretPos.getY()) <= 2 &&
                        Math.abs(pPos.getZ() - secretPos.getZ()) <= 2;

                    if (nearItem) {
                        long now = System.currentTimeMillis();
                        if (itemProximityTarget == null || !itemProximityTarget.equals(secretPos)) {
                            itemProximityTarget = secretPos;
                            itemProximityStartMillis = now;
                        } else if (now - itemProximityStartMillis >= 1200L) {
                            BlockPos stillTarget = currentRoom.getSecretLocation();
                            if (stillTarget != null && stillTarget.equals(itemProximityTarget) && currentRoom.getSecretType() == Room.SECRET_TYPES.ITEM) {
                                System.out.println("[SecretRoutes] Item proximity fallback triggered at " + stillTarget);
                                currentRoom.nextSecret();
                                itemProximityStartMillis = 0L;
                                itemProximityTarget = null;
                            }
                        }
                    } else {
                        itemProximityStartMillis = 0L;
                        itemProximityTarget = null;
                    }
                } else {
                    itemProximityStartMillis = 0L;
                    itemProximityTarget = null;
                }
            }
        }

        // Inventory check every 5 ticks
        if (++tickCounter % 5 == 0) {
            checkItemPickup(player);
        }
    }

    private static void checkItemPickup(LocalPlayer player) {
        Map<String, Integer> currentInventory = new HashMap<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                String itemName = stack.getHoverName().getString();
                currentInventory.put(itemName, currentInventory.getOrDefault(itemName, 0) + stack.getCount());
            }
        }

        for (Map.Entry<String, Integer> entry : currentInventory.entrySet()) {
            String itemName = entry.getKey();
            int currentCount = entry.getValue();
            int previousCount = previousInventory.getOrDefault(itemName, 0);

            if (currentCount > previousCount && isSecretItem(itemName)) {
                Room currentRoom = OnEnterNewRoom.currentRoom;
                if (currentRoom != null && currentRoom.getSecretType() == Room.SECRET_TYPES.ITEM) {
                    BlockPos itemPos = currentRoom.getSecretLocation();
                    if (itemPos != null) {
                        BlockPos pPos = player.blockPosition();
                        if (pPos.distSqr(itemPos) <= 100) { // dentro de 10 blocks
                            System.out.println("[SecretRoutes] Picked up secret item: " + itemName);
                            currentRoom.nextSecret();
                        }
                    }
                }
            }
        }

        previousInventory.clear();
        previousInventory.putAll(currentInventory);
    }

    private static boolean isSecretItem(String itemName) {
        String normalized = itemName == null ? "" : itemName.replaceAll("§.", "").toLowerCase();
        for (String item : VALID_SECRET_ITEMS) {
            if (normalized.contains(item.toLowerCase())) return true;
        }
        return false;
    }

    public static void resetState() {
        previousInventory.clear();
        tickCounter = 0;
        itemProximityStartMillis = 0L;
        itemProximityTarget = null;
        interactProximityStartMillis = 0L;
        interactProximityTarget = null;
        lastInteractPos = null;
        lastInteractTime = 0L;
    }
}
