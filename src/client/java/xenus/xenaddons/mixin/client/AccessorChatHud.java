package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;
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

    @Invoker("getMessageLineIndex")
    int xenaddons$getMessageLineIndex(double x, double y);

    @Accessor("messages")
    List<ChatHudLine> xenaddons$getMessages();

    @Accessor("visibleMessages")
    List<Visible> xenaddons$getVisibleMessages();

    @Accessor("scrolledLines")
    int xenaddons$getScrolledLines();

    @Accessor("scrolledLines")
    void xenaddons$setScrolledLines(int value);
}
