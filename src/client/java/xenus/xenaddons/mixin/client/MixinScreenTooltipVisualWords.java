package xenus.xenaddons.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xenus.xenaddons.config.GoatedPlayersManager;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.features.impl.misc.ItemPriceModule;
import xenus.xenaddons.utils.ItemPriceManager;

import java.util.ArrayList;
import java.util.List;

@Mixin(Screen.class)
public class MixinScreenTooltipVisualWords {

    @Inject(method = "getTooltipFromItem", at = @At("RETURN"), cancellable = true)
    private static void xenaddons$replaceItemTooltipText(MinecraftClient client, ItemStack stack, CallbackInfoReturnable<List<Text>> cir) {
        List<Text> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) return;

        List<Text> replaced = new ArrayList<>(original.size());
        for (Text line : original) {
            replaced.add(GoatedPlayersManager.applyVisualWords(line));
        }

        ItemPriceModule itemPrice = ModuleManager.getModule(ItemPriceModule.class);
        if (itemPrice != null && itemPrice.isEnabled()) {
            ItemPriceManager.requestRefreshIfNeeded();

            String itemId = ItemPriceManager.resolvePricingId(stack);
            if (itemId != null) {
                int count = Math.max(1, stack.getCount());
                Double ahPrice = ItemPriceManager.getAhPrice(itemId);
                Double bzPrice = ItemPriceManager.getBzPrice(itemId);

                // Priority: Bazaar first, then Lowest BIN
                if (itemPrice.showBzPrice.getValue() && bzPrice != null) {
                    double total = bzPrice * count;
                    String line = "§b§lBazaar: §6" + ItemPriceManager.formatCoins(total);
                    if (count > 1) line += " §7(" + count + "x)";
                    replaced.add(Text.literal(line));
                } else if (itemPrice.showAhPrice.getValue() && ahPrice != null) {
                    double total = ahPrice * count;
                    String line = "§b§lLowest BIN: §6" + ItemPriceManager.formatCoins(total);
                    if (count > 1) line += " §7(" + count + "x)";
                    replaced.add(Text.literal(line));
                }
            }
        }

        cir.setReturnValue(replaced);
    }
}
