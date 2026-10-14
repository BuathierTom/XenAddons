package xenus.xenaddons.mixin.client;

import net.minecraft.network.chat.TextColor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.compat.XenTextCompat;
import xenus.xenaddons.config.VisualWordsRenderState;

import java.util.ArrayList;
import java.util.List;

@Mixin(ChatScreen.class)
public abstract class MixinChatScreenVisualWords {
    @Shadow
    protected EditBox input;

    @Inject(method = "init", at = @At("TAIL"))
    private void xenaddons$addVisualWordsFormatter(CallbackInfo ci) {
        if (input == null) return;

        input.addFormatter((text, firstCharacterIndex) -> {
            String replaced = XenTextCompat.normalizeLegacyCodes(XenTextCompat.replaceVisualWords(text));
            return toOrderedTextWithLegacyCodes(replaced);
        });
    }

    private FormattedCharSequence toOrderedTextWithLegacyCodes(String input) {
        if (input == null || input.isEmpty()) return FormattedCharSequence.EMPTY;

        List<FormattedCharSequence> parts = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        Style currentStyle = Style.EMPTY;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '§' && i + 1 < input.length()) {
                int rgb = parseLegacyHexColor(input, i);
                if (rgb >= 0) {
                    if (!plain.isEmpty()) {
                        parts.add(FormattedCharSequence.forward(plain.toString(), currentStyle));
                        plain.setLength(0);
                    }
                    currentStyle = Style.EMPTY.withColor(rgb);
                    i += 13;
                    continue;
                }

                if (!plain.isEmpty()) {
                    parts.add(FormattedCharSequence.forward(plain.toString(), currentStyle));
                    plain.setLength(0);
                }

                ChatFormatting fmt = ChatFormatting.getByCode(input.charAt(i + 1));
                if (fmt != null) {
                    if (fmt == ChatFormatting.RESET) {
                        currentStyle = Style.EMPTY;
                    } else if (TextColor.fromLegacyFormat(fmt) != null) {
                        currentStyle = Style.EMPTY.applyFormat(fmt);
                    } else {
                        currentStyle = currentStyle.applyFormat(fmt);
                    }
                }
                i++;
                continue;
            }
            plain.append(c);
        }

        if (!plain.isEmpty()) {
            parts.add(FormattedCharSequence.forward(plain.toString(), currentStyle));
        }
        return FormattedCharSequence.composite(parts);
    }

    private static int parseLegacyHexColor(String input, int start) {
        if (start < 0 || start + 13 >= input.length()) return -1;
        if (input.charAt(start) != '§') return -1;
        if (Character.toLowerCase(input.charAt(start + 1)) != 'x') return -1;

        int[] nibble = new int[6];
        for (int i = 0; i < 6; i++) {
            int sectionIdx = start + 2 + i * 2;
            int digitIdx = sectionIdx + 1;
            if (input.charAt(sectionIdx) != '§') return -1;
            int value = Character.digit(input.charAt(digitIdx), 16);
            if (value < 0) return -1;
            nibble[i] = value;
        }

        return (nibble[0] << 20) | (nibble[1] << 16) | (nibble[2] << 12) | (nibble[3] << 8) | (nibble[4] << 4) | nibble[5];
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void xenaddons$beginChatScreenRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        VisualWordsRenderState.pushChatRender();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void xenaddons$endChatScreenRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        VisualWordsRenderState.popChatRender();
    }
}
