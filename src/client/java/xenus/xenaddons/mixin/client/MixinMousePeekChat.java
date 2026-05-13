package xenus.xenaddons.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.compat.XenMouseCompat;

@Mixin(Mouse.class)
public abstract class MixinMousePeekChat {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void xenaddons$scrollPeekChat(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (XenMouseCompat.handlePeekChatScroll(client, window, vertical)) {
            ci.cancel();
        }
    }
}
