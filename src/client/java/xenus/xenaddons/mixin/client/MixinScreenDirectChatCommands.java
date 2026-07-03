package xenus.xenaddons.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Pattern;

@Mixin(Screen.class)
public abstract class MixinScreenDirectChatCommands {
    private static final Pattern DIRECT_COMMAND_PATTERN =
            Pattern.compile("^/?(p kick|block add|pv)\\s+\\w{1,16}$", Pattern.CASE_INSENSITIVE);

    @Inject(method = "clickCommandAction", at = @At("HEAD"), cancellable = true)
    private static void xenaddons$runPfCommandsDirectly(LocalPlayer player, String command, Screen screen, CallbackInfo ci) {
        if (player == null || player.connection == null) return;
        if (command == null || !DIRECT_COMMAND_PATTERN.matcher(command).matches()) return;

        String toSend = command.startsWith("/") ? command.substring(1) : command;
        player.connection.sendCommand(toSend);
        ci.cancel();
    }
}
