package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.features.impl.misc.PetLevelModule;

@Mixin(GuiGraphicsExtractor.class)
public class MixinDrawContextPetLevel {

    // The 4 argument overload delegates here, so this single hook covers every item slot.
    @Inject(
        method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
        at = @At("TAIL"),
        require = 0
    )
    private void xenaddons$renderPetLevel(Font textRenderer, ItemStack stack, int x, int y, String countOverride, CallbackInfo ci) {
        PetLevelModule.renderPetLevel((GuiGraphicsExtractor) (Object) this, textRenderer, stack, x, y);
    }
}
