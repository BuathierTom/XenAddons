package xenus.xenaddons.features.impl.misc;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import org.lwjgl.glfw.GLFW;
import xenus.xenaddons.compat.XenHandledScreenCompat;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.KeybindSetting;
import xenus.xenaddons.utils.ItemPriceManager;

import java.util.Locale;

public class BazaarAHQuickSearchModule extends Module {
    public final KeybindSetting searchKey = new KeybindSetting("Search Key", GLFW.GLFW_KEY_M);

    private boolean wasPressed = false;

    public BazaarAHQuickSearchModule() {
        super("Bazaar/AH Quick Search", "Search hovered item with a keybind", Category.MISC);
        this.registerSetting(searchKey);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null || client.player == null) {
                wasPressed = false;
                return;
            }
            if (!this.isEnabled()) {
                wasPressed = false;
                return;
            }
            if (!(client.screen instanceof AbstractContainerScreen<?> handled)) {
                wasPressed = false;
                return;
            }

            boolean isPressed = isKeyPressed(searchKey.getKey());
            if (isPressed && !wasPressed) {
                runSearchForHoveredItem(handled);
            }
            wasPressed = isPressed;
        });
    }

    private boolean isKeyPressed(int key) {
        if (key == -1 || mc == null || mc.getWindow() == null) return false;
        long handle = mc.getWindow().handle();
        try {
            if (key < 0) {
                int btn = -100 - key;
                return btn >= 0 && GLFW.glfwGetMouseButton(handle, btn) == GLFW.GLFW_PRESS;
            }
            return GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void runSearchForHoveredItem(AbstractContainerScreen<?> handled) {
        var slot = XenHandledScreenCompat.getFocusedSlot(handled);
        if (slot == null || !slot.hasItem()) return;

        ItemStack stack = slot.getItem();
        if (stack == null || stack.isEmpty()) return;
        String itemName = sanitizeItemName(stack.getHoverName().getString(), stack);
        if (itemName.isBlank()) return;

        String cmd = buildCommand(handled.getTitle().getString(), stack, itemName);
        if (cmd == null || cmd.isBlank()) return;
        if (mc.player == null || mc.player.connection == null) return;
        mc.player.connection.sendCommand(cmd);
    }

    private String buildCommand(String screenTitle, ItemStack stack, String itemName) {
        // Auto only: detect from item id first, then from current gui title.
        ItemPriceManager.requestRefreshIfNeeded();
        String pricingId = ItemPriceManager.resolvePricingId(stack);
        if (pricingId != null && ItemPriceManager.getBazaarPrice(pricingId) != null) {
            return "bz " + itemName;
        }
        String title = stripFormatting(screenTitle).toLowerCase(Locale.ROOT);
        if (title.startsWith("bazaar")) return "bz " + itemName;
        if (title.startsWith("auctions")) return "ahs " + itemName;

        // Fallback when unknown menu context: prefer AH for non-bazaar entries.
        return "ahs " + itemName;
    }

    private static String sanitizeItemName(String raw, ItemStack stack) {
        if (raw == null) return "";
        String name = stripFormatting(raw);

        // Pets: "[Lvl 100] Sheep" -> "Sheep"
        name = name.replaceFirst("(?i)^\\[\\s*lvl\\s*\\d+\\s*]\\s*", "");

        // First pass: strip obvious decorative prefixes so reforge detection can match.
        name = name.replace("✪", "")
            .replace("➊", "")
            .replace("➋", "")
            .replace("➌", "")
            .replace("➍", "")
            .replace("➎", "");
        name = name.replaceAll("[\\p{So}\\p{Sk}]", " ");
        name = name.replaceAll("\\s+", " ").trim();

        String modifier = readModifier(stack);
        if (!modifier.isBlank()) {
            String lowerName = name.toLowerCase(Locale.ROOT);
            String lowerMod = modifier.toLowerCase(Locale.ROOT);
            if (lowerName.startsWith(lowerMod + " ")) {
                name = name.substring(modifier.length()).trim();
            }
        }

        // Fallback for cases where modifier is missing/incorrect in NBT.
        name = removeKnownReforgePrefix(name);

        // Keep only readable characters used in item names.
        name = name.replaceAll("[^\\p{L}\\p{N} '\\-()]", " ");
        name = name.replaceAll("\\s+", " ").trim();
        return name;
    }

    private static String removeKnownReforgePrefix(String name) {
        if (name == null || name.isBlank()) return "";
        int split = name.indexOf(' ');
        if (split <= 0) return name;

        String firstWord = name.substring(0, split).toLowerCase(Locale.ROOT);
        // Broad curated set of common hypixel reforges (armor/weapons/bows/tools/accessories).
        String[] reforges = {
            "ancient", "gentle", "fierce", "heavy", "light", "mythic", "pure", "smart", "titanic", "wise",
            "perfect", "necrotic", "loving", "giant", "spiked", "renowned", "cubic", "reinforced", "clean",
            "hasty", "sharp", "heroic", "spicy", "legendary", "odd", "fast", "fair", "epic", "awkward",
            "rich", "fine", "neat", "rapid", "deadly", "unreal", "precise", "headstrong", "spiritual",
            "fleet", "dirty", "fabled", "suspicious", "gilded", "warped", "withered", "livid", "bizarre",
            "itchy", "pleasant", "pretty", "shiny", "simple", "strange", "vivid", "godly", "demonic",
            "forceful", "hurtful", "strong", "superior", "keen", "fortuitous", "fortunate", "treacherous",
            "zealous", "silky", "bloody", "shaded", "sweet", "commando", "itchy", "hurtful", "warrior"
        };
        for (String reforge : reforges) {
            if (firstWord.equals(reforge)) {
                return name.substring(split + 1).trim();
            }
        }
        return name;
    }

    private static String readModifier(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return "";
        CompoundTag root = customData.copyTag();
        if (root == null) return "";
        CompoundTag extra = root.getCompound("ExtraAttributes").orElse(root.getCompound("extra_attributes").orElse(null));
        if (extra == null) return "";
        String mod = extra.getStringOr("modifier", "");
        if (mod == null || mod.isBlank()) mod = extra.getStringOr("MODIFIER", "");
        if (mod == null || mod.isBlank()) return "";
        return mod.replace('_', ' ').trim();
    }

    private static String stripFormatting(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.replaceAll("(?i)§[0-9A-FK-OR]", "");
    }
}
