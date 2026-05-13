package xenus.xenaddons.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;
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
        chatHud.render(context, ticks, mouseX, mouseY, true);
        return true;
    }

    public static double toChatLineX(ChatHud chatHud, double x) {
        return ((AccessorChatHud) chatHud).xenaddons$toChatLineX(x);
    }

    public static double toChatLineY(ChatHud chatHud, double y) {
        return ((AccessorChatHud) chatHud).xenaddons$toChatLineY(y);
    }

    public static int getMessageLineIndex(ChatHud chatHud, double x, double y) {
        return ((AccessorChatHud) chatHud).xenaddons$getMessageLineIndex(x, y);
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
}
