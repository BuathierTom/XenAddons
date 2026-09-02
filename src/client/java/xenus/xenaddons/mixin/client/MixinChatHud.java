package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.compat.XenTextCompat;
import xenus.xenaddons.config.VisualWordsChatStore;
import xenus.xenaddons.config.VisualWordsRenderState;
import xenus.xenaddons.features.impl.misc.CompactPotionMessageProcessor;
import xenus.xenaddons.features.impl.misc.PrivateMessageProcessor;

@Mixin(net.minecraft.client.gui.components.ChatComponent.class)
public class MixinChatHud {
    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private Component xenaddons$replaceChatText(Component text) {
        Component compacted = CompactPotionMessageProcessor.compact(text);
        Component formatted = PrivateMessageProcessor.format(compacted);
        Component replaced = XenTextCompat.replaceText(formatted);
        VisualWordsChatStore.put(replaced, text);
        return replaced;
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void xenaddons$beginChatRender(GuiGraphicsExtractor context, Font textRenderer, int currentTick, int mouseX, int mouseY, ChatComponent.DisplayMode displayMode, boolean refreshIndicator, CallbackInfo ci) {
        VisualWordsRenderState.pushChatRender();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void xenaddons$endChatRender(GuiGraphicsExtractor context, Font textRenderer, int currentTick, int mouseX, int mouseY, ChatComponent.DisplayMode displayMode, boolean refreshIndicator, CallbackInfo ci) {
        VisualWordsRenderState.popChatRender();
    }
}
