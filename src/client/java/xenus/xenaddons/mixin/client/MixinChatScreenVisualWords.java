package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.config.GoatedPlayersManager;

import java.util.ArrayList;
import java.util.List;

@Mixin(ChatScreen.class)
public abstract class MixinChatScreenVisualWords {
    @Shadow
    protected TextFieldWidget chatField;

    @Inject(method = "init", at = @At("TAIL"))
    private void xenaddons$addVisualWordsFormatter(CallbackInfo ci) {
        if (chatField == null) return;

        chatField.addFormatter((text, firstCharacterIndex) -> {
            String replaced = GoatedPlayersManager.normalizeLegacyCodes(GoatedPlayersManager.applyVisualWords(text));
            return toOrderedTextWithLegacyCodes(replaced);
        });
    }

    private OrderedText toOrderedTextWithLegacyCodes(String input) {
        if (input == null || input.isEmpty()) return OrderedText.empty();

        List<OrderedText> parts = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        Style currentStyle = Style.EMPTY;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '§' && i + 1 < input.length()) {
                if (!plain.isEmpty()) {
                    parts.add(OrderedText.styledForwardsVisitedString(plain.toString(), currentStyle));
                    plain.setLength(0);
                }

                Formatting fmt = Formatting.byCode(input.charAt(i + 1));
                if (fmt != null) {
                    if (fmt == Formatting.RESET) {
                        currentStyle = Style.EMPTY;
                    } else if (fmt.isColor()) {
                        currentStyle = Style.EMPTY.withFormatting(fmt);
                    } else {
                        currentStyle = currentStyle.withFormatting(fmt);
                    }
                }
                i++;
                continue;
            }
            plain.append(c);
        }

        if (!plain.isEmpty()) {
            parts.add(OrderedText.styledForwardsVisitedString(plain.toString(), currentStyle));
        }
        return OrderedText.concat(parts);
    }
}
