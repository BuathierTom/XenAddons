package xenus.xenaddons.mixin.client;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.BatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.features.impl.dungeons.BatDeathAlertModule;

@Mixin(ClientWorld.class)
public class MixinClientWorldBatDeathAlert {

    @Inject(method = "removeEntity", at = @At("HEAD"))
    private void xenaddons$batDeathAlert$onRemoveEntity(int entityId, Entity.RemovalReason removalReason, CallbackInfo ci) {
        if (removalReason != Entity.RemovalReason.KILLED) return;
        ClientWorld world = (ClientWorld) (Object) this;
        Entity entity = world.getEntityById(entityId);
        if (entity instanceof BatEntity bat) {
            BatDeathAlertModule.onBatDeath(bat);
        }
    }
}

