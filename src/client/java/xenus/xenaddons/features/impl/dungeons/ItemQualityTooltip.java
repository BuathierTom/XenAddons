package xenus.xenaddons.features.impl.dungeons;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import xenus.xenaddons.features.ModuleManager;

import java.util.List;
import java.util.Locale;

public final class ItemQualityTooltip {
    private ItemQualityTooltip() {
    }

    public static void appendQualityBonus(List<Text> tooltip, ItemStack stack) {
        ItemQualityModule module = ModuleManager.getModule(ItemQualityModule.class);
        if (module == null || !module.isEnabled()) return;
        if (tooltip == null || stack == null || stack.isEmpty()) return;
        if (tooltip.stream().map(Text::getString).anyMatch(s -> s != null && s.contains("Quality Bonus:"))) return;

        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) return;
        NbtCompound root = customData.copyNbt();
        if (root == null) return;
        NbtCompound extra = resolveExtraAttributes(root);
        if (extra == null) extra = root;

        int boost = readIntFlexible(extra, "baseStatBoostPercentage", "base_stat_boost_percentage");
        if (boost <= 0) {
            boost = readIntFlexible(root, "baseStatBoostPercentage", "base_stat_boost_percentage");
        }
        if (boost <= 0) return;

        int tier = readIntFlexible(extra, "item_tier", "dungeon_item_level");
        if (tier <= 0) tier = readIntFlexible(root, "item_tier", "dungeon_item_level");
        String req = readStringFlexible(extra, "dungeon_skill_req", "dungeonSkillReq");
        if (req.isBlank()) req = readStringFlexible(root, "dungeon_skill_req", "dungeonSkillReq");
        String floor = resolveFloorDisplay(req, tier);
        String color = resolveBoostColor(boost);
        Text qualityLine = Text.literal("§6Quality Bonus: " + color + "+" + boost + "% §7(" + floor + "§7)");
        int insertIndex = findRarityLineInsertIndex(tooltip);
        if (insertIndex >= 0 && insertIndex <= tooltip.size()) {
            tooltip.add(insertIndex, qualityLine);
        } else {
            tooltip.add(qualityLine);
        }
    }

    private static int findRarityLineInsertIndex(List<Text> tooltip) {
        for (int i = tooltip.size() - 1; i >= 0; i--) {
            String line = tooltip.get(i).getString();
            if (line == null) continue;
            String normalized = line.trim().toUpperCase(Locale.ROOT);
            if (normalized.isEmpty()) continue;

            boolean hasRarityWord = normalized.contains("VERY SPECIAL")
                || normalized.contains("SPECIAL")
                || normalized.contains("SUPREME")
                || normalized.contains("DIVINE")
                || normalized.contains("MYTHIC")
                || normalized.contains("LEGENDARY")
                || normalized.contains("EPIC")
                || normalized.contains("RARE")
                || normalized.contains("UNCOMMON")
                || normalized.contains("COMMON");
            if (!hasRarityWord) continue;

            // The rarity line is usually all-uppercase ("MYTHIC DUNGEON CHESTPLATE", etc).
            if (!normalized.equals(line.trim().toUpperCase(Locale.ROOT))) continue;

            if (normalized.contains(" DUNGEON ")
                || normalized.contains(" HELMET")
                || normalized.contains(" CHESTPLATE")
                || normalized.contains(" LEGGINGS")
                || normalized.contains(" BOOTS")
                || normalized.contains(" SWORD")
                || normalized.contains(" BOW")
                || normalized.contains(" WAND")
                || normalized.contains(" STAFF")
                || normalized.contains(" GAUNTLET")
                || normalized.contains(" BELT")
                || normalized.contains(" CLOAK")
                || normalized.contains(" NECKLACE")
                || normalized.contains(" BRACELET")) {
                return i + 1;
            }
        }
        return -1;
    }

    private static NbtCompound resolveExtraAttributes(NbtCompound root) {
        if (root == null) return null;
        NbtCompound extra = root.getCompound("ExtraAttributes").orElse(root.getCompound("extra_attributes").orElse(null));
        if (extra != null) return extra;

        NbtCompound tag = root.getCompound("tag").orElse(null);
        if (tag == null) return null;
        return tag.getCompound("ExtraAttributes").orElse(tag.getCompound("extra_attributes").orElse(null));
    }

    private static int readIntFlexible(NbtCompound nbt, String... keys) {
        if (nbt == null || keys == null) return 0;
        for (String key : keys) {
            if (key == null || key.isBlank()) continue;
            if (!nbt.contains(key)) continue;
            NbtElement element = nbt.get(key);
            if (element == null) continue;
            try {
                return switch (element.getType()) {
                    case NbtElement.BYTE_TYPE -> ((net.minecraft.nbt.NbtByte) element).byteValue();
                    case NbtElement.SHORT_TYPE -> ((net.minecraft.nbt.NbtShort) element).shortValue();
                    case NbtElement.INT_TYPE -> ((net.minecraft.nbt.NbtInt) element).intValue();
                    case NbtElement.LONG_TYPE -> (int) ((net.minecraft.nbt.NbtLong) element).longValue();
                    case NbtElement.FLOAT_TYPE -> Math.round(((net.minecraft.nbt.NbtFloat) element).floatValue());
                    case NbtElement.DOUBLE_TYPE -> (int) Math.round(((net.minecraft.nbt.NbtDouble) element).doubleValue());
                    case NbtElement.STRING_TYPE -> {
                        String raw = ((net.minecraft.nbt.NbtString) element).value();
                        try {
                            yield Integer.parseInt(raw);
                        } catch (Exception ignored) {
                            yield 0;
                        }
                    }
                    default -> 0;
                };
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    private static String readStringFlexible(NbtCompound nbt, String... keys) {
        if (nbt == null || keys == null) return "";
        for (String key : keys) {
            if (key == null || key.isBlank()) continue;
            String s = nbt.getString(key, "");
            if (s != null && !s.isBlank()) return s;
        }
        return "";
    }

    private static String resolveFloorDisplay(String req, int tier) {
        if (req == null) req = "";

        if (req.isEmpty() && tier == 0) return "§aE";
        if (req.isEmpty()) return "§bF" + tier;

        String[] parts = req.split(":", 2);
        String dungeon = parts.length > 0 ? parts[0] : "";
        int levelReq = 0;
        if (parts.length > 1) {
            try {
                levelReq = Integer.parseInt(parts[1]);
            } catch (Exception ignored) {
                levelReq = 0;
            }
        }

        if ("CATACOMBS".equals(dungeon)) {
            if (levelReq - tier > 19) {
                return "§4M" + (tier - 3);
            } else {
                return "§aF" + tier;
            }
        }

        return "§b" + dungeon + " " + tier;
    }

    private static String resolveBoostColor(int boost) {
        if (boost <= 17) return "§c";
        if (boost <= 33) return "§e";
        if (boost <= 49) return "§a";
        return "§b";
    }
}
