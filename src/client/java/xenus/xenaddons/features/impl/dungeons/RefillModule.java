package xenus.xenaddons.features.impl.dungeons;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.utils.ChatUtils;

import java.util.Locale;

public class RefillModule extends Module {
    private enum RefillTarget {
        PEARL("pearl", "ENDER_PEARL", 16, Items.ENDER_PEARL),
        TNT("tnt", "SUPERBOOM_TNT", 64, Items.TNT),
        LEAP("leap", "SPIRIT_LEAP", 16, null),
        TWAP("twap", "TWILIGHT_ARROW_POISON", 8, null),
        TAP("tap", "TOXIC_ARROW_POISON", 8, null),
        JERRY("jerry", "INFLATABLE_JERRY", 64, null),
        DECOY("decoy", "DUNGEON_DECOY", 64, null);

        private final String argument;
        private final String skyBlockId;
        private final int targetAmount;
        private final Item vanillaFallback;

        RefillTarget(String argument, String skyBlockId, int targetAmount, Item vanillaFallback) {
            this.argument = argument;
            this.skyBlockId = skyBlockId;
            this.targetAmount = targetAmount;
            this.vanillaFallback = vanillaFallback;
        }
    }

    public RefillModule() {
        super("Refill", "Refills dungeon utility items with /xa refill", Category.DUNGEON);
        this.setEnabled(true);
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> createCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> command = ClientCommands.literal("refill")
                .executes(context -> {
                    ChatUtils.error("Usage: /xa refill <pearl|tnt|leap|twap|tap|jerry|decoy> [amount]");
                    return 0;
                });

        for (RefillTarget target : RefillTarget.values()) {
            command.then(ClientCommands.literal(target.argument)
                    .executes(context -> refill(target, target.targetAmount))
                    .then(ClientCommands.argument("amount", IntegerArgumentType.integer(1))
                            .executes(context -> refill(target,
                                    IntegerArgumentType.getInteger(context, "amount")))));
        }

        return command;
    }

    private static int refill(RefillTarget target, int targetAmount) {
        RefillModule module = ModuleManager.getModule(RefillModule.class);
        if (module == null || !module.isEnabled()) {
            ChatUtils.error("The Refill feature must be enabled in the Dungeon category.");
            return 0;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.connection == null) {
            ChatUtils.error("You must be connected to a server to use Refill.");
            return 0;
        }

        int currentAmount = countTarget(player, target, targetAmount);
        int missingAmount = Math.max(0, targetAmount - currentAmount);
        if (missingAmount == 0) {
            ChatUtils.info("Already refilled at max.");
            return 1;
        }

        player.connection.sendCommand("gfs " + target.skyBlockId + " " + missingAmount);
        return 1;
    }

    private static int countTarget(LocalPlayer player, RefillTarget target, int targetAmount) {
        int amount = 0;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (matches(stack, target)) {
                amount += stack.getCount();
                if (amount >= targetAmount) {
                    return targetAmount;
                }
            }
        }
        return amount;
    }

    private static boolean matches(ItemStack stack, RefillTarget target) {
        if (stack == null || stack.isEmpty()) return false;

        String skyBlockId = getSkyBlockId(stack);
        if (skyBlockId != null) {
            return target.skyBlockId.equals(skyBlockId.toUpperCase(Locale.ROOT));
        }

        return target.vanillaFallback != null && stack.is(target.vanillaFallback);
    }

    private static String getSkyBlockId(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;

        CompoundTag root = customData.copyTag();
        String directId = root.getStringOr("id", "");
        if (!directId.isBlank()) return directId;

        CompoundTag extra = root.getCompound("ExtraAttributes")
                .orElse(root.getCompound("extra_attributes").orElse(null));
        if (extra == null) return null;

        String extraId = extra.getStringOr("id", "");
        return extraId.isBlank() ? null : extraId;
    }
}
