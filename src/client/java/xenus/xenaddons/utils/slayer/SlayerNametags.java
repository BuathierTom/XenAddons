package xenus.xenaddons.utils.slayer;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Links the invisible nametag armor stands Hypixel floats above a mob back to that mob, so we can
 * read a slayer boss's health straight off its own nametag.
 *
 * Ported from Athen's EntityAPI (BSD 3-Clause, Copyright (c) 2025-2026 Starred), whose approach
 * comes from SkyblockAPI (MIT). Used with the author's permission.
 */
public final class SlayerNametags {

    // Characters a pure damage/health indicator is made of. A tag containing only these is a
    // floating number, never a name, so it is not worth attaching to anything.
    private static final String DAMAGE_SYMBOLS = "✧✯❤⚔✷ﬗ♞☄";

    private SlayerNametags() {
    }

    public static void attach(Entity stand) {
        // This runs for every named armor stand in the world, so the slayer modules' toggles gate
        // the whole mechanism: with both off it costs one boolean check per stand.
        if (!SlayerTracker.isTrackingWanted()) return;
        if (stand == null) return;

        Component customName = stand.getCustomName();
        if (customName == null) return;
        if (isDamageTag(SlayerTypes.stripFormatting(customName.getString()))) return;

        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        Entity best = null;
        Entity second = null;
        double bestDistance = Double.MAX_VALUE;
        double secondDistance = Double.MAX_VALUE;
        int bestTickGap = Integer.MAX_VALUE;

        for (Entity candidate : level.getEntities(stand, stand.getBoundingBox().inflate(0.0, 1.0, 0.0))) {
            Entity entity = candidate;
            if (entity instanceof ArmorStand) continue;
            if (!entity.isAlive()) continue;
            // Skyblock hides the real mob behind an invisible entity in a few places, but an
            // invisible entity wearing armor (or a creeper) is still the thing being named.
            if (entity.isInvisible() && !(entity instanceof Creeper) && !hasArmor(entity)) continue;

            // Tarantula bosses ride a cave spider; the name belongs to the spider carrying it.
            if (entity instanceof CaveSpider) {
                for (Entity mount : level.getEntities(entity, entity.getBoundingBox().inflate(0.0, 0.5, 0.0))) {
                    if (mount instanceof Spider && !mount.isInvisible() && mount.isAlive()) {
                        entity = mount;
                        break;
                    }
                }
            }

            double distance = entity.distanceToSqr(stand);
            if (best != null && distance > bestDistance) continue;

            int tickGap = Math.abs(entity.tickCount - stand.tickCount);
            // Same spot, same age: this is certainly the owner, stop looking.
            if (distance == 0.0 && tickGap == 0) {
                best = entity;
                break;
            }

            if (best == null || distance < bestDistance || (distance == bestDistance && tickGap < bestTickGap)) {
                second = best;
                secondDistance = bestDistance;
                best = entity;
                bestDistance = distance;
                bestTickGap = tickGap;
            } else if (second == null || distance < secondDistance) {
                second = entity;
                secondDistance = distance;
            }
        }

        if (best == null) return;
        if (!(stand instanceof SlayerEntityDuck standDuck)) return;

        // Already pointing at one of the two closest candidates: leave it alone rather than
        // letting the tag flicker between two mobs standing on top of each other.
        Entity previous = standDuck.xenaddons$attachedTo();
        if (previous != null && (previous == best || previous == second)) return;

        if (!(best instanceof SlayerEntityDuck mobDuck)) return;
        List<WeakReference<Entity>> attachments = mobDuck.xenaddons$attachments();
        attachments.removeIf(ref -> {
            Entity attached = ref.get();
            return attached == null || !attached.isAlive();
        });

        boolean alreadyListed = false;
        for (WeakReference<Entity> ref : attachments) {
            if (ref.get() == stand) {
                alreadyListed = true;
                break;
            }
        }
        if (!alreadyListed) attachments.add(new WeakReference<>(stand));
        standDuck.xenaddons$attachedTo(best);

        SlayerTracker.onAttach(stand, best);
    }

    /** The nametag lines currently floating above {@code mob}, top line last as Hypixel sends them. */
    public static List<Component> attachedNames(Entity mob) {
        if (!(mob instanceof SlayerEntityDuck duck)) return Collections.emptyList();
        List<Component> names = new ArrayList<>();
        for (WeakReference<Entity> ref : duck.xenaddons$attachments()) {
            Entity stand = ref.get();
            if (stand == null || !stand.isAlive()) continue;
            Component name = stand.getCustomName();
            if (name != null) names.add(name);
        }
        return names;
    }

    private static boolean isDamageTag(String plain) {
        if (plain == null || plain.isEmpty()) return true;
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (Character.isDigit(c) || c == ',' || DAMAGE_SYMBOLS.indexOf(c) >= 0) continue;
            return false;
        }
        return true;
    }

    private static boolean hasArmor(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        return !living.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                || !living.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                || !living.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                || !living.getItemBySlot(EquipmentSlot.FEET).isEmpty();
    }
}
