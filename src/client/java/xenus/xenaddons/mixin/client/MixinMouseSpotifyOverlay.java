package xenus.xenaddons.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.compat.XenMouseCompat;

@Mixin(Mouse.class)
public abstract class MixinMouseSpotifyOverlay {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void xenaddons$clickSpotifyOverlayControls(long window, MouseInput input, int action, CallbackInfo ci) {
        if (XenMouseCompat.handleSpotifyOverlayClick(client, window, input, action)) {
            ci.cancel();
        }
    }
}
