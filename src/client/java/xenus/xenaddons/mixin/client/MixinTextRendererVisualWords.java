package xenus.xenaddons.mixin.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xenus.xenaddons.config.GoatedPlayersManager;

@Mixin(TextRenderer.class)
public class MixinTextRendererVisualWords {

    @ModifyVariable(method = "getWidth(Ljava/lang/String;)I", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private String xenaddons$replaceStringWidth(String text) {
        if (text == null || text.indexOf('§') >= 0 || text.indexOf('&') >= 0) return text;
        return GoatedPlayersManager.applyVisualWordsPlain(text);
    }

    @ModifyVariable(method = "getWidth(Lnet/minecraft/text/StringVisitable;)I", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private StringVisitable xenaddons$replaceVisitableWidth(StringVisitable text) {
        if (text instanceof Text t) return GoatedPlayersManager.applyVisualWords(t);
        return text;
    }

    @ModifyVariable(method = "getWidth(Lnet/minecraft/text/OrderedText;)I", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private OrderedText xenaddons$replaceOrderedWidth(OrderedText text) {
        return GoatedPlayersManager.applyVisualWords(text);
    }

    @ModifyVariable(method = "prepare(Ljava/lang/String;FFIZI)Lnet/minecraft/client/font/TextRenderer$GlyphDrawable;", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private String xenaddons$replaceStringPrepare(String text) {
        if (text == null || text.indexOf('§') >= 0 || text.indexOf('&') >= 0) return text;
        return GoatedPlayersManager.applyVisualWordsPlain(text);
    }

    @ModifyVariable(method = "prepare(Lnet/minecraft/text/OrderedText;FFIZI)Lnet/minecraft/client/font/TextRenderer$GlyphDrawable;", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private OrderedText xenaddons$replaceOrderedPrepare(OrderedText text) {
        return GoatedPlayersManager.applyVisualWords(text);
    }
}
