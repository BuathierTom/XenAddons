package xenus.xenaddons.features.impl.dungeons;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import xenus.xenaddons.features.ModuleManager;

import java.util.List;
import java.util.Locale;

public final class ItemQualityTooltip {
    private ItemQualityTooltip() {
    }

    public static void appendQualityBonus(List<Component> tooltip, ItemStack stack) {
        ItemQualityModule module = ModuleManager.getModule(ItemQualityModule.class);
        if (module == null || !module.isEnabled()) return;
        if (tooltip == null || stack == null || stack.isEmpty()) return;
        if (tooltip.stream().map(Component::getString).anyMatch(s -> s != null && s.contains("Quality Bonus:"))) return;

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        CompoundTag root = customData.copyTag();
        if (root == null) return;
        CompoundTag extra = resolveExtraAttributes(root);
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
        Component qualityLine = Component.literal("§6Quality Bonus: " + color + "+" + boost + "% §7(" + floor + "§7)");
        int insertIndex = findRarityLineInsertIndex(tooltip);
        if (insertIndex >= 0 && insertIndex <= tooltip.size()) {
            tooltip.add(insertIndex, qualityLine);
        } else {
            tooltip.add(qualityLine);
        }
    }

    private static int findRarityLineInsertIndex(List<Component> tooltip) {
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

    private static CompoundTag resolveExtraAttributes(CompoundTag root) {
        if (root == null) return null;
        CompoundTag extra = root.getCompound("ExtraAttributes").orElse(root.getCompound("extra_attributes").orElse(null));
        if (extra != null) return extra;

        CompoundTag tag = root.getCompound("tag").orElse(null);
        if (tag == null) return null;
        return tag.getCompound("ExtraAttributes").orElse(tag.getCompound("extra_attributes").orElse(null));
    }

    private static int readIntFlexible(CompoundTag nbt, String... keys) {
        if (nbt == null || keys == null) return 0;
        for (String key : keys) {
            if (key == null || key.isBlank()) continue;
            if (!nbt.contains(key)) continue;
            Tag element = nbt.get(key);
            if (element == null) continue;
            try {
                Number number = element.asNumber().orElse(null);
                if (number != null) {
                    return Math.round(number.floatValue());
                }

                String raw = element.asString().orElse(null);
                if (raw != null) {
                    return Integer.parseInt(raw);
                }
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    private static String readStringFlexible(CompoundTag nbt, String... keys) {
        if (nbt == null || keys == null) return "";
        for (String key : keys) {
            if (key == null || key.isBlank()) continue;
            String s = nbt.getStringOr(key, "");
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
