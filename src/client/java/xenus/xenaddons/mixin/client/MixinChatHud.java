package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.config.GoatedPlayersManager;
import xenus.xenaddons.config.VisualWordsChatStore;
import xenus.xenaddons.config.VisualWordsRenderState;
import xenus.xenaddons.features.impl.misc.CompactPotionMessageProcessor;

@Mixin(net.minecraft.client.gui.hud.ChatHud.class)
public class MixinChatHud {
    @Unique
    private static final ThreadLocal<Text> XENADDONS_ORIGINAL_MESSAGE = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<Text> XENADDONS_REPLACED_MESSAGE = new ThreadLocal<>();

    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/text/Text;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private Text xenaddons$replaceChatTextSimple(Text text) {
        Text compacted = CompactPotionMessageProcessor.compact(text);
        Text replaced = GoatedPlayersManager.applyVisualWords(compacted);
        XENADDONS_ORIGINAL_MESSAGE.set(text);
        XENADDONS_REPLACED_MESSAGE.set(replaced);
        VisualWordsChatStore.put(replaced, text);
        return replaced;
    }

    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private Text xenaddons$replaceChatTextAdvanced(Text text) {
        Text pendingOriginal = XENADDONS_ORIGINAL_MESSAGE.get();
        Text pendingReplaced = XENADDONS_REPLACED_MESSAGE.get();
        if (pendingOriginal != null && pendingReplaced != null && pendingReplaced.equals(text)) {
            VisualWordsChatStore.put(text, pendingOriginal);
            return text;
        }

        Text compacted = CompactPotionMessageProcessor.compact(text);
        Text replaced = GoatedPlayersManager.applyVisualWords(compacted);
        XENADDONS_ORIGINAL_MESSAGE.set(text);
        XENADDONS_REPLACED_MESSAGE.set(replaced);
        VisualWordsChatStore.put(replaced, text);
        return replaced;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void xenaddons$beginChatRender(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        VisualWordsRenderState.pushChatRender();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void xenaddons$endChatRender(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        XENADDONS_ORIGINAL_MESSAGE.remove();
        XENADDONS_REPLACED_MESSAGE.remove();
        VisualWordsRenderState.popChatRender();
    }
}
