package xenus.xenaddons.features.impl.dungeons;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.hud.HudState;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.settings.ColorSetting;
import xenus.xenaddons.utils.DungeonUtils;

public class LoadoutTitleModule extends Module {
    private static final Pattern EQUIPPED_LOADOUT_PATTERN = Pattern.compile("^You equipped (?!an?\\s)(.+)!$");
    private static final int NOTIFICATION_DURATION_TICKS = 8;

    private static final Pattern LOADOUTS_TITLE_PATTERN = Pattern.compile("^\\(\\d+/\\d+\\) Loadouts$");
    private static final Pattern LOCKED_LOADOUT_PATTERN = Pattern.compile("^Loadout \\d+ Locked$");
    private static final String UNEQUIPPED_MARKER = "Left-click to equip!";
    private static final String UNUSED_MARKER = "You must customize this loadout";
    private static final int[] LOADOUT_ICON_SLOTS = {14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43};
    private static final Pattern PET_PART_PATTERN = Pattern.compile("^Pet:.*$");
    private static final Pattern NONE_PART_PATTERN = Pattern.compile("^[^:]+:\\s*None$");

    private static final int SLOT_SIZE = 16;
    private static final int DEFAULT_HIGHLIGHT_COLOR = 0xC09BFF00;

    private final BooleanSetting inDungeonsOnly = new BooleanSetting("In Dungeons Only", false);
    private final BooleanSetting highlightActiveLoadout = new BooleanSetting("Highlight Active Loadout", true);
    public final ColorSetting highlightColor = new ColorSetting("Highlight Color", DEFAULT_HIGHLIGHT_COLOR);
    private final BooleanSetting hideNoneParts = new BooleanSetting("Hide None Parts", true);

    public LoadoutTitleModule() {
        super("Loadout Title", "Shows a title when you equip a loadout", Category.DUNGEON);
        registerSetting(inDungeonsOnly);
        registerSetting(highlightActiveLoadout);
        registerSetting(highlightColor);
        registerSetting(hideNoneParts);
    }

    public static void hideNoneParts(List<Component> tooltip) {
        LoadoutTitleModule module = ModuleManager.getModule(LoadoutTitleModule.class);
        if (module == null || !module.isEnabled() || !module.hideNoneParts.getValue()) return;
        if (tooltip == null || tooltip.size() < 2 || !isInLoadoutsMenu()) return;

        int start = -1;
        for (int i = 1; i < tooltip.size(); i++) {
            if (PET_PART_PATTERN.matcher(strip(tooltip.get(i).getString())).matches()) {
                start = i;
                break;
            }
        }
        if (start < 0) return;

        int end = start;
        while (end < tooltip.size() && !strip(tooltip.get(end).getString()).isEmpty()) {
            end++;
        }

        List<Component> section = tooltip.subList(start, end);
        boolean removedAll = section.stream().allMatch(line -> NONE_PART_PATTERN.matcher(strip(line.getString())).matches());
        section.removeIf(line -> NONE_PART_PATTERN.matcher(strip(line.getString())).matches());

        if (removedAll && start > 1 && strip(tooltip.get(start - 1).getString()).isEmpty()) {
            tooltip.remove(start - 1);
        }
    }

    public static void renderActiveLoadoutHighlight(AbstractContainerScreen<?> screen, GuiGraphicsExtractor context) {
        LoadoutTitleModule module = ModuleManager.getModule(LoadoutTitleModule.class);
        if (module == null || !module.isEnabled() || !module.highlightActiveLoadout.getValue()) return;
        if (screen == null || context == null || !isLoadoutsScreen(screen)) return;

        List<Slot> slots = screen.getMenu().slots;
        for (int index : LOADOUT_ICON_SLOTS) {
            if (index >= slots.size()) continue;

            Slot slot = slots.get(index);
            if (!isEquippedLoadout(slot.getItem())) continue;

            drawHighlight(context, slot.x, slot.y, module.getHighlightColor());
        }
    }

    private static boolean isLoadoutsScreen(AbstractContainerScreen<?> screen) {
        Component title = screen.getTitle();
        return title != null && LOADOUTS_TITLE_PATTERN.matcher(strip(title.getString())).matches();
    }

    public static boolean isInLoadoutsMenu() {
        Screen screen = Minecraft.getInstance().screen;
        return screen instanceof AbstractContainerScreen<?> container && isLoadoutsScreen(container);
    }

    private static boolean isEquippedLoadout(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        ItemLore lore = stack.get(DataComponents.LORE);
        List<String> loreLines = new ArrayList<>();
        if (lore != null) {
            for (Component line : lore.lines()) {
                if (line != null) loreLines.add(line.getString());
            }
        }
        return isEquippedLoadout(stack.getHoverName().getString(), loreLines);
    }

    static boolean isEquippedLoadout(String displayName, List<String> loreLines) {
        if (LOCKED_LOADOUT_PATTERN.matcher(strip(displayName)).matches()) return false;
        if (loreLines == null || loreLines.isEmpty()) return false;

        for (String line : loreLines) {
            if (line == null) continue;

            String text = strip(line);
            if (text.contains(UNEQUIPPED_MARKER) || text.contains(UNUSED_MARKER)) return false;
        }
        return true;
    }

    private static void drawHighlight(GuiGraphicsExtractor context, int x, int y, int color) {
        context.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, color);
    }

    public int getHighlightColor() {
        Integer color = highlightColor.getValue();
        return color == null ? DEFAULT_HIGHLIGHT_COLOR : color;
    }

    private static String strip(String input) {
        if (input == null) return "";
        return input.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
    }

    public static void init() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
            !checkAndProcess(message.getString())
        );
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
            !checkAndProcess(message.getString())
        );
    }

    private static boolean checkAndProcess(String message) {
        LoadoutTitleModule module = ModuleManager.getModule(LoadoutTitleModule.class);
        return module != null && module.isEnabled() && module.handleAndFilter(message);
    }

    private boolean handleAndFilter(String message) {
        Matcher matcher = EQUIPPED_LOADOUT_PATTERN.matcher(message.trim());
        if (!matcher.matches()) {
            return false;
        }

        String loadoutName = matcher.group(1).trim();
        if (loadoutName.isEmpty()) {
            return false;
        }

        mc.execute(() -> showNotification(loadoutName));
        return true;
    }

    private void showNotification(String loadoutName) {
        if (!inDungeonsOnly.getValue() || DungeonUtils.inDungeon()) {
            HudState.PET_NOTIFICATION.show(
                Component.literal("§a" + loadoutName),
                Component.literal("§bLoadout§r"),
                NOTIFICATION_DURATION_TICKS
            );
        }

        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("§bLoadout §7> §a" + loadoutName));
        }
    }

    public void debugTrigger() {
        showNotification("Farming");
    }
}
