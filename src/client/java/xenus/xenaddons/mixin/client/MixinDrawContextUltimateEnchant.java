package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.features.impl.dungeons.ShowUltimateEnchantModule;

@Mixin(GuiGraphicsExtractor.class)
public class MixinDrawContextUltimateEnchant {

    @Inject(
        method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
        at = @At("TAIL"),
        require = 0
    )
    private void xenaddons$renderUltimateEnchant(Font textRenderer, ItemStack stack, int x, int y, String countOverride, CallbackInfo ci) {
        ShowUltimateEnchantModule.renderItemLabel((GuiGraphicsExtractor) (Object) this, textRenderer, stack, x, y);
    }
}
