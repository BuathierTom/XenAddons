package xenus.xenaddons.mixin.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.utils.CommandTreeRefresher;

/** Keeps the last command tree around so toggling a dev command can refresh autocomplete at once. */
@Mixin(ClientPacketListener.class)
public class MixinClientPacketListenerCommandTree {

    @Inject(method = "handleCommands", at = @At("TAIL"))
    private void xenaddons$rememberCommandTree(ClientboundCommandsPacket packet, CallbackInfo ci) {
        CommandTreeRefresher.remember(packet);
    }
}
