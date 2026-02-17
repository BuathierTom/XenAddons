package xenus.xenaddons.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xenus.xenaddons.config.VisualWordsChatStore;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.features.impl.misc.CopyChatModule;
import xenus.xenaddons.utils.ChatUtils;

import java.util.List;

@Mixin(ChatScreen.class)
public class MixinChatScreenCopyChat {

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
        int messageIndex = accessor.xenaddons$getMessageIndex(chatX, chatY);
        if (messageIndex < 0) return;

        List<ChatHudLine> messages = accessor.xenaddons$getMessages();
        if (messageIndex >= messages.size()) return;

        ChatHudLine line = messages.get(messageIndex);
        if (line == null || line.content() == null) return;

        Text original = VisualWordsChatStore.getOriginalOrNull(line.content());
        String copied = (original != null ? original : line.content()).getString();
        if (copied.isBlank()) return;

        mc.keyboard.setClipboard(copied);
        ChatUtils.success("Copied message to clipboard");
    }
}
