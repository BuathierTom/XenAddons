package xenus.xenaddons.features.impl.misc;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
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

    private static void onTick(MinecraftClient client) {
        if (!ExperimentationTableSolverModule.isActive()) return;
        if (client == null || client.player == null) return;
        if (!(client.currentScreen instanceof HandledScreen<?> screen)) {
            reset();
            return;
        }

        if (!isSuperpairsScreen(screen)) {
            reset();
            return;
        }

        ScreenHandler handler = screen.getScreenHandler();
        if (handler == null) return;
        if (handler.syncId != lastSyncId) {
            reset();
            lastSyncId = handler.syncId;
        }

        superpairsActive = true;
        readSuperpairsSlots(handler);
    }

    public static boolean onHandledScreenMouseClicked(HandledScreen<?> screen, int button) {
        if (!ExperimentationTableSolverModule.isActive()) return false;
        ExperimentationTableSolverModule module = ExperimentationTableSolverModule.get();
        if (module == null || !module.keepItemsVisible.getValue()) return false;
        if (button != 0 && button != 1) return false;
        if (screen == null || !isSuperpairsScreen(screen)) return false;

        Slot slot = XenHandledScreenCompat.getFocusedSlot(screen);
        if (slot != null && slot.hasStack()) {
            handleSuperpairsClick(slot);
        }
        return false;
    }

    public static ItemStack getSuperpairsVisualStack(Slot slot) {
        if (!ExperimentationTableSolverModule.isActive()) return null;
        ExperimentationTableSolverModule module = ExperimentationTableSolverModule.get();
        if (module == null || !module.keepItemsVisible.getValue()) return null;
        if (!superpairsActive) return null;
        if (slot == null || !slot.hasStack()) return null;
        if (!isUnknownSuperpairsItem(slot.getStack())) return null;

        ItemStack replacement = superpairsSlotMap.get(slot.id);
        return replacement == null ? null : replacement.copy();
    }

    public static String getSuperpairsOverlayText(Slot slot) {
        if (!ExperimentationTableSolverModule.isActive()) return null;
        ExperimentationTableSolverModule module = ExperimentationTableSolverModule.get();
        if (module == null || !module.keepItemsVisible.getValue()) return null;
        if (!superpairsActive) return null;
        if (slot == null) return null;

        ItemStack stack = superpairsSlotMap.get(slot.id);
        if (stack == null || stack.isEmpty()) return null;

        String fromName = extractEnchantingXpText(stack.getName().getString());
        if (fromName != null) return fromName;

        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null || lore.lines().isEmpty()) return null;
        for (var line : lore.lines()) {
            String fromLore = extractEnchantingXpText(line.getString());
            if (fromLore != null) return fromLore;
        }
        return null;
    }

    private static void handleSuperpairsClick(Slot slot) {
        int slotId = slot.id;
        if (superpairsSlotMap.containsKey(slotId)) return;

        ItemStack clicked = slot.getStack();
        if (isUnknownSuperpairsItem(clicked)) {
            superpairsSlotsToRead.add(slotId);
        } else {
            superpairsSlotMap.put(slotId, clicked.copy());
            superpairsSlotsToRead.remove(slotId);
        }
    }

    private static void readSuperpairsSlots(ScreenHandler handler) {
        for (Slot slot : handler.slots) {
            if (slot == null || !slot.hasStack()) continue;
            int slotId = slot.id;
            ItemStack stack = slot.getStack();
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
            if (slot == null || !slot.hasStack()) continue;

            ItemStack stack = slot.getStack();
            if (!isUnknownSuperpairsItem(stack) && isSuperpairsRewardLike(stack)) {
                superpairsSlotMap.put(slotId, stack.copy());
                it.remove();
            }
        }
    }

    private static boolean isSuperpairsScreen(HandledScreen<?> screen) {
        String title = stripFormatting(screen.getTitle().getString());
        return title != null && title.toLowerCase(Locale.ROOT).startsWith("superpairs");
    }

    private static boolean isUnknownSuperpairsItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String s = stripFormatting(stack.getName().getString());
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
        String name = stripFormatting(stack.getName().getString());
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

    private static Slot findSlotById(ScreenHandler handler, int slotId) {
        if (handler == null) return null;
        for (Slot slot : handler.slots) {
            if (slot != null && slot.id == slotId) return slot;
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
