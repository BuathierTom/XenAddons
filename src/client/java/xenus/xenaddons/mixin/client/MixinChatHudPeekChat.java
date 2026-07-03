package xenus.xenaddons.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xenus.xenaddons.compat.XenChatHudCompat;

@Mixin(ChatComponent.class)
public abstract class MixinChatHudPeekChat {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true)
    private void xenaddons$useFocusedHeightWhilePeeking(CallbackInfoReturnable<Integer> cir) {
        Integer height = XenChatHudCompat.focusedHeightWhilePeeking(minecraft);
        if (height != null) {
            cir.setReturnValue(height);
        }
    }
}
