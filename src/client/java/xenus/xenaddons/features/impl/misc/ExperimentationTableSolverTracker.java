package xenus.xenaddons.features.impl.misc;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import xenus.xenaddons.compat.XenHandledScreenCompat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExperimentationTableSolverTracker {
    private static final Pattern ENCHANTING_XP_PATTERN = Pattern.compile("(?i)([\\d.]+k)\\s+enchanting\\s+exp");

    private static final Map<Integer, ItemStack> superpairsSlotMap = new HashMap<>();
    private static final Set<Integer> superpairsSlotsToRead = new HashSet<>();

    private static boolean initialized = false;
    private static boolean superpairsActive = false;
    private static int lastSyncId = Integer.MIN_VALUE;

    private ExperimentationTableSolverTracker() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        ClientTickEvents.END_CLIENT_TICK.register(ExperimentationTableSolverTracker::onTick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    private static void onTick(Minecraft client) {
        if (!ExperimentationTableSolverModule.isActive()) return;
        if (client == null || client.player == null) return;
        if (!(client.screen instanceof AbstractContainerScreen<?> screen)) {
            reset();
            return;
        }

        if (!isSuperpairsScreen(screen)) {
            reset();
            return;
        }

        AbstractContainerMenu handler = screen.getMenu();
        if (handler == null) return;
        if (handler.containerId != lastSyncId) {
            reset();
            lastSyncId = handler.containerId;
        }

        superpairsActive = true;
        readSuperpairsSlots(handler);
    }

    public static boolean onHandledScreenMouseClicked(AbstractContainerScreen<?> screen, int button) {
        if (!ExperimentationTableSolverModule.isActive()) return false;
        ExperimentationTableSolverModule module = ExperimentationTableSolverModule.get();
        if (module == null || !module.keepItemsVisible.getValue()) return false;
        if (button != 0 && button != 1) return false;
        if (screen == null || !isSuperpairsScreen(screen)) return false;

        Slot slot = XenHandledScreenCompat.getFocusedSlot(screen);
        if (slot != null && slot.hasItem()) {
            handleSuperpairsClick(slot);
        }
        return false;
    }

    public static ItemStack getSuperpairsVisualStack(Slot slot) {
        if (!ExperimentationTableSolverModule.isActive()) return null;
        ExperimentationTableSolverModule module = ExperimentationTableSolverModule.get();
        if (module == null || !module.keepItemsVisible.getValue()) return null;
        if (!superpairsActive) return null;
        if (slot == null || !slot.hasItem()) return null;
        if (!isUnknownSuperpairsItem(slot.getItem())) return null;

        ItemStack replacement = superpairsSlotMap.get(slot.index);
        return replacement == null ? null : replacement.copy();
    }

    public static String getSuperpairsOverlayText(Slot slot) {
        if (!ExperimentationTableSolverModule.isActive()) return null;
        ExperimentationTableSolverModule module = ExperimentationTableSolverModule.get();
        if (module == null || !module.keepItemsVisible.getValue()) return null;
        if (!superpairsActive) return null;
        if (slot == null) return null;

        ItemStack stack = superpairsSlotMap.get(slot.index);
        if (stack == null || stack.isEmpty()) return null;

        String fromName = extractEnchantingXpText(stack.getHoverName().getString());
        if (fromName != null) return fromName;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null || lore.lines().isEmpty()) return null;
        for (var line : lore.lines()) {
            String fromLore = extractEnchantingXpText(line.getString());
            if (fromLore != null) return fromLore;
        }
        return null;
    }

    private static void handleSuperpairsClick(Slot slot) {
        int slotId = slot.index;
        if (superpairsSlotMap.containsKey(slotId)) return;

        ItemStack clicked = slot.getItem();
        if (isUnknownSuperpairsItem(clicked)) {
            superpairsSlotsToRead.add(slotId);
        } else {
            superpairsSlotMap.put(slotId, clicked.copy());
            superpairsSlotsToRead.remove(slotId);
        }
    }

    private static void readSuperpairsSlots(AbstractContainerMenu handler) {
        for (Slot slot : handler.slots) {
            if (slot == null || !slot.hasItem()) continue;
            int slotId = slot.index;
            ItemStack stack = slot.getItem();
            if (!isUnknownSuperpairsItem(stack) && isSuperpairsRewardLike(stack)) {
                superpairsSlotMap.put(slotId, stack.copy());
                superpairsSlotsToRead.remove(slotId);
            }
        }

        if (superpairsSlotsToRead.isEmpty()) return;
        Iterator<Integer> it = superpairsSlotsToRead.iterator();
        while (it.hasNext()) {
            int slotId = it.next();
            Slot slot = findSlotById(handler, slotId);
            if (slot == null || !slot.hasItem()) continue;

            ItemStack stack = slot.getItem();
            if (!isUnknownSuperpairsItem(stack) && isSuperpairsRewardLike(stack)) {
                superpairsSlotMap.put(slotId, stack.copy());
                it.remove();
            }
        }
    }

    private static boolean isSuperpairsScreen(AbstractContainerScreen<?> screen) {
        String title = stripFormatting(screen.getTitle().getString());
        return title != null && title.toLowerCase(Locale.ROOT).startsWith("superpairs");
    }

    private static boolean isUnknownSuperpairsItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String s = stripFormatting(stack.getHoverName().getString());
        if (s == null) return false;
        String low = s.trim().toLowerCase(Locale.ROOT);
        if (low.isEmpty()) return false;
        if (low.equals("?")) return true;
        if (low.endsWith("?")) return true;
        if (low.startsWith("click any button")) return true;
        if (low.startsWith("click a second button")) return true;
        if (low.startsWith("click a sec")) return true;
        return low.startsWith("next button");
    }

    private static boolean isSuperpairsRewardLike(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String name = stripFormatting(stack.getHoverName().getString());
        if (name == null || name.isBlank()) return false;

        String low = name.toLowerCase(Locale.ROOT);
        if (low.contains("remaining clicks")) return false;
        if (low.contains("remember the pattern")) return false;
        if (low.contains("timer:")) return false;
        if (low.contains("round:")) return false;
        if (low.contains("click any button")) return false;
        if (low.contains("click a second button")) return false;
        if (low.contains("next button")) return false;
        if (extractEnchantingXpText(name) != null) return true;

        return low.contains("enchanted book")
                || low.contains("experience bottle")
                || low.contains("guardian")
                || low.contains("instant find")
                || low.contains("gained +")
                || low.contains(" xp");
    }

    private static String extractEnchantingXpText(String raw) {
        String name = stripFormatting(raw);
        if (name == null) return null;
        Matcher matcher = ENCHANTING_XP_PATTERN.matcher(name);
        if (!matcher.find()) return null;
        return matcher.group(1);
    }

    private static Slot findSlotById(AbstractContainerMenu handler, int slotId) {
        if (handler == null) return null;
        for (Slot slot : handler.slots) {
            if (slot != null && slot.index == slotId) return slot;
        }
        return null;
    }

    private static String stripFormatting(String input) {
        if (input == null) return null;
        return input.replaceAll("(?i)[§&][0-9A-FK-OR]", "");
    }

    private static void reset() {
        superpairsActive = false;
        lastSyncId = Integer.MIN_VALUE;
        superpairsSlotMap.clear();
        superpairsSlotsToRead.clear();
    }
}
