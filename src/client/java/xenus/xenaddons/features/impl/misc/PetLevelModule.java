package xenus.xenaddons.features.impl.misc;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import xenus.xenaddons.compat.XenDrawContextCompat;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.settings.ColorSetting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PetLevelModule extends Module {

    private static final int DEFAULT_COLOR = 0xFFFFDDC1;
    private static final float SLOT_SIZE = 16.0f;

    private static final Pattern LEVEL_PATTERN = Pattern.compile("^(?:⭐\\s*)?\\[Lvl (\\d+)]");

    private static final int COMMON_MAX_LEVEL = 100;
    private static final int DRAGON_MAX_LEVEL = 200;

    public final ColorSetting textColor = new ColorSetting("Text Color", DEFAULT_COLOR);

    public PetLevelModule() {
        super("Pet Level", "Shows a pet's level in the corner of its slot, hidden once it is maxed", Category.MISC);
        this.registerSetting(textColor);
    }

    public static void renderPetLevel(GuiGraphicsExtractor context, Font font, ItemStack stack, int x, int y) {
        PetLevelModule module = ModuleManager.getModule(PetLevelModule.class);
        if (module == null || !module.isEnabled()) return;

        String level = resolveLevel(stack);
        if (level == null) return;

        drawTopLeft(context, font, level, x, y, module.getTextColor());
    }

    public int getTextColor() {
        Integer color = textColor.getValue();
        return color == null ? DEFAULT_COLOR : color;
    }

    static String resolveLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (!stack.is(Items.PLAYER_HEAD)) return null;

        return levelFor(stack.getHoverName().getString());
    }

    static String levelFor(String displayName) {
        String name = stripFormatting(displayName);
        if (name == null) return null;

        Matcher matcher = LEVEL_PATTERN.matcher(name);
        if (!matcher.find()) return null;

        int level;
        try {
            level = Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }

        return isMaxed(level, name) ? null : matcher.group(1);
    }

    private static boolean isMaxed(int level, String name) {
        boolean highCapDragon = name.contains("Dragon") && !name.contains("Ender Dragon");
        return level >= (highCapDragon ? DRAGON_MAX_LEVEL : COMMON_MAX_LEVEL);
    }

    private static void drawTopLeft(GuiGraphicsExtractor context, Font font, String label, int x, int y, int color) {
        float width = Math.max(1.0f, font.width(label));
        float scale = Math.min(1.0f, SLOT_SIZE / width);

        context.pose().pushMatrix();
        context.pose().translate((float) x, (float) y);
        context.pose().scale(scale, scale);
        XenDrawContextCompat.text(context, font, label, 0, 0, color);
        context.pose().popMatrix();
    }

    private static String stripFormatting(String input) {
        if (input == null) return null;
        return input.replaceAll("(?i)[§&][0-9A-FK-OR]", "").trim();
    }
}
