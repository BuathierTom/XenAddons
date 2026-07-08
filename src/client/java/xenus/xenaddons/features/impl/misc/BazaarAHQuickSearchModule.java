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
        String itemName = ItemPriceManager.resolveQuickSearchName(stack);
        if (itemName == null || itemName.isBlank()) {
            itemName = sanitizeItemName(stack.getHoverName().getString(), stack);
        }
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
        if (pricingId != null && (pricingId.startsWith("ENCHANTMENT_") || ItemPriceManager.getBazaarPrice(pricingId) != null)) {
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

        String lowerName = name.toLowerCase(Locale.ROOT);

        String[] reforges = {
            "ambered", "ancient", "astute", "auspicious", "awkward", "beady", "bizarre", "blazing",
            "blended", "blessed", "blood-soaked", "bloodshot", "bloody", "blooming", "bountiful",
            "brilliant", "bulky", "bustling", "buzzing", "candied", "chomp", "clean", "coldfused",
            "colossal", "cubic", "deadly", "demonic", "dimensional", "dirty", "double-bit", "earthy",
            "empowered", "epic", "erudite", "excellent", "fabled", "fair", "fanged", "fast",
            "festive", "fierce", "fine", "fleet", "forceful", "fortified", "fortunate", "fruitful",
            "gentle", "giant", "gilded", "glacial", "glistening", "godly", "grand", "great",
            "greater spook", "green thumb", "hasty", "headstrong", "heated", "heavy", "hefty",
            "heroic", "honored", "hurtful", "hyper", "itchy", "jaded", "jerry", "keen",
            "legendary", "light", "loving", "lucky", "lumberjack's", "lush", "lustrous", "magnetic",
            "menacing", "mithraic", "moil", "mossy", "mythic", "neat", "necrotic", "odd",
            "ominous", "peasant's", "perfect", "pitchin'", "pleasant", "precise", "pretty",
            "prospector's", "pure", "rapid", "refined", "reinforced", "renowned", "rich",
            "ridiculous", "robust", "rooted", "royal", "rugged", "salty", "scraped", "shaded",
            "sharp", "shiny", "silky", "simple", "smart", "snowy", "soft", "spicy", "spiritual",
            "spiked", "stained", "stellar", "stiff", "strange", "strengthened", "strong", "sturdy",
            "superior", "suspicious", "sunny", "sweet", "treacherous", "undead", "unyielding", "warped",
            "waxed", "withered", "zooming"
        };
        for (String reforge : reforges) {
            if (lowerName.equals(reforge)) return "";
            if (lowerName.startsWith(reforge + " ")) {
                return name.substring(reforge.length() + 1).trim();
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
