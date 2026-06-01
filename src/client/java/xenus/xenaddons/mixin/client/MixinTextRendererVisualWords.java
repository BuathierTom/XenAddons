package xenus.xenaddons.mixin.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xenus.xenaddons.compat.XenTextCompat;

@Mixin(TextRenderer.class)
public class MixinTextRendererVisualWords {

    @ModifyVariable(method = "getWidth(Ljava/lang/String;)I", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private String xenaddons$replaceStringWidth(String text) {
        return XenTextCompat.replacePlainString(text);
    }

    @ModifyVariable(method = "getWidth(Lnet/minecraft/text/StringVisitable;)I", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private StringVisitable xenaddons$replaceVisitableWidth(StringVisitable text) {
        return XenTextCompat.replaceStringVisitable(text);
    }

    @ModifyVariable(method = "getWidth(Lnet/minecraft/text/OrderedText;)I", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private OrderedText xenaddons$replaceOrderedWidth(OrderedText text) {
        return XenTextCompat.replaceOrderedText(text);
    }

    @ModifyVariable(method = "prepare(Ljava/lang/String;FFIZI)Lnet/minecraft/client/font/TextRenderer$GlyphDrawable;", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private String xenaddons$replaceStringPrepare(String text) {
        return XenTextCompat.replacePlainString(text);
    }

    @ModifyVariable(method = "prepare(Lnet/minecraft/text/OrderedText;FFIZZI)Lnet/minecraft/client/font/TextRenderer$GlyphDrawable;", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private OrderedText xenaddons$replaceOrderedPrepare(OrderedText text) {
        return XenTextCompat.replaceOrderedText(text);
    }
}
