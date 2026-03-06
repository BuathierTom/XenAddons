package xenus.xenaddons.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.regex.Pattern;

@Mixin(Screen.class)
public abstract class MixinScreenDirectChatCommands {
    @Shadow protected MinecraftClient client;

    private static final Pattern DIRECT_COMMAND_PATTERN =
            Pattern.compile("^/?(p kick|block add|pv)\\s+\\w{1,16}$", Pattern.CASE_INSENSITIVE);

    @Inject(method = "handleTextClick", at = @At("HEAD"), cancellable = true)
    private void xenaddons$runPfCommandsDirectly(Style style, CallbackInfoReturnable<Boolean> cir) {
        if (style == null || client == null || client.player == null || client.player.networkHandler == null) return;
        ClickEvent clickEvent = style.getClickEvent();
        if (!(clickEvent instanceof ClickEvent.RunCommand runCommand)) return;

        String command = runCommand.command();
        if (command == null || !DIRECT_COMMAND_PATTERN.matcher(command).matches()) return;

        String toSend = command.startsWith("/") ? command.substring(1) : command;
        client.player.networkHandler.sendChatCommand(toSend);
        cir.setReturnValue(true);
    }
}
