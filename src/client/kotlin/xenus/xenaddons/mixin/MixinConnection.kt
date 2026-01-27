package xenus.xenaddons.mixin

import xenus.xenaddons.api.events.EventBus
import net.minecraft.network.Connection
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.PacketFlow
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(Connection::class)
abstract class MixinConnection {
    @Shadow
    abstract fun getReceiving(): PacketFlow

    @Inject(method = ["channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V"], at = [At("HEAD")], cancellable = true)
    private fun onPacketReceive(context: Any, packet: Packet<*>, ci: CallbackInfo) {
        if (getReceiving() == PacketFlow.CLIENTBOUND) {
            try {
                EventBus.onPacketReceived(packet)
            } catch (e: Exception) {
                // e.printStackTrace()
            }
        }
    }
    
    @Inject(method = ["send(Lnet/minecraft/network/protocol/Packet;)V"], at = [At("HEAD")])
    private fun onPacketSend(packet: Packet<*>, ci: CallbackInfo) {
         if (getReceiving() == PacketFlow.CLIENTBOUND) { // If receiving is CLIENTBOUND, then we are the client sending to server (mostly)
             EventBus.onPacketSent(packet)
         }
    }
}
