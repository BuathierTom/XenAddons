package xenus.xenaddons.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xenus.xenaddons.features.impl.misc.PeekChatController;

@Mixin(ChatHud.class)
public abstract class MixinChatHudPeekChat {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true)
    private void xenaddons$useFocusedHeightWhilePeeking(CallbackInfoReturnable<Integer> cir) {
        if (!PeekChatController.shouldUseFocusedHeight(client)) return;
        cir.setReturnValue(ChatHud.getHeight(client.options.getChatHeightFocused().getValue()));
    }
}
