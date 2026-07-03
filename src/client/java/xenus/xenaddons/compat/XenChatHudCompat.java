package xenus.xenaddons.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessage.Line;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import xenus.xenaddons.features.impl.misc.PeekChatController;
import xenus.xenaddons.mixin.client.AccessorChatHud;

import java.util.List;

public final class XenChatHudCompat {
    private XenChatHudCompat() {}

    public static Integer focusedHeightWhilePeeking(Minecraft client) {
        if (!PeekChatController.shouldUseFocusedHeight(client)) return null;
        return ChatComponent.getHeight(client.options.chatHeightFocused().get());
    }

    public static boolean renderPeekChat(Minecraft client, ChatComponent chatHud, GuiGraphicsExtractor context, int ticks) {
        if (!PeekChatController.shouldRenderPeek(client)) return false;

        var window = client.getWindow();
        int mouseX = Mth.floor(client.mouseHandler.getScaledXPos(window));
        int mouseY = Mth.floor(client.mouseHandler.getScaledYPos(window));

        context.nextStratum();
        chatHud.extractRenderState(context, client.font, ticks, mouseX, mouseY, ChatComponent.DisplayMode.BACKGROUND, false);
        return true;
    }

    public static Style getTextStyleAt(Minecraft client, Line visibleLine, double chatX) {
        if (client == null || visibleLine == null || visibleLine.content() == null || chatX < 0.0) return null;

        int targetX = Mth.floor(chatX);
        StringBuilder text = new StringBuilder();
        Style[] styleAtMouse = new Style[1];

        visibleLine.content().accept((index, style, codePoint) -> {
            text.appendCodePoint(codePoint);
            if (client.font.width(text.toString()) > targetX) {
                styleAtMouse[0] = style;
                return false;
            }
            return true;
        });

        return styleAtMouse[0];
    }

    public static double toChatLineX(ChatComponent chatHud, double x) {
        return x / getChatScale(Minecraft.getInstance()) - 4.0;
    }

    public static double toChatLineY(ChatComponent chatHud, double y) {
        Minecraft client = Minecraft.getInstance();
        double bottomOffset = client.getWindow().getGuiScaledHeight() - y - 40.0;
        return bottomOffset / (getChatScale(client) * getLineHeight(client));
    }

    public static int getMessageLineIndex(ChatComponent chatHud, double x, double y) {
        if (!chatHud.isChatFocused() || isChatHidden()) return -1;
        Minecraft client = Minecraft.getInstance();
        if (x < -4.0 || x > Mth.floor(getWidth(client) / getChatScale(client))) return -1;

        List<Line> visibleMessages = getVisibleMessages(chatHud);
        int visibleLineCount = Math.min(chatHud.getLinesPerPage(), visibleMessages.size());
        if (y < 0.0 || y >= visibleLineCount) return -1;

        int lineIndex = Mth.floor(y + getScrolledLines(chatHud));
        if (lineIndex < 0 || lineIndex >= visibleMessages.size()) return -1;
        return lineIndex;
    }

    public static List<GuiMessage> getMessages(ChatComponent chatHud) {
        return ((AccessorChatHud) chatHud).xenaddons$getMessages();
    }

    public static List<Line> getVisibleMessages(ChatComponent chatHud) {
        return ((AccessorChatHud) chatHud).xenaddons$getVisibleMessages();
    }

    public static int getScrolledLines(ChatComponent chatHud) {
        return ((AccessorChatHud) chatHud).xenaddons$getScrolledLines();
    }

    public static void setScrolledLines(ChatComponent chatHud, int value) {
        ((AccessorChatHud) chatHud).xenaddons$setScrolledLines(value);
    }

    private static boolean isChatHidden() {
        return Minecraft.getInstance().options.chatVisibility().get() == ChatVisiblity.HIDDEN;
    }

    private static int getWidth(Minecraft client) {
        return ChatComponent.getWidth(client.options.chatWidth().get());
    }

    private static double getChatScale(Minecraft client) {
        return client.options.chatScale().get();
    }

    private static int getLineHeight(Minecraft client) {
        return (int) (9.0 * (client.options.chatLineSpacing().get() + 1.0));
    }
}
