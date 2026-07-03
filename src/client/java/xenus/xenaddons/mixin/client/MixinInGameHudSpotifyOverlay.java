package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.compat.XenHudCompat;

@Mixin(Gui.class)
public class MixinInGameHudSpotifyOverlay {

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void xenaddons$renderSpotifyBeforeHud(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        XenHudCompat.renderEarlyHudOverlays(context);
    }
}
