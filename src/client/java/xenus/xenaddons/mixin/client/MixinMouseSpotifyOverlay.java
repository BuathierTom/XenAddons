package xenus.xenaddons.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.compat.XenMouseCompat;

@Mixin(MouseHandler.class)
public abstract class MixinMouseSpotifyOverlay {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void xenaddons$clickSpotifyOverlayControls(long window, MouseButtonInfo input, int action, CallbackInfo ci) {
        if (XenMouseCompat.handleSpotifyOverlayClick(minecraft, window, input, action)) {
            ci.cancel();
        }
    }
}
