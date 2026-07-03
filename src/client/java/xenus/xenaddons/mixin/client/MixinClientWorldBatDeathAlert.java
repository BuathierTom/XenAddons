package xenus.xenaddons.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ambient.Bat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.features.impl.dungeons.BatDeathAlertModule;

@Mixin(ClientLevel.class)
public class MixinClientWorldBatDeathAlert {

    @Inject(method = "removeEntity", at = @At("HEAD"))
    private void xenaddons$batDeathAlert$onRemoveEntity(int entityId, Entity.RemovalReason removalReason, CallbackInfo ci) {
        if (removalReason != Entity.RemovalReason.KILLED) return;
        ClientLevel world = (ClientLevel) (Object) this;
        Entity entity = world.getEntity(entityId);
        if (entity instanceof Bat bat) {
            BatDeathAlertModule.onBatDeath(bat);
        }
    }
}

