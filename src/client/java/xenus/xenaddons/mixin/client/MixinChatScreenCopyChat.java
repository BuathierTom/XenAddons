package xenus.xenaddons.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.ClickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.lwjgl.glfw.GLFW;
import xenus.xenaddons.config.VisualWordsChatStore;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.features.impl.misc.CopyChatModule;
import xenus.xenaddons.utils.ChatUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;

@Mixin(ChatScreen.class)
public class MixinChatScreenCopyChat {
    private static final Pattern LEGACY_COLOR_CODE = Pattern.compile("(?i)[§&][0-9A-FK-OR]");
    private static final Pattern XEN_KICK_PROMPT = Pattern.compile("^\\[XenAddons] Press to kick (\\w{1,16})$");
    private static final Pattern DIRECT_COMMAND_PATTERN = Pattern.compile("^/(p kick|block add|pv)\\s+\\w{1,16}$");
    private static final Map<Integer, Character> LEGACY_COLOR_BY_RGB = new HashMap<>();
    static {
        LEGACY_COLOR_BY_RGB.put(0x000000, '0');
        LEGACY_COLOR_BY_RGB.put(0x0000AA, '1');
        LEGACY_COLOR_BY_RGB.put(0x00AA00, '2');
        LEGACY_COLOR_BY_RGB.put(0x00AAAA, '3');
        LEGACY_COLOR_BY_RGB.put(0xAA0000, '4');
        LEGACY_COLOR_BY_RGB.put(0xAA00AA, '5');
        LEGACY_COLOR_BY_RGB.put(0xFFAA00, '6');
        LEGACY_COLOR_BY_RGB.put(0xAAAAAA, '7');
        LEGACY_COLOR_BY_RGB.put(0x555555, '8');
        LEGACY_COLOR_BY_RGB.put(0x5555FF, '9');
        LEGACY_COLOR_BY_RGB.put(0x55FF55, 'a');
        LEGACY_COLOR_BY_RGB.put(0x55FFFF, 'b');
        LEGACY_COLOR_BY_RGB.put(0xFF5555, 'c');
        LEGACY_COLOR_BY_RGB.put(0xFF55FF, 'd');
        LEGACY_COLOR_BY_RGB.put(0xFFFF55, 'e');
        LEGACY_COLOR_BY_RGB.put(0xFFFFFF, 'f');
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void xenaddons$copyChatOnRightClick(Click click, boolean bl, CallbackInfoReturnable<Boolean> cir) {
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

        if (click.button() == 0 && mc.player != null && mc.player.networkHandler != null) {
            Style clickedStyle = chatHud.getTextStyleAt(chatX, chatY);
            ClickEvent clickEvent = clickedStyle == null ? null : clickedStyle.getClickEvent();
            if (clickEvent instanceof ClickEvent.RunCommand runCommand) {
                String cmd = runCommand.command();
                if (cmd != null && DIRECT_COMMAND_PATTERN.matcher(cmd).matches()) {
                    String toSend = cmd.startsWith("/") ? cmd.substring(1) : cmd;
                    mc.player.networkHandler.sendChatCommand(toSend);
                    cir.setReturnValue(true);
                    return;
                }
            }

            String plain = stripCodes(line.content().getString()).trim();
            java.util.regex.Matcher kickMatcher = XEN_KICK_PROMPT.matcher(plain);
            if (kickMatcher.matches()) {
                String target = kickMatcher.group(1);
                mc.player.networkHandler.sendChatCommand("p kick " + target);
                cir.setReturnValue(true);
                return;
            }
        }

        if (click.button() != 1) return;

        CopyChatModule module = ModuleManager.getModule(CopyChatModule.class);
        if (module == null || !module.isEnabled()) return;
        if (module.isRequireCtrlClickEnabled() && !isCtrlDown(mc)) return;

        Text original = VisualWordsChatStore.getOriginalOrNull(line.content());
        Text source = (original != null ? original : line.content());

        boolean altDown = isAltOrOptionDown(mc);
        boolean formattedCopy = altDown;
        String copied = formattedCopy
                ? toLegacyFormatted(source)
                : LEGACY_COLOR_CODE.matcher(source.getString()).replaceAll("");
        if (copied.isBlank()) return;

        mc.keyboard.setClipboard(copied);
        ChatUtils.success(formattedCopy ? "Copied formatted message to clipboard" : "Copied message to clipboard");
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

    private boolean isAltOrOptionDown(MinecraftClient mc) {
        if (mc == null || mc.getWindow() == null) return false;
        long handle = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
    }

    private boolean isCtrlDown(MinecraftClient mc) {
        if (mc == null || mc.getWindow() == null) return false;
        long handle = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    private String toLegacyFormatted(Text text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        final Style[] lastStyle = new Style[]{null};
        OrderedText ordered = text.asOrderedText();
        ordered.accept((index, style, codePoint) -> {
            if (!style.equals(lastStyle[0])) {
                sb.append(styleToLegacyPrefix(style));
                lastStyle[0] = style;
            }
            sb.appendCodePoint(codePoint);
            return true;
        });
        return sb.toString();
    }

    private String styleToLegacyPrefix(Style style) {
        if (style == null) return "§r";
        StringBuilder sb = new StringBuilder("§r");
        if (style.getColor() != null) {
            Integer rgb = style.getColor().getRgb();
            if (rgb != null) {
                Character code = LEGACY_COLOR_BY_RGB.get(rgb & 0xFFFFFF);
                if (code != null) sb.append('§').append(code);
            }
        }
        if (Boolean.TRUE.equals(style.isObfuscated())) sb.append("§k");
        if (Boolean.TRUE.equals(style.isBold())) sb.append("§l");
        if (Boolean.TRUE.equals(style.isStrikethrough())) sb.append("§m");
        if (Boolean.TRUE.equals(style.isUnderlined())) sb.append("§n");
        if (Boolean.TRUE.equals(style.isItalic())) sb.append("§o");
        return sb.toString();
    }
}
