package xenus.xenaddons.features.impl.misc;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import xenus.xenaddons.mixin.client.HandledScreenAccessor;
import xenus.xenaddons.utils.ChatUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExperimentationTableSolverTracker {
    private enum TaskType {NONE, CHRONOMATRON, ULTRASEQUENCER, SUPERPAIRS}
    private enum HelperPhase {READ, REPLICATE}
    private enum ChronoColor {GREEN, LIME, PINK, CYAN, ORANGE, PURPLE}

    private static final int ROUND_STATUS_SLOT = 4;
    private static final int PHASE_STATUS_SLOT = 49;
    private static final Pattern ROUND_PATTERN = Pattern.compile("(?i)round:\\s*(\\d+)");
    private static final Pattern ENCHANTING_XP_PATTERN = Pattern.compile("(?i)([\\d.]+k)\\s+enchanting\\s+exp");
    private static final Pattern ROUNDS_NEEDED_PATTERN = Pattern.compile("(?i)(?:Chain|Series) of (\\d+):");

    private static final List<ChronoColor> hypixelChronomatronData = new ArrayList<>();
    private static final List<ChronoColor> userChronomatronProgress = new ArrayList<>();
    private static final List<Integer> hypixelUltrasequencerData = new ArrayList<>();
    private static final List<Integer> userUltrasequencerProgress = new ArrayList<>();
    private static final Map<Integer, ItemStack> ultrasequencerDyeMap = new HashMap<>();
    private static final Map<Integer, ItemStack> superpairsSlotMap = new HashMap<>();
    private static final Set<Integer> superpairsSlotsToRead = new HashSet<>();

    private static boolean initialized = false;
    private static boolean chronHasBeenEmpty = true;
    private static int currentChronomatronRound = 0;
    private static int currentUltraSequencerRound = 0;
    private static int chronomatronSequenceIndex = 0;
    private static HelperPhase currentAddonPhase = null;
    private static TaskType currentTask = TaskType.NONE;
    private static int lastSyncId = Integer.MIN_VALUE;
    private static int roundsNeeded = -1;
    private static boolean maxClicksAlertSent = false;

    private ExperimentationTableSolverTracker() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        ClientTickEvents.END_CLIENT_TICK.register(ExperimentationTableSolverTracker::onTick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> resetAll());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> resetAll());
    }

    private static void onTick(MinecraftClient client) {
        if (!ExperimentationTableSolverModule.isActive()) return;
        if (client == null || client.player == null) return;
        if (!(client.currentScreen instanceof HandledScreen<?> screen)) {
            reset();
            return;
        }

        String screenTitle = stripFormatting(screen.getTitle().getString());
        if (screenTitle != null && screenTitle.trim().toLowerCase(Locale.ROOT).endsWith("stakes")) {
            readRoundsNeededFromStakes(screen.getScreenHandler());
            return;
        }

        TaskType taskType = detectTaskType(screen);
        if (taskType == TaskType.NONE) {
            reset();
            return;
        }

        ScreenHandler handler = screen.getScreenHandler();
        if (handler == null) return;
        if (taskType != currentTask || handler.syncId != lastSyncId) {
            resetTaskData();
            currentTask = taskType;
            lastSyncId = handler.syncId;
        }

        if (currentTask == TaskType.SUPERPAIRS) {
            readSuperpairsSlots(handler);
            return;
        }

        HelperPhase oldPhase = currentAddonPhase;
        currentAddonPhase = readPhaseOrNull(handler);
        if (currentAddonPhase == null) return;

        if (currentTask == TaskType.CHRONOMATRON) readNextChronomatron(handler, oldPhase);
        if (currentTask == TaskType.ULTRASEQUENCER) readUltrasequencer(handler);
        checkAndSendMaxClicksAlert();
    }

    public static void renderSlotHighlight(DrawContext context, Slot slot) {
        if (!ExperimentationTableSolverModule.isActive()) return;
        ExperimentationTableSolverModule module = ExperimentationTableSolverModule.get();
        if (module == null || !module.highlightNextClick.getValue()) return;
        if (slot == null || currentAddonPhase != HelperPhase.REPLICATE) return;

        int argb = 0;
        if (currentTask == TaskType.CHRONOMATRON && currentChronomatronRound >= 1) {
            argb = getChronomatronHighlightArgb(slot);
        } else if (currentTask == TaskType.ULTRASEQUENCER && currentUltraSequencerRound >= 1) {
            argb = getUltrasequencerHighlightArgb(slot);
        }

        if (argb != 0) {
            context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, argb);
        }
    }

    public static boolean onHandledScreenMouseClicked(HandledScreen<?> screen, int button) {
        if (!ExperimentationTableSolverModule.isActive()) return false;
        ExperimentationTableSolverModule module = ExperimentationTableSolverModule.get();
        if (module == null) return false;
        if (button != 0 && button != 1) return false;
        if (screen == null) return false;

        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        Slot slot = accessor.getFocusedSlot();
        if (slot == null || !slot.hasStack()) return false;

        if (currentTask == TaskType.SUPERPAIRS && module.keepItemsVisible.getValue()) {
            handleSuperpairsClick(slot);
            return false;
        }

        if (!module.preventMisclicks.getValue()) return false;
        if (currentAddonPhase != HelperPhase.REPLICATE) return false;

        if (currentTask == TaskType.CHRONOMATRON) {
            return handleChronomatronClick(slot);
        }
        if (currentTask == TaskType.ULTRASEQUENCER) {
            return handleUltrasequencerClick(slot);
        }
        return false;
    }

    public static ItemStack getSuperpairsVisualStack(Slot slot) {
        if (!ExperimentationTableSolverModule.isActive()) return null;
        ExperimentationTableSolverModule module = ExperimentationTableSolverModule.get();
        if (module == null || !module.keepItemsVisible.getValue()) return null;
        if (currentTask != TaskType.SUPERPAIRS) return null;
        if (slot == null || !slot.hasStack()) return null;
        if (!isUnknownSuperpairsItem(slot.getStack())) return null;
        ItemStack replacement = superpairsSlotMap.get(slot.id);
        return replacement == null ? null : replacement.copy();
    }

    public static String getSuperpairsOverlayText(Slot slot) {
        if (!ExperimentationTableSolverModule.isActive()) return null;
        ExperimentationTableSolverModule module = ExperimentationTableSolverModule.get();
        if (module == null || !module.keepItemsVisible.getValue()) return null;
        if (currentTask != TaskType.SUPERPAIRS) return null;
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

    private static boolean handleChronomatronClick(Slot slot) {
        if (userChronomatronProgress.size() == hypixelChronomatronData.size()) return false;
        ChronoColor expected = hypixelChronomatronData.get(userChronomatronProgress.size());
        ChronoColor clicked = getChronoColorOrNull(slot.getStack());
        if (clicked == expected) {
            userChronomatronProgress.add(clicked);
            return false;
        }
        return true;
    }

    private static boolean handleUltrasequencerClick(Slot slot) {
        if (userUltrasequencerProgress.size() == hypixelUltrasequencerData.size()) return false;
        int expectedSlot = hypixelUltrasequencerData.get(userUltrasequencerProgress.size());
        if (slot.id == expectedSlot) {
            userUltrasequencerProgress.add(slot.id);
            return false;
        }
        return true;
    }

    private static int getChronomatronHighlightArgb(Slot slot) {
        ChronoColor nextColor = getOrNull(hypixelChronomatronData, userChronomatronProgress.size());
        ChronoColor nextNextColor = getOrNull(hypixelChronomatronData, userChronomatronProgress.size() + 1);
        ChronoColor thirdColor = getOrNull(hypixelChronomatronData, userChronomatronProgress.size() + 2);
        ChronoColor slotColor = getChronoColorOrNull(slot.getStack());
        if (slotColor == null) return 0;
        if (slotColor == nextColor) return 0xA000FF00;
        if (slotColor == nextNextColor) return 0xA0FFAA00;
        if (slotColor == thirdColor) return 0xA0FF5555;
        return 0;
    }

    private static int getUltrasequencerHighlightArgb(Slot slot) {
        int progress = userUltrasequencerProgress.size();
        int order = -1;
        for (int i = 0; i < hypixelUltrasequencerData.size(); i++) {
            if (hypixelUltrasequencerData.get(i) == slot.id) {
                order = i;
                break;
            }
        }
        if (order < progress || order < 0) return 0;
        if (order == progress) return 0xA000FF00;
        if (order == progress + 1) return 0xA0FFAA00;
        if (order == progress + 2) return 0xA0FF5555;
        int distance = order - progress;
        int alpha = Math.max(0x18, 0x70 / distance);
        return (alpha << 24) | 0xFF5555;
    }

    private static void readNextChronomatron(ScreenHandler handler, HelperPhase oldPhase) {
        Integer readRound = readChronomatronRoundOrNull(handler);
        if (readRound == null) return;
        currentChronomatronRound = readRound;

        int hypixelSizeNow = hypixelChronomatronData.size();
        int userSizeNow = userChronomatronProgress.size();

        List<ChronoColor> activeColors = new ArrayList<>();
        for (Slot slot : handler.slots) {
            if (slot == null || !slot.hasStack()) continue;
            if (!isChronomatronReadItem(slot.getStack())) continue;
            ChronoColor c = getChronoColorOrNull(slot.getStack());
            if (c != null && !activeColors.contains(c)) activeColors.add(c);
        }

        if (activeColors.isEmpty()) {
            chronHasBeenEmpty = true;
            return;
        }
        if (!chronHasBeenEmpty) return;
        chronHasBeenEmpty = false;

        ChronoColor clickedColor = null;
        for (ChronoColor itemColor : activeColors) {
            ChronoColor expected = getOrNull(hypixelChronomatronData, chronomatronSequenceIndex);
            if (expected == null || itemColor == expected) {
                clickedColor = itemColor;
                break;
            }
        }
        if (clickedColor == null) return;

        boolean shouldReadLastReplicate = oldPhase == HelperPhase.READ || hypixelSizeNow < currentChronomatronRound;
        boolean isReadingReady = oldPhase == null || oldPhase == HelperPhase.READ;
        boolean shouldNotReadYet = switch (currentAddonPhase) {
            case REPLICATE -> !shouldReadLastReplicate;
            case READ -> !isReadingReady;
        };
        if (shouldNotReadYet) return;

        if (chronomatronSequenceIndex == hypixelSizeNow) {
            hypixelChronomatronData.add(clickedColor);
            chronomatronSequenceIndex = 0;
            userChronomatronProgress.clear();
        } else {
            chronomatronSequenceIndex++;
        }
    }

    private static Integer readChronomatronRoundOrNull(ScreenHandler handler) {
        if (handler.slots.size() <= ROUND_STATUS_SLOT) return null;
        Slot slot = handler.slots.get(ROUND_STATUS_SLOT);
        if (slot == null || !slot.hasStack()) return null;
        String name = stripFormatting(slot.getStack().getName().getString());
        Matcher m = ROUND_PATTERN.matcher(name);
        if (!m.find()) return null;
        try {
            return Integer.parseInt(m.group(1));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void readUltrasequencer(ScreenHandler handler) {
        List<UltraSequencerSlot> ordered = new ArrayList<>();
        for (Slot slot : handler.slots) {
            if (slot == null || !slot.hasStack()) continue;
            String strippedName = stripFormatting(slot.getStack().getName().getString()).trim();
            if (strippedName.isEmpty()) continue;
            Integer sequence = parseIntOrNull(strippedName);
            if (sequence == null) continue;
            currentUltraSequencerRound = Math.max(currentUltraSequencerRound, sequence);
            ultrasequencerDyeMap.putIfAbsent(sequence, slot.getStack().copy());
            ordered.add(new UltraSequencerSlot(sequence, slot.id));
        }
        ordered.sort(Comparator.comparingInt(a -> a.sequenceNumber));

        boolean isOld = currentUltraSequencerRound != ordered.size();
        boolean alreadyKnown = hypixelUltrasequencerData.size() == ordered.size();
        if (isOld || alreadyKnown) return;

        hypixelUltrasequencerData.clear();
        userUltrasequencerProgress.clear();
        for (UltraSequencerSlot entry : ordered) {
            hypixelUltrasequencerData.add(entry.slotId);
        }
    }

    private static void handleSuperpairsClick(Slot slot) {
        if (slot == null || !slot.hasStack()) return;
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

    private static HelperPhase readPhaseOrNull(ScreenHandler handler) {
        if (handler == null || handler.slots.size() <= PHASE_STATUS_SLOT) return null;
        Slot slot = handler.slots.get(PHASE_STATUS_SLOT);
        if (slot == null || !slot.hasStack()) return null;
        String phaseText = stripFormatting(slot.getStack().getName().getString());
        if (phaseText == null) return null;
        String low = phaseText.toLowerCase(Locale.ROOT);
        if (low.contains("timer:")) return HelperPhase.REPLICATE;
        if (low.contains("remember the pattern")) return HelperPhase.READ;
        return null;
    }

    private static void readRoundsNeededFromStakes(ScreenHandler handler) {
        if (handler == null) return;
        int newRounds = -1;
        for (int slotId = 24; slotId >= 20; slotId--) {
            Slot slot = findSlotById(handler, slotId);
            if (slot == null || !slot.hasStack()) continue;
            List<String> lore = getLoreLines(slot.getStack());
            if (lore.isEmpty()) continue;

            boolean practice = lore.stream().anyMatch(line -> line.contains("Practice mode has no rewards"));
            if (practice) {
                newRounds = -1;
                break;
            }
            boolean blocked = lore.stream().anyMatch(line ->
                    line.contains("Enchanting level too low!") || line.contains("Not enough experience!"));
            if (blocked) continue;

            for (int i = lore.size() - 1; i >= 0; i--) {
                String line = stripFormatting(lore.get(i));
                if (line == null) continue;
                Matcher m = ROUNDS_NEEDED_PATTERN.matcher(line);
                if (m.find()) {
                    try {
                        newRounds = Integer.parseInt(m.group(1));
                    } catch (Exception ignored) {
                    }
                    break;
                }
            }
            if (newRounds != -1) break;
        }
        roundsNeeded = newRounds;
        maxClicksAlertSent = false;
    }

    private static void checkAndSendMaxClicksAlert() {
        if (roundsNeeded < 0 || maxClicksAlertSent) return;
        boolean done = false;
        if (currentTask == TaskType.CHRONOMATRON) {
            done = currentChronomatronRound > roundsNeeded;
        } else if (currentTask == TaskType.ULTRASEQUENCER) {
            done = currentUltraSequencerRound > (roundsNeeded - 1);
        }
        if (!done) return;
        ChatUtils.info("You have reached the maximum extra Superpairs clicks from this add-on!");
        maxClicksAlertSent = true;
        roundsNeeded = -1;
    }

    private static TaskType detectTaskType(HandledScreen<?> screen) {
        String title = stripFormatting(screen.getTitle().getString());
        if (title == null) return TaskType.NONE;
        String low = title.toLowerCase(Locale.ROOT);
        if (low.startsWith("chronomatron")) return TaskType.CHRONOMATRON;
        if (low.startsWith("ultrasequencer")) return TaskType.ULTRASEQUENCER;
        if (low.startsWith("superpairs")) return TaskType.SUPERPAIRS;
        return TaskType.NONE;
    }

    private static boolean isChronomatronReadItem(ItemStack stack) {
        String id = String.valueOf(stack.getItem());
        return id.contains("terracotta");
    }

    private static ChronoColor getChronoColorOrNull(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String name = stripFormatting(stack.getName().getString());
        if (name == null) return null;
        return switch (name.trim().toLowerCase(Locale.ROOT)) {
            case "green" -> ChronoColor.GREEN;
            case "lime" -> ChronoColor.LIME;
            case "pink" -> ChronoColor.PINK;
            case "cyan" -> ChronoColor.CYAN;
            case "orange" -> ChronoColor.ORANGE;
            case "purple" -> ChronoColor.PURPLE;
            default -> null;
        };
    }

    private static Integer parseIntOrNull(String text) {
        try {
            return Integer.parseInt(text);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String stripFormatting(String input) {
        if (input == null) return null;
        return input.replaceAll("(?i)[§&][0-9A-FK-OR]", "");
    }

    private static List<String> getLoreLines(ItemStack stack) {
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null || lore.lines().isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        for (var line : lore.lines()) {
            out.add(stripFormatting(line.getString()));
        }
        return out;
    }

    private static void reset() {
        currentTask = TaskType.NONE;
        lastSyncId = Integer.MIN_VALUE;
        resetTaskData();
    }

    private static void resetAll() {
        reset();
        roundsNeeded = -1;
        maxClicksAlertSent = false;
    }

    private static void resetTaskData() {
        hypixelChronomatronData.clear();
        userChronomatronProgress.clear();
        hypixelUltrasequencerData.clear();
        userUltrasequencerProgress.clear();
        ultrasequencerDyeMap.clear();
        superpairsSlotMap.clear();
        superpairsSlotsToRead.clear();
        chronHasBeenEmpty = true;
        currentChronomatronRound = 0;
        currentUltraSequencerRound = 0;
        chronomatronSequenceIndex = 0;
        currentAddonPhase = null;
    }

    private static <T> T getOrNull(List<T> list, int index) {
        if (index < 0 || index >= list.size()) return null;
        return list.get(index);
    }

    private record UltraSequencerSlot(int sequenceNumber, int slotId) {
    }
}
