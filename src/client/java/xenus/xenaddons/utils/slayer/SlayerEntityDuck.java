package xenus.xenaddons.utils.slayer;

import net.minecraft.world.entity.Entity;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Implemented on every {@link Entity} by MixinEntitySlayerNametags so that a mob can carry the
 * invisible armor stands that display its name, and an armor stand can point back at its mob.
 *
 * Ported from Athen (BSD 3-Clause, Copyright (c) 2025-2026 Starred), whose own attachment idea
 * comes from SkyblockAPI (MIT). Used with the author's permission.
 */
public interface SlayerEntityDuck {

    List<WeakReference<Entity>> xenaddons$attachments();

    Entity xenaddons$attachedTo();

    void xenaddons$attachedTo(Entity entity);
}
