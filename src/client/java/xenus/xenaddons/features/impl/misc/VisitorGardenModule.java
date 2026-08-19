package xenus.xenaddons.features.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import xenus.xenaddons.compat.XenHandledScreenCompat;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.utils.ChatUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VisitorGardenModule extends Module {
    private static final String REQUIRED_HEADER = "Items Required";
    private static final String HINT = "§eRight-click to search on the bazaar";
    private static final String READY_MARKER = "Click to give";
    private static final Pattern AMOUNT_SUFFIX = Pattern.compile("^(.*?)\\s+x\\s*([\\d,.]+)$", Pattern.CASE_INSENSITIVE);

    public VisitorGardenModule() {
        super("Visitor Garden", "Right click a visitor offer to open the Bazaar for the item it wants", Category.MISC);
    }

    public static boolean onMouseClicked(AbstractContainerScreen<?> screen, int button) {
        VisitorGardenModule module = ModuleManager.getModule(VisitorGardenModule.class);
        if (module == null || !module.isEnabled()) return false;
        if (button != 1 || screen == null) return false;

        Slot slot = XenHandledScreenCompat.getFocusedSlot(screen);
        if (slot == null || !slot.hasItem()) return false;

        Required required = parseFirstRequired(slot.getItem());
        if (required == null) return false;

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.player.connection == null) return false;

        if (client.keyboardHandler != null) client.keyboardHandler.setClipboard(required.amount);
        client.player.connection.sendCommand("bz " + required.name);
        ChatUtils.info("§7Copied §e" + required.amount + " §7to clipboard for §a" + required.name);
        return true;
    }

    public static void appendHint(List<Component> tooltip, ItemStack stack) {
        VisitorGardenModule module = ModuleManager.getModule(VisitorGardenModule.class);
        if (module == null || !module.isEnabled()) return;
        if (tooltip == null || tooltip.isEmpty()) return;
        if (parseFirstRequired(stack) == null) return;

        if (!tooltip.get(tooltip.size() - 1).getString().isBlank()) {
            tooltip.add(Component.literal(""));
        }
        tooltip.add(Component.literal(HINT));
    }

    private static Required parseFirstRequired(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return null;
        return parseFirstRequired(lore.lines());
    }

    static Required parseFirstRequired(List<Component> lines) {
        if (lines == null || lines.isEmpty()) return null;
        if (isOfferReady(lines)) return null;

        int start = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (stripFormatting(lines.get(i).getString()).strip().startsWith(REQUIRED_HEADER)) {
                start = i + 1;
                break;
            }
        }
        if (start < 0) return null;

        for (int i = start; i < lines.size(); i++) {
            String text = stripFormatting(lines.get(i).getString()).strip();
            if (text.isEmpty() || text.endsWith(":")) break;

            Matcher matcher = AMOUNT_SUFFIX.matcher(text);
            if (matcher.matches()) {
                String name = matcher.group(1).strip();
                String amount = matcher.group(2).replaceAll("[^0-9]", "");
                if (!name.isEmpty() && !amount.isEmpty()) return new Required(name, amount);
                continue;
            }
            return new Required(text, "1");
        }
        return null;
    }

    private static boolean isOfferReady(List<Component> lines) {
        for (Component line : lines) {
            if (stripFormatting(line.getString()).contains(READY_MARKER)) return true;
        }
        return false;
    }

    private static String stripFormatting(String input) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '§') {
                if (i + 1 < input.length()) i++;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    private record Required(String name, String amount) {}
}
