package xenus.xenaddons.mixin.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.features.impl.fishing.HotspotHighlightModule;
import xenus.xenaddons.features.impl.fishing.HotspotTracker;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListenerHotspotParticles {

    @Inject(method = "handleParticleEvent", at = @At("HEAD"), cancellable = true)
    private void xenaddons$hotspot$onParticleEvent(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        if (packet == null) return;
        boolean isHotspotParticle = HotspotTracker.onParticlePacket(packet);
        if (isHotspotParticle && HotspotHighlightModule.shouldHideParticles()) {
            ci.cancel();
        }
    }
}
