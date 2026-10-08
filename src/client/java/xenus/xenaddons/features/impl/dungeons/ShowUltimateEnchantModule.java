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
import xenus.xenaddons.settings.ColorSetting;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class ShowUltimateEnchantModule extends Module {
    private static final int DEFAULT_LABEL_COLOR = 0xFFFF55FF;
    private static final float SLOT_SIZE = 16.0f;
    private static final float EDGE_PADDING = 1.0f;
    private static final float LABEL_SCALE_FACTOR = 0.9f;

    private static final String TERMINATOR_ID = "TERMINATOR";
    private static final String TERMINATOR_NAME = "Terminator";
    private static final String HYPERION_ID = "HYPERION";
    private static final String HYPERION_NAME = "Hyperion";

    private static final String SOUL_EATER_LABEL = "SE";
    private static final String DUPLEX_LABEL = "DU";
    private static final String CHIMERA_LABEL = "CH";
    private static final String SWARM_LABEL = "SW";
    private static final String ULTIMATE_WISE_LABEL = "UW";
    private static final String COMBO_LABEL = "CB";
    private static final String FATAL_TEMPO_LABEL = "FT";
    private static final String INFERNO_LABEL = "I";

    /** Duplex is still stored under its original name, reiterate, in the item NBT. */
    private static final Map<String, String> TERMINATOR_ENCHANT_KEYS = Map.of(
        "ultimate_soul_eater", SOUL_EATER_LABEL,
        "ultimate_reiterate", DUPLEX_LABEL,
        "ultimate_duplex", DUPLEX_LABEL
    );

    private static final Map<String, String> HYPERION_ENCHANT_KEYS = Map.of(
        "ultimate_chimera", CHIMERA_LABEL,
        "ultimate_swarm", SWARM_LABEL,
        "ultimate_wise", ULTIMATE_WISE_LABEL,
        "ultimate_combo", COMBO_LABEL,
        "ultimate_soul_eater", SOUL_EATER_LABEL,
        "ultimate_fatal_tempo", FATAL_TEMPO_LABEL,
        "ultimate_inferno", INFERNO_LABEL
    );

    private static final Map<Pattern, String> TERMINATOR_LORE_PATTERNS = compileLorePatterns(Map.of(
        "Soul Eater", SOUL_EATER_LABEL,
        "Duplex", DUPLEX_LABEL
    ));

    private static final Map<Pattern, String> HYPERION_LORE_PATTERNS = compileLorePatterns(Map.of(
        "Chimera", CHIMERA_LABEL,
        "Swarm", SWARM_LABEL,
        "Ultimate Wise", ULTIMATE_WISE_LABEL,
        "Combo", COMBO_LABEL,
        "Soul Eater", SOUL_EATER_LABEL,
        "Fatal Tempo", FATAL_TEMPO_LABEL,
        "Inferno", INFERNO_LABEL
    ));

    public final BooleanSetting showOnTerminator = new BooleanSetting("Terminator", true);
    public final BooleanSetting showOnHyperion = new BooleanSetting("Hyperion", true);
    public final ColorSetting labelColor = new ColorSetting("Label Color", DEFAULT_LABEL_COLOR);

    public ShowUltimateEnchantModule() {
        super("Show Ultimate Enchant", "Marks a Terminator or Hyperion carrying a tracked ultimate enchant", Category.DUNGEON);
        registerSetting(showOnTerminator);
        registerSetting(showOnHyperion);
        registerSetting(labelColor);
    }

    public static void renderItemLabel(GuiGraphicsExtractor context, Font font, ItemStack stack, int x, int y) {
        ShowUltimateEnchantModule module = ModuleManager.getModule(ShowUltimateEnchantModule.class);
        if (module == null || !module.isEnabled()) return;

        String label = module.resolveLabel(stack);
        if (label == null) return;

        drawLabel(context, font, label, x, y, module.labelColor.getValue());
    }

    private String resolveLabel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        CompoundTag extra = readExtraAttributes(stack);

        if (showOnTerminator.getValue() && matchesItem(stack, extra, TERMINATOR_ID, TERMINATOR_NAME)) {
            String label = readLabelFromEnchantments(extra, TERMINATOR_ENCHANT_KEYS);
            if (label != null) return label;
            return readLabelFromLore(stack, TERMINATOR_LORE_PATTERNS);
        }

        if (showOnHyperion.getValue() && matchesItem(stack, extra, HYPERION_ID, HYPERION_NAME)) {
            String label = readLabelFromEnchantments(extra, HYPERION_ENCHANT_KEYS);
            if (label != null) return label;
            return readLabelFromLore(stack, HYPERION_LORE_PATTERNS);
        }

        return null;
    }

    /**
     * Menu display items lose their SkyBlock id, so accept the name as well. Reforges only
     * prefix the name, and the label still needs a matching enchantment to show up.
     */
    private static boolean matchesItem(ItemStack stack, CompoundTag extra, String id, String name) {
        if (id.equals(readString(extra, "id"))) return true;

        String displayName = stripFormatting(stack.getHoverName().getString());
        return displayName != null && displayName.contains(name);
    }

    private static String readLabelFromEnchantments(CompoundTag extra, Map<String, String> labelsByKey) {
        if (extra == null) return null;

        CompoundTag enchantments = extra.getCompound("enchantments").orElse(null);
        if (enchantments == null || enchantments.isEmpty()) return null;

        for (String key : enchantments.keySet()) {
            String label = labelsByKey.get(key.toLowerCase(Locale.ROOT));
            if (label != null) return label;
        }

        return null;
    }

    private static String readLabelFromLore(ItemStack stack, Map<Pattern, String> lorePatterns) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null || lore.lines().isEmpty()) return null;

        for (Component line : lore.lines()) {
            if (line == null) continue;

            String text = stripFormatting(line.getString());
            if (text == null || text.isBlank()) continue;

            for (Map.Entry<Pattern, String> entry : lorePatterns.entrySet()) {
                if (entry.getKey().matcher(text).find()) return entry.getValue();
            }
        }

        return null;
    }

    private static Map<Pattern, String> compileLorePatterns(Map<String, String> labelsByName) {
        Map<Pattern, String> patterns = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : labelsByName.entrySet()) {
            String regex = "\\b" + entry.getKey().replace(" ", "\\s+") + "\\b";
            patterns.put(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), entry.getValue());
        }
        return patterns;
    }

    private static void drawLabel(GuiGraphicsExtractor context, Font font, String label, int x, int y, int color) {
        float width = Math.max(1.0f, font.width(label));
        float scale = Math.min(1.0f, SLOT_SIZE / width) * LABEL_SCALE_FACTOR;
        float drawX = x + SLOT_SIZE - (width * scale) - EDGE_PADDING;
        float drawY = y + EDGE_PADDING;

        context.pose().pushMatrix();
        context.pose().translate(drawX, drawY);
        context.pose().scale(scale, scale);
        XenDrawContextCompat.text(context, font, label, 0, 0, color);
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
