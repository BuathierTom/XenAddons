package xenus.xenaddons.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.compat.XenMouseCompat;

@Mixin(MouseHandler.class)
public abstract class MixinMousePeekChat {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void xenaddons$scrollPeekChat(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (XenMouseCompat.handlePeekChatScroll(minecraft, window, vertical)) {
            ci.cancel();
        }
    }
}
