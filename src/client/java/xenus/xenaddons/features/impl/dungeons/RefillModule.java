package xenus.xenaddons.features.impl.dungeons;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.utils.ChatUtils;
import xenus.xenaddons.utils.ItemPriceManager;

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
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> createCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> command = ClientCommands.literal("refill")
                .executes(context -> {
                    ChatUtils.error("Usage: /xa refill <pearl|tnt|leap|twap|tap|jerry|decoy>");
                    return 0;
                });

        for (RefillTarget target : RefillTarget.values()) {
            command.then(ClientCommands.literal(target.argument)
                    .executes(context -> refill(target)));
        }

        return command;
    }

    private static int refill(RefillTarget target) {
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

        int currentAmount = countTarget(player, target);
        int missingAmount = Math.max(0, target.targetAmount - currentAmount);
        if (missingAmount == 0) {
            ChatUtils.info(target.skyBlockId + " is already refilled (" + currentAmount + "/"
                    + target.targetAmount + ").");
            return 1;
        }

        player.connection.sendCommand("gfs " + target.skyBlockId + " " + missingAmount);
        ChatUtils.success("Refilling " + target.skyBlockId + ": " + currentAmount + "/"
                + target.targetAmount + " (requesting " + missingAmount + ").");
        return 1;
    }

    private static int countTarget(LocalPlayer player, RefillTarget target) {
        int amount = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (matches(stack, target)) {
                amount += stack.getCount();
            }
        }
        return amount;
    }

    private static boolean matches(ItemStack stack, RefillTarget target) {
        if (stack == null || stack.isEmpty()) return false;

        String skyBlockId = ItemPriceManager.extractSkyBlockId(stack);
        if (skyBlockId != null) {
            return target.skyBlockId.equals(skyBlockId.toUpperCase(Locale.ROOT));
        }

        return target.vanillaFallback != null && stack.is(target.vanillaFallback);
    }
}
