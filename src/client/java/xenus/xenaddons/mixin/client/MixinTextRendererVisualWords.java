package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xenus.xenaddons.compat.XenTextCompat;

@Mixin(Font.class)
public class MixinTextRendererVisualWords {

    @ModifyVariable(method = "getWidth(Ljava/lang/String;)I", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private String xenaddons$replaceStringWidth(String text) {
        return XenTextCompat.replacePlainString(text);
    }

    @ModifyVariable(method = "getWidth(Lnet/minecraft/network/chat/FormattedText;)I", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private FormattedText xenaddons$replaceVisitableWidth(FormattedText text) {
        return XenTextCompat.replaceStringVisitable(text);
    }

    @ModifyVariable(method = "getWidth(Lnet/minecraft/util/FormattedCharSequence;)I", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private FormattedCharSequence xenaddons$replaceOrderedWidth(FormattedCharSequence text) {
        return XenTextCompat.replaceOrderedText(text);
    }

    @ModifyVariable(method = "prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private String xenaddons$replaceStringPrepare(String text) {
        return XenTextCompat.replacePlainString(text);
    }

    @ModifyVariable(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private FormattedCharSequence xenaddons$replaceOrderedPrepare(FormattedCharSequence text) {
        return XenTextCompat.replaceOrderedText(text);
    }
}
