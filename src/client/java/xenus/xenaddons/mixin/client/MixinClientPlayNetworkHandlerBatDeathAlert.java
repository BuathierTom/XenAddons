package xenus.xenaddons.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.features.impl.dungeons.BatDeathAlertModule;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandlerBatDeathAlert {

    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void xenaddons$batDeathAlert$onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (packet == null) return;
        if (packet.getStatus() != 3) return; // 3 = living entity death status

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) return;

        Entity entity = packet.getEntity(client.world);
        if (entity instanceof BatEntity bat) {
            BatDeathAlertModule.onBatDeath(bat);
        }
    }
}

