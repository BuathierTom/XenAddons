package xenus.xenaddons.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.compat.XenChatHudCompat;

@Mixin(Gui.class)
public abstract class MixinInGameHudPeekChat {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private ChatComponent chat;
    @Shadow private int tickCount;

    @Inject(method = "extractChat", at = @At("HEAD"), cancellable = true)
    private void xenaddons$renderPeekChat(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (XenChatHudCompat.renderPeekChat(minecraft, chat, context, tickCount)) {
            ci.cancel();
        }
    }
}
