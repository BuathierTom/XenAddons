package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.features.impl.dungeons.LoadoutTitleModule;

@Mixin(AbstractContainerScreen.class)
public class MixinHandledScreenLoadoutHighlight {

    @Inject(
        method = "extractContents",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractSlots(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V",
            shift = At.Shift.BEFORE
        )
    )
    private void xenaddons$renderLoadoutHighlight(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        LoadoutTitleModule.renderActiveLoadoutHighlight((AbstractContainerScreen<?>) (Object) this, context);
    }
}
