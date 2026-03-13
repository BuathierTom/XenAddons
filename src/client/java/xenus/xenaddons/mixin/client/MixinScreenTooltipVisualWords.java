package xenus.xenaddons.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xenus.xenaddons.config.GoatedPlayersManager;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.features.impl.dungeons.ItemQualityTooltip;
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
            replaced.add(GoatedPlayersManager.applyVisualWordsForTooltip(line));
        }

        ItemPriceModule itemPrice = ModuleManager.getModule(ItemPriceModule.class);
        if (itemPrice != null && itemPrice.isEnabled()) {
            ItemPriceManager.requestRefreshIfNeeded();

            String itemId = ItemPriceManager.resolvePricingId(stack);
            if (itemId != null) {
                int count = Math.max(1, stack.getCount());
                ItemPriceManager.BazaarPrice bazaar = itemPrice.showBzPrice.getValue() ? ItemPriceManager.getBazaarPrice(itemId) : null;

                // Bazaar has priority; if missing, fallback to Lowest BIN.
                if (bazaar != null) {
                    int stackAmountForShift = count > 1 ? count : 64;
                    boolean showStack = true;
                    long handle = client.getWindow().getHandle();
                    boolean shiftDown = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                        || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
                    int multiplier = (showStack && shiftDown) ? stackAmountForShift : 1;

                    addPriceSectionSpacer(replaced);
                    if (showStack && !shiftDown) {
                        replaced.add(Text.literal("§7[SHIFT show x" + stackAmountForShift + "]"));
                    }

                    addBazaarLine(replaced, "Bazaar Buy", bazaar.buy(), multiplier);
                    addBazaarLine(replaced, "Bazaar Sell", bazaar.sell(), multiplier);
                    addBazaarLine(replaced, "Bazaar Insta-Buy", bazaar.instaBuy(), multiplier);
                    addBazaarLine(replaced, "Bazaar Insta-Sell", bazaar.instaSell(), multiplier);
                } else if (itemPrice.showAhPrice.getValue()) {
                    Double lowestBin = ItemPriceManager.getLowestBin(itemId);
                    if (lowestBin != null) {
                        addPriceSectionSpacer(replaced);
                        replaced.add(Text.literal("§b§lLowest BIN: §6" + ItemPriceManager.formatCoins(lowestBin)));
                    }
                }
            }
        }

        ItemQualityTooltip.appendQualityBonus(replaced, stack);

        cir.setReturnValue(replaced);
    }

    private static void addBazaarLine(List<Text> tooltip, String label, Double price, int multiplier) {
        if (price == null) return;
        double shown = price * Math.max(1, multiplier);
        tooltip.add(Text.literal("§b§l" + label + ": §6" + ItemPriceManager.formatCoins(shown)));
    }

    private static void addPriceSectionSpacer(List<Text> tooltip) {
        if (tooltip.isEmpty()) return;
        Text last = tooltip.get(tooltip.size() - 1);
        if (last.getString().isEmpty()) return;
        tooltip.add(Text.literal(""));
    }
}
