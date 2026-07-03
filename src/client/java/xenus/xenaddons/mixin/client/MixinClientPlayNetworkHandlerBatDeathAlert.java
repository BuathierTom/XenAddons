package xenus.xenaddons.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.features.impl.dungeons.BatDeathAlertModule;

@Mixin(ClientPacketListener.class)
public class MixinClientPlayNetworkHandlerBatDeathAlert {

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void xenaddons$batDeathAlert$onEntityStatus(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (packet == null) return;
        if (packet.getEventId() != 3) return; // 3 = living entity death status

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) return;

        Entity entity = packet.getEntity(client.level);
        if (entity instanceof Bat bat) {
            BatDeathAlertModule.onBatDeath(bat);
        }
    }
}
