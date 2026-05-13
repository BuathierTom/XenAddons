package xenus.xenaddons.compat;

import net.minecraft.item.ItemStack;
import xenus.xenaddons.features.impl.misc.CooldownItemsTracker;
import xenus.xenaddons.features.impl.misc.CustomHyperionSoundHandler;

public final class XenInteractionCompat {
    private XenInteractionCompat() {}

    public static void onItemRightClick(ItemStack stack) {
        CooldownItemsTracker.onItemRightClick(stack);
        CustomHyperionSoundHandler.onItemUse(stack);
    }

    public static void onItemLeftClick(ItemStack stack) {
        CooldownItemsTracker.onItemLeftClick(stack);
    }
}
