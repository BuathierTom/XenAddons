package xenus.xenaddons.features.impl.dungeons;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import xenus.xenaddons.compat.XenDrawContextCompat;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.settings.BooleanSetting;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class ShowUltimateEnchantModule extends Module {
    private static final int LABEL_COLOR = 0xFFFF55FF;
    private static final float SLOT_SIZE = 16.0f;
    private static final String TERMINATOR_ID = "TERMINATOR";
    private static final String TERMINATOR_NAME = "Terminator";
    private static final String SOUL_EATER_LABEL = "SE";
    private static final String DUPLEX_LABEL = "DU";

    /** Duplex is still stored under its original name, reiterate, in the item NBT. */
    private static final Map<String, String> LABELS_BY_ENCHANT_KEY = Map.of(
        "ultimate_soul_eater", SOUL_EATER_LABEL,
        "ultimate_reiterate", DUPLEX_LABEL,
        "ultimate_duplex", DUPLEX_LABEL
    );

    private static final Pattern SOUL_EATER_PATTERN = Pattern.compile("\\bSoul\\s+Eater\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DUPLEX_PATTERN = Pattern.compile("\\bDuplex\\b", Pattern.CASE_INSENSITIVE);

    public final BooleanSetting showOnTerminator = new BooleanSetting("Terminator", true);

    public ShowUltimateEnchantModule() {
        super("Show Ultimate Enchant", "Marks a Terminator carrying Soul Eater or Duplex", Category.DUNGEON);
        registerSetting(showOnTerminator);
    }

    public static void renderItemLabel(GuiGraphicsExtractor context, Font font, ItemStack stack, int x, int y) {
        ShowUltimateEnchantModule module = ModuleManager.getModule(ShowUltimateEnchantModule.class);
        if (module == null || !module.isEnabled()) return;

        String label = module.resolveLabel(stack);
        if (label == null) return;

        drawLabel(context, font, label, x, y);
    }

    private String resolveLabel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (!showOnTerminator.getValue()) return null;

        CompoundTag extra = readExtraAttributes(stack);
        if (!isTerminator(stack, extra)) return null;

        String fromEnchantments = readLabelFromEnchantments(extra);
        if (fromEnchantments != null) return fromEnchantments;

        return readLabelFromLore(stack);
    }

    /**
     * Menu display items lose their SkyBlock id, so accept the name as well. Reforges only
     * prefix the name, and the label still needs a matching enchantment to show up.
     */
    private static boolean isTerminator(ItemStack stack, CompoundTag extra) {
        if (TERMINATOR_ID.equals(readString(extra, "id"))) return true;

        String displayName = stripFormatting(stack.getHoverName().getString());
        return displayName != null && displayName.contains(TERMINATOR_NAME);
    }

    private static String readLabelFromEnchantments(CompoundTag extra) {
        if (extra == null) return null;

        CompoundTag enchantments = extra.getCompound("enchantments").orElse(null);
        if (enchantments == null || enchantments.isEmpty()) return null;

        for (String key : enchantments.keySet()) {
            String label = LABELS_BY_ENCHANT_KEY.get(key.toLowerCase(Locale.ROOT));
            if (label != null) return label;
        }

        return null;
    }

    private static String readLabelFromLore(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null || lore.lines().isEmpty()) return null;

        for (Component line : lore.lines()) {
            if (line == null) continue;

            String text = stripFormatting(line.getString());
            if (text == null || text.isBlank()) continue;
            if (SOUL_EATER_PATTERN.matcher(text).find()) return SOUL_EATER_LABEL;
            if (DUPLEX_PATTERN.matcher(text).find()) return DUPLEX_LABEL;
        }

        return null;
    }

    private static void drawLabel(GuiGraphicsExtractor context, Font font, String label, int x, int y) {
        float width = Math.max(1.0f, font.width(label));
        float scale = Math.min(1.0f, SLOT_SIZE / width);
        float drawX = x + SLOT_SIZE - (width * scale);

        context.pose().pushMatrix();
        context.pose().translate(drawX, (float) y);
        context.pose().scale(scale, scale);
        XenDrawContextCompat.text(context, font, label, 0, 0, LABEL_COLOR);
        context.pose().popMatrix();
    }

    private static CompoundTag readExtraAttributes(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;

        CompoundTag root = customData.copyTag();
        if (root == null) return null;

        return root.getCompound("ExtraAttributes")
            .orElse(root.getCompound("extra_attributes").orElse(null));
    }

    private static String readString(CompoundTag nbt, String key) {
        if (nbt == null) return null;
        String value = nbt.getStringOr(key, "");
        return value == null || value.isBlank() ? null : value;
    }

    private static String stripFormatting(String input) {
        if (input == null) return null;
        return input.replaceAll("(?i)[§&][0-9A-FK-OR]", "").trim();
    }
}
