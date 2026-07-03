package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessage.Line;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ChatComponent.class)
public interface AccessorChatHud {
    @Accessor("allMessages")
    List<GuiMessage> xenaddons$getMessages();

    @Accessor("trimmedMessages")
    List<Line> xenaddons$getVisibleMessages();

    @Accessor("chatScrollbarPos")
    int xenaddons$getScrolledLines();

    @Accessor("chatScrollbarPos")
    void xenaddons$setScrolledLines(int value);
}
