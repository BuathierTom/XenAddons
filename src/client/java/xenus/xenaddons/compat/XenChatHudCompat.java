package xenus.xenaddons.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;
import net.minecraft.network.message.ChatVisibility;
import net.minecraft.text.Style;
import net.minecraft.util.math.MathHelper;
import xenus.xenaddons.features.impl.misc.PeekChatController;
import xenus.xenaddons.mixin.client.AccessorChatHud;

import java.util.List;

public final class XenChatHudCompat {
    private XenChatHudCompat() {}

    public static Integer focusedHeightWhilePeeking(MinecraftClient client) {
        if (!PeekChatController.shouldUseFocusedHeight(client)) return null;
        return ChatHud.getHeight(client.options.getChatHeightFocused().getValue());
    }

    public static boolean renderPeekChat(MinecraftClient client, ChatHud chatHud, DrawContext context, int ticks) {
        if (!PeekChatController.shouldRenderPeek(client)) return false;

        var window = client.getWindow();
        int mouseX = MathHelper.floor(client.mouse.getScaledX(window));
        int mouseY = MathHelper.floor(client.mouse.getScaledY(window));

        context.createNewRootLayer();
        chatHud.render(context, client.textRenderer, ticks, mouseX, mouseY, true, false);
        return true;
    }

    public static Style getTextStyleAt(MinecraftClient client, Visible visibleLine, double chatX) {
        if (client == null || visibleLine == null || visibleLine.content() == null || chatX < 0.0) return null;

        int targetX = MathHelper.floor(chatX);
        StringBuilder text = new StringBuilder();
        Style[] styleAtMouse = new Style[1];

        visibleLine.content().accept((index, style, codePoint) -> {
            text.appendCodePoint(codePoint);
            if (client.textRenderer.getWidth(text.toString()) > targetX) {
                styleAtMouse[0] = style;
                return false;
            }
            return true;
        });

        return styleAtMouse[0];
    }

    public static double toChatLineX(ChatHud chatHud, double x) {
        return x / getChatScale(MinecraftClient.getInstance()) - 4.0;
    }

    public static double toChatLineY(ChatHud chatHud, double y) {
        MinecraftClient client = MinecraftClient.getInstance();
        double bottomOffset = client.getWindow().getScaledHeight() - y - 40.0;
        return bottomOffset / (getChatScale(client) * getLineHeight(client));
    }

    public static int getMessageLineIndex(ChatHud chatHud, double x, double y) {
        if (!chatHud.isChatFocused() || isChatHidden()) return -1;
        MinecraftClient client = MinecraftClient.getInstance();
        if (x < -4.0 || x > MathHelper.floor(getWidth(client) / getChatScale(client))) return -1;

        List<Visible> visibleMessages = getVisibleMessages(chatHud);
        int visibleLineCount = Math.min(chatHud.getVisibleLineCount(), visibleMessages.size());
        if (y < 0.0 || y >= visibleLineCount) return -1;

        int lineIndex = MathHelper.floor(y + getScrolledLines(chatHud));
        if (lineIndex < 0 || lineIndex >= visibleMessages.size()) return -1;
        return lineIndex;
    }

    public static List<ChatHudLine> getMessages(ChatHud chatHud) {
        return ((AccessorChatHud) chatHud).xenaddons$getMessages();
    }

    public static List<Visible> getVisibleMessages(ChatHud chatHud) {
        return ((AccessorChatHud) chatHud).xenaddons$getVisibleMessages();
    }

    public static int getScrolledLines(ChatHud chatHud) {
        return ((AccessorChatHud) chatHud).xenaddons$getScrolledLines();
    }

    public static void setScrolledLines(ChatHud chatHud, int value) {
        ((AccessorChatHud) chatHud).xenaddons$setScrolledLines(value);
    }

    private static boolean isChatHidden() {
        return MinecraftClient.getInstance().options.getChatVisibility().getValue() == ChatVisibility.HIDDEN;
    }

    private static int getWidth(MinecraftClient client) {
        return ChatHud.getWidth(client.options.getChatWidth().getValue());
    }

    private static double getChatScale(MinecraftClient client) {
        return client.options.getChatScale().getValue();
    }

    private static int getLineHeight(MinecraftClient client) {
        return (int) (9.0 * (client.options.getChatLineSpacing().getValue() + 1.0));
    }
}
