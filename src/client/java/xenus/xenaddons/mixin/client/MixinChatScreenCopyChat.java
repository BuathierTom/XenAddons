package xenus.xenaddons.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xenus.xenaddons.config.VisualWordsChatStore;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.features.impl.misc.CopyChatModule;
import xenus.xenaddons.utils.ChatUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

@Mixin(ChatScreen.class)
public class MixinChatScreenCopyChat {
    private static final Pattern LEGACY_COLOR_CODE = Pattern.compile("(?i)[§&][0-9A-FK-OR]");

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void xenaddons$copyChatOnRightClick(Click click, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        if (click.button() != 1) return;

        CopyChatModule module = ModuleManager.getModule(CopyChatModule.class);
        if (module == null || !module.isEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.inGameHud == null) return;

        ChatHud chatHud = mc.inGameHud.getChatHud();
        AccessorChatHud accessor = (AccessorChatHud) chatHud;

        double chatX = accessor.xenaddons$toChatLineX(click.x());
        double chatY = accessor.xenaddons$toChatLineY(click.y());
        int lineIndex = accessor.xenaddons$getMessageLineIndex(chatX, chatY);
        if (lineIndex < 0) return;

        List<Visible> visibleLines = accessor.xenaddons$getVisibleMessages();
        if (lineIndex >= visibleLines.size()) return;
        Visible visibleLine = visibleLines.get(lineIndex);
        if (visibleLine == null) return;

        ChatHudLine line = resolveClickedMessage(accessor.xenaddons$getMessages(), visibleLine);
        if (line == null || line.content() == null) return;

        Text original = VisualWordsChatStore.getOriginalOrNull(line.content());
        String copied = (original != null ? original : line.content()).getString();
        copied = LEGACY_COLOR_CODE.matcher(copied).replaceAll("");
        if (copied.isBlank()) return;

        mc.keyboard.setClipboard(copied);
        ChatUtils.success("Copied message to clipboard");
    }

    private ChatHudLine resolveClickedMessage(List<ChatHudLine> allMessages, Visible visibleLine) {
        if (allMessages == null || allMessages.isEmpty()) return null;

        int addedTime = visibleLine.addedTime();
        List<ChatHudLine> matches = new ArrayList<>();
        for (ChatHudLine msg : allMessages) {
            if (msg == null || msg.content() == null) continue;
            if (msg.creationTick() == addedTime && !msg.content().getString().isBlank()) {
                matches.add(msg);
            }
        }

        if (matches.isEmpty()) return null;
        if (matches.size() == 1) return matches.get(0);

        String visibleText = stripCodes(orderedTextToString(visibleLine.content()));
        for (ChatHudLine candidate : matches) {
            String candidateText = stripCodes(candidate.content().getString());
            if (!visibleText.isBlank() && candidateText.contains(visibleText)) {
                return candidate;
            }
        }
        return matches.get(0);
    }

    private String orderedTextToString(OrderedText text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return sb.toString();
    }

    private String stripCodes(String input) {
        if (input == null) return "";
        return LEGACY_COLOR_CODE.matcher(input).replaceAll("");
    }
}
