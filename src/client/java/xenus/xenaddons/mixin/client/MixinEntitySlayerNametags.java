package xenus.xenaddons.mixin.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.utils.slayer.SlayerEntityDuck;
import xenus.xenaddons.utils.slayer.SlayerNametags;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Gives every entity a link to the nametag armor stands floating above it. Ported from Athen's
 * EntityMixin (BSD 3-Clause, Copyright (c) 2025-2026 Starred), itself based on SkyblockAPI (MIT).
 * Used with the author's permission.
 */
@Mixin(Entity.class)
public abstract class MixinEntitySlayerNametags implements SlayerEntityDuck {

    @Shadow
    @Final
    private static EntityDataAccessor<Optional<Component>> DATA_CUSTOM_NAME;

    @Shadow
    public abstract Component getCustomName();

    @Unique
    private List<WeakReference<Entity>> xenaddons$slayerAttachments;

    @Unique
    private Entity xenaddons$slayerAttachedTo;

    // Staggered so the periodic re-check costs almost nothing across a world full of entities.
    @Unique
    private int xenaddons$slayerAttachCooldown = 0;

    @Unique
    private Entity xenaddons$self() {
        return (Entity) (Object) this;
    }

    @Override
    public List<WeakReference<Entity>> xenaddons$attachments() {
        if (xenaddons$slayerAttachments == null) xenaddons$slayerAttachments = new ArrayList<>();
        return xenaddons$slayerAttachments;
    }

    @Override
    public Entity xenaddons$attachedTo() {
        return xenaddons$slayerAttachedTo;
    }

    @Override
    public void xenaddons$attachedTo(Entity entity) {
        xenaddons$slayerAttachedTo = entity;
    }

    @Inject(method = "setCustomName", at = @At("RETURN"))
    private void xenaddons$slayer$setCustomName(Component name, CallbackInfo ci) {
        if (name == null) return;
        if (!(xenaddons$self() instanceof ArmorStand)) return;
        SlayerNametags.attach(xenaddons$self());
    }

    // On the client the name usually arrives through synched data rather than setCustomName.
    @Inject(method = "onSyncedDataUpdated(Lnet/minecraft/network/syncher/EntityDataAccessor;)V", at = @At("TAIL"))
    private void xenaddons$slayer$onSyncedDataUpdated(EntityDataAccessor<?> accessor, CallbackInfo ci) {
        if (accessor != DATA_CUSTOM_NAME) return;
        if (!(xenaddons$self() instanceof ArmorStand)) return;
        if (getCustomName() == null) return;
        SlayerNametags.attach(xenaddons$self());
    }

    // Safety net: catches stands whose name landed before the mob existed, and prunes dead links.
    @Inject(method = "tick", at = @At("RETURN"))
    private void xenaddons$slayer$tick(CallbackInfo ci) {
        if (xenaddons$slayerAttachCooldown-- > 0) return;
        xenaddons$slayerAttachCooldown = 20;

        if (xenaddons$slayerAttachments != null) {
            xenaddons$slayerAttachments.removeIf(ref -> {
                Entity attached = ref.get();
                return attached == null || !attached.isAlive();
            });
        }

        Entity self = xenaddons$self();
        if (!(self instanceof ArmorStand stand)) return;
        if (!stand.hasCustomName()) return;
        SlayerNametags.attach(self);
    }

    @Inject(method = "onRemoval", at = @At("RETURN"))
    private void xenaddons$slayer$onRemoval(Entity.RemovalReason reason, CallbackInfo ci) {
        if (!(xenaddons$slayerAttachedTo instanceof SlayerEntityDuck duck)) return;
        duck.xenaddons$attachments().removeIf(ref -> ref.get() == xenaddons$self());
    }
}
