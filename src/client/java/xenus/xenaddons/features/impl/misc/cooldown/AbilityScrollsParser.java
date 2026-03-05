package xenus.xenaddons.features.impl.misc.cooldown;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.HashSet;
import java.util.Set;

final class AbilityScrollsParser {
    private AbilityScrollsParser() {
    }

    static Set<ItemAbility> getAllAbilityScrolls(ItemStack stack) {
        Set<ItemAbility> out = new HashSet<>();
        if (stack == null || stack.isEmpty()) return out;

        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) return out;
        NbtCompound root = customData.copyNbt();
        if (root == null) return out;
        NbtCompound extra = root.getCompound("ExtraAttributes").orElse(root);
        NbtList scrolls = extra.getList("ability_scroll").orElseGet(() -> extra.getList("ABILITY_SCROLL").orElse(null));
        if (scrolls == null || scrolls.isEmpty()) return out;

        for (NbtElement element : scrolls) {
            String token = element.asString().orElse("").toUpperCase(java.util.Locale.ROOT);
            switch (token) {
                case "WITHER_SHIELD" -> {
                    out.add(ItemAbility.WITHER_SHIELD_SCROLL);
                }
                case "SHADOW_WARP" -> {
                    out.add(ItemAbility.SHADOW_WARP_SCROLL);
                }
                case "IMPLOSION" -> {
                    out.add(ItemAbility.IMPLOSION_SCROLL);
                }
                default -> {
                    // ignore unknown scroll values
                }
            }
        }

        if (out.contains(ItemAbility.WITHER_SHIELD_SCROLL) &&
            out.contains(ItemAbility.SHADOW_WARP_SCROLL) &&
            out.contains(ItemAbility.IMPLOSION_SCROLL)) {
            out.clear();
            out.add(ItemAbility.WITHER_IMPACT);
        }
        return out;
    }
}
