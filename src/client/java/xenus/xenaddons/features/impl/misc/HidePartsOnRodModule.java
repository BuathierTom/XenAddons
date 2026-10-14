package xenus.xenaddons.features.impl.misc;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import xenus.xenaddons.compat.XenInputCompat;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.settings.KeybindSetting;
import xenus.xenaddons.utils.ItemPriceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HidePartsOnRodModule extends Module {
    public final KeybindSetting toggleKey = new KeybindSetting("Toggle Key", -1);

    private boolean toggleWasDown = false;

    public HidePartsOnRodModule() {
        super("Hide Parts on Rod", "Hides the Hook, Line and Sinker lines from fishing rod tooltips", Category.MISC);
        this.registerSetting(toggleKey);

        // Not gated on isEnabled(): the key has to be able to switch the module back on,
        // and it must work with a screen open since that is when rod tooltips are read.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean down = isKeyDown(toggleKey.getKey());
            if (down && !toggleWasDown) toggle();
            toggleWasDown = down;
        });
    }

    private boolean isKeyDown(int key) {
        if (key == -1 || mc == null || mc.getWindow() == null) return false;
        try {
            if (key < 0) {
                int button = -100 - key;
                return button >= 0 && XenInputCompat.isMouseButtonDown(button);
            }
            return XenInputCompat.isKeyDown(key);
        } catch (Exception ignored) {
            return false;
        }
    }

    public static void hideRodParts(List<Component> tooltip, ItemStack stack) {
        HidePartsOnRodModule module = ModuleManager.getModule(HidePartsOnRodModule.class);
        if (module == null || !module.isEnabled()) return;
        if (tooltip == null || tooltip.size() < 2 || !isFishingRod(stack)) return;

        boolean removedEmptySlot = false;
        for (int i = 1; i < tooltip.size(); i++) {
            if (isPartLine(stripFormatting(tooltip.get(i).getString()))) {
                removedEmptySlot = true;
                break;
            }
        }
        if (!removedEmptySlot) return;

        List<Component> kept = new ArrayList<>(tooltip.size());
        for (int i = 0; i < tooltip.size(); i++) {
            String plain = stripFormatting(tooltip.get(i).getString());
            // Index 0 is the item name, never touch it.
            if (i > 0 && (isPartLine(plain) || isRoddyHint(plain))) continue;
            kept.add(tooltip.get(i));
        }

        if (kept.size() == tooltip.size()) return;

        collapseBlankRuns(kept);
        tooltip.clear();
        tooltip.addAll(kept);
    }

    private static boolean isFishingRod(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.is(Items.FISHING_ROD)) return true;
        String id = ItemPriceManager.extractSkyBlockId(stack);
        return id != null && id.toUpperCase(Locale.ROOT).contains("ROD");
    }

    // Part lines look like "<glyph> Hook NONE". The glyph is not worth matching: it may be a
    // surrogate pair, a private-use codepoint, or something Java classifies as a letter, so we
    // only require the keyword to sit at the very start, right after that short prefix.
    // Safe because hideRodParts() already restricted us to fishing rods.
    private static final int MAX_GLYPH_PREFIX = 3;
    private static final String EMPTY_SLOT = "NONE";

    private static boolean isPartLine(String plain) {
        String text = plain.strip();
        if (text.isEmpty()) return false;

        for (String part : new String[] {"Hook", "Line", "Sinker"}) {
            int at = text.indexOf(part);
            if (at < 0 || at > MAX_GLYPH_PREFIX) continue;
            // Only empty slots. A fitted part reads "<glyph> Hotspot Hook", so the keyword
            // lands past the prefix anyway, but requiring NONE makes the intent explicit.
            String rest = text.substring(at + part.length()).strip();
            if (rest.startsWith(":")) rest = rest.substring(1).strip();
            if (rest.equalsIgnoreCase(EMPTY_SLOT)) return true;
        }
        return false;
    }

    private static boolean isRoddyHint(String plain) {
        String text = plain.toLowerCase(Locale.ROOT);
        return text.contains("apply parts to this rod")
            || (text.contains("roddy") && text.contains("backwater"));
    }

    // Removing the parts block can leave two blank lines back to back, or a trailing one.
    private static void collapseBlankRuns(List<Component> lines) {
        for (int i = lines.size() - 1; i > 0; i--) {
            boolean blank = lines.get(i).getString().isBlank();
            if (blank && (i == lines.size() - 1 || lines.get(i - 1).getString().isBlank())) {
                lines.remove(i);
            }
        }
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
}
