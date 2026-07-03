package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.network.chat.Component;
import xenus.xenaddons.compat.XenHudCompat;

@Mixin(Gui.class)
public abstract class MixinInGameHud {
    
    @Shadow private Component subtitle;

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractTitle(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void onBeforeRenderTitle(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        XenHudCompat.pushAutoPetTitleScale(context, subtitle);
    }

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractTitle(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.AFTER))
    private void onAfterRenderTitle(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        XenHudCompat.popAutoPetTitleScale(context, subtitle);
    }
}
