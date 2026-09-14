package xenus.xenaddons.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xenus.xenaddons.compat.XenTextCompat;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.features.impl.dungeons.ItemQualityTooltip;
import xenus.xenaddons.features.impl.dungeons.LoadoutTitleModule;
import xenus.xenaddons.features.impl.misc.HidePartsOnRodModule;
import xenus.xenaddons.features.impl.misc.ItemPriceModule;
import xenus.xenaddons.features.impl.misc.VisitorGardenModule;
import xenus.xenaddons.utils.ItemPriceManager;

import java.util.List;

@Mixin(Screen.class)
public class MixinScreenTooltipVisualWords {

    @Inject(method = "getTooltipFromItem", at = @At("RETURN"), cancellable = true)
    private static void xenaddons$replaceItemTooltipText(Minecraft client, ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        List<Component> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) return;
        if (isPartyFinderScreen(client)) return;

        List<Component> replaced = XenTextCompat.replaceTooltipList(original);
        HidePartsOnRodModule.hideRodParts(replaced, stack);
        VisitorGardenModule.appendHint(replaced, stack);

        ItemPriceModule itemPrice = ModuleManager.getModule(ItemPriceModule.class);
        if (itemPrice != null && itemPrice.isEnabled() && !LoadoutTitleModule.isInLoadoutsMenu()) {
            ItemPriceManager.requestRefreshIfNeeded();

            String itemId = ItemPriceManager.resolvePricingId(stack);
            if (itemId != null) {
                int count = Math.max(1, stack.getCount());
                ItemPriceManager.BazaarPrice bazaar = itemPrice.showBzPrice.getValue() ? ItemPriceManager.getBazaarPrice(itemId) : null;

                if (bazaar != null) {
                    int stackAmountForShift = ItemPriceModule.resolveShiftAmount(replaced, count);
                    // A sack holding a single item would only offer a pointless "x1" line.
                    boolean showStack = !itemId.startsWith("ENCHANTMENT_") && stackAmountForShift > 1;
                    long handle = client.getWindow().handle();
                    boolean shiftDown = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                        || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
                    int multiplier = (showStack && shiftDown) ? stackAmountForShift : 1;

                    addPriceSectionSpacer(replaced);
                    if (showStack && !shiftDown) {
                        replaced.add(Component.literal("§7[SHIFT show x" + stackAmountForShift + "]"));
                    }

                    addBazaarLine(replaced, "Bazaar Buy", bazaar.buy(), multiplier);
                    addBazaarLine(replaced, "Bazaar Sell", bazaar.sell(), multiplier);
                    addBazaarLine(replaced, "Bazaar Insta-Buy", bazaar.instaBuy(), multiplier);
                    addBazaarLine(replaced, "Bazaar Insta-Sell", bazaar.instaSell(), multiplier);
                } else if (itemPrice.showAhPrice.getValue()) {
                    Double lowestBin = ItemPriceManager.getLowestBin(itemId);
                    if (lowestBin != null) {
                        addPriceSectionSpacer(replaced);
                        replaced.add(Component.literal("§b§lLowest BIN: §6" + ItemPriceManager.formatCoins(lowestBin)));
                    }
                }
            }
        }

        ItemQualityTooltip.appendQualityBonus(replaced, stack);

        cir.setReturnValue(replaced);
    }

    private static boolean isPartyFinderScreen(Minecraft client) {
        if (client == null || client.screen == null || client.screen.getTitle() == null) return false;
        return "Party Finder".equals(client.screen.getTitle().getString());
    }

    private static void addBazaarLine(List<Component> tooltip, String label, Double price, int multiplier) {
        if (price == null) return;
        double shown = price * Math.max(1, multiplier);
        tooltip.add(Component.literal("§b§l" + label + ": §6" + ItemPriceManager.formatCoins(shown)));
    }

    private static void addPriceSectionSpacer(List<Component> tooltip) {
        if (tooltip.isEmpty()) return;
        Component last = tooltip.get(tooltip.size() - 1);
        if (last.getString().isEmpty()) return;
        tooltip.add(Component.literal(""));
    }
}
