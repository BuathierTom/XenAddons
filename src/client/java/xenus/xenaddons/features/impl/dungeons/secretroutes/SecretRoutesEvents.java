package xenus.xenaddons.features.impl.dungeons.secretroutes;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xenus.xenaddons.features.impl.dungeons.SecretRoutesModule;

import java.util.HashMap;
import java.util.Map;

public class SecretRoutesEvents {

    private static final long INTERACT_COOLDOWN = 110;
    private static long lastInteractTime = 0;
    private static BlockPos lastInteractPos = null;

    private static final Map<String, Integer> previousInventory = new HashMap<>();
    private static int tickCounter = 0;

    public static final String[] VALID_SECRET_ITEMS = {
            "Decoy", "Defuse Kit", "Dungeon Chest Key", "Healing VIII",
            "Inflatable Jerry", "Spirit Leap", "Training Weights", "Trap", "Treasure Talisman"
    };

    public static void init() {
        UseBlockCallback.EVENT.register(SecretRoutesEvents::onUseBlock);
    }

    public static ActionResult onUseBlock(net.minecraft.entity.player.PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (!SecretRoutesModule.INSTANCE.isEnabled()) return ActionResult.PASS;
        if (!LocationUtils.isInDungeons()) return ActionResult.PASS;
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;

        long currentTime = System.currentTimeMillis();
        BlockPos pos = hitResult.getBlockPos();

        if (lastInteractPos != null && lastInteractPos.equals(pos) && (currentTime - lastInteractTime) < INTERACT_COOLDOWN) {
            return ActionResult.PASS;
        }

        lastInteractTime = currentTime;
        lastInteractPos = pos;

        Block block = world.getBlockState(pos).getBlock();

        // Check if interactable block
        if (block != Blocks.CHEST && block != Blocks.TRAPPED_CHEST && block != Blocks.LEVER && block != Blocks.PLAYER_HEAD && block != Blocks.SKELETON_SKULL) {
            return ActionResult.PASS;
        }

        Room currentRoom = OnEnterNewRoom.currentRoom;
        if (currentRoom != null && currentRoom.getSecretType() == Room.SECRET_TYPES.INTERACT) {
            BlockPos interactPos = currentRoom.getSecretLocation();
            if (interactPos != null && pos.equals(interactPos)) {
                System.out.println("[SecretRoutes] Interacted with correct secret at " + pos);
                currentRoom.nextSecret();
            }
        }

        return ActionResult.PASS;
    }

    public static void onTick(MinecraftClient client) {
        if (!SecretRoutesModule.INSTANCE.isEnabled()) return;
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        if (!LocationUtils.isInDungeons()) return;

        Room currentRoom = OnEnterNewRoom.currentRoom;
        if (currentRoom != null) {
            Room.SECRET_TYPES secretType = currentRoom.getSecretType();
            BlockPos secretPos = currentRoom.getSecretLocation();
            
            if (secretPos != null) {
                BlockPos pPos = player.getBlockPos();
                
                // BAT proximity check (3 blocks)
                if (secretType == Room.SECRET_TYPES.BAT) {
                    if (Math.abs(pPos.getX() - secretPos.getX()) <= 3 &&
                        Math.abs(pPos.getY() - secretPos.getY()) <= 3 &&
                        Math.abs(pPos.getZ() - secretPos.getZ()) <= 3) {
                        System.out.println("[SecretRoutes] Near bat! Moving to next secret.");
                        currentRoom.nextSecret();
                    }
                }
                
                // ITEM proximity check (2 blocks - original "Auto" feature)
                /* // If we want to implement the auto-bypass if player is near for a while
                if (secretType == Room.SECRET_TYPES.ITEM) {
                    if (Math.abs(pPos.getX() - secretPos.getX()) <= 2 &&
                        Math.abs(pPos.getY() - secretPos.getY()) <= 2 &&
                        Math.abs(pPos.getZ() - secretPos.getZ()) <= 2) {
                        // In original it has a 1.5s delay... maybe just rely on item pickup for now.
                    }
                }
                */
            }
        }

        // Inventory check every 5 ticks
        if (++tickCounter % 5 == 0) {
            checkItemPickup(player);
        }
    }

    private static void checkItemPickup(ClientPlayerEntity player) {
        Map<String, Integer> currentInventory = new HashMap<>();
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                String itemName = stack.getName().getString();
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
                        BlockPos pPos = player.getBlockPos();
                        if (pPos.getSquaredDistance(itemPos) <= 100) { // dentro de 10 blocks
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
        for (String item : VALID_SECRET_ITEMS) {
            if (itemName.contains(item)) return true;
        }
        return false;
    }
}
