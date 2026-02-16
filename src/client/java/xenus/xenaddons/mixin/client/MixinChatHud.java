package xenus.xenaddons.mixin.client;

import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xenus.xenaddons.config.GoatedPlayersManager;

@Mixin(net.minecraft.client.gui.hud.ChatHud.class)
public class MixinChatHud {

    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/text/Text;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private Text xenaddons$replaceChatTextSimple(Text text) {
        return GoatedPlayersManager.applyVisualWords(text);
    }

    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private Text xenaddons$replaceChatTextAdvanced(Text text) {
        return GoatedPlayersManager.applyVisualWords(text);
    }
}
