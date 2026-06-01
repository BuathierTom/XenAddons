package xenus.xenaddons.mixin.client;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Pattern;

@Mixin(Screen.class)
public abstract class MixinScreenDirectChatCommands {
    private static final Pattern DIRECT_COMMAND_PATTERN =
            Pattern.compile("^/?(p kick|block add|pv)\\s+\\w{1,16}$", Pattern.CASE_INSENSITIVE);

    @Inject(method = "handleRunCommand", at = @At("HEAD"), cancellable = true)
    private static void xenaddons$runPfCommandsDirectly(ClientPlayerEntity player, String command, Screen screen, CallbackInfo ci) {
        if (player == null || player.networkHandler == null) return;
        if (command == null || !DIRECT_COMMAND_PATTERN.matcher(command).matches()) return;

        String toSend = command.startsWith("/") ? command.substring(1) : command;
        player.networkHandler.sendChatCommand(toSend);
        ci.cancel();
    }
}
