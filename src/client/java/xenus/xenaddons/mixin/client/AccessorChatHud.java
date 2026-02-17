package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatHud.class)
public interface AccessorChatHud {
    @Invoker("toChatLineX")
    double xenaddons$toChatLineX(double x);

    @Invoker("toChatLineY")
    double xenaddons$toChatLineY(double y);

    @Invoker("getMessageIndex")
    int xenaddons$getMessageIndex(double x, double y);

    @Accessor("messages")
    List<ChatHudLine> xenaddons$getMessages();
}

