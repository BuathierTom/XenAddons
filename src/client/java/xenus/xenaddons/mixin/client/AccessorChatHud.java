package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ChatHud.class)
public interface AccessorChatHud {
    @Accessor("messages")
    List<ChatHudLine> xenaddons$getMessages();

    @Accessor("visibleMessages")
    List<Visible> xenaddons$getVisibleMessages();

    @Accessor("scrolledLines")
    int xenaddons$getScrolledLines();

    @Accessor("scrolledLines")
    void xenaddons$setScrolledLines(int value);
}
