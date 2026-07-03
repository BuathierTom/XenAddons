package xenus.xenaddons.features.impl.misc;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.Identifier;

import java.util.Set;

public final class CustomHyperionSoundHandler {
    private static final Set<String> WITHER_BLADES = Set.of("HYPERION", "VALKYRIE", "SCYLLA", "ASTRAEA");
    private static final long MAX_DELAY_MS = 2_000L;
    private static final float VOLUME_BOOST = 2.5f;
    private static final float MAX_EFFECTIVE_VOLUME = 8.0f;
    private static final int LOUDNESS_PASSES = 1;
    private static long suppressUntilMs = -1L;
    private static boolean customPlayedInWindow = false;

    private CustomHyperionSoundHandler() {
    }

    public static void onItemUse(ItemStack stack) {
        if (!CustomHyperionSoundModule.isActive()) return;
        if (stack == null || stack.isEmpty()) return;
        String id = getItemId(stack);
        if (id == null || !WITHER_BLADES.contains(id)) {
            String displayName = stack.getHoverName().getString().toUpperCase(java.util.Locale.ROOT);
            if (!(displayName.contains("HYPERION") || displayName.contains("VALKYRIE") || displayName.contains("SCYLLA") || displayName.contains("ASTRAEA"))) {
                return;
            }
        }
        suppressUntilMs = System.currentTimeMillis() + MAX_DELAY_MS;
        customPlayedInWindow = false;
    }

    public static boolean onPlaySound(SoundInstance sound) {
        if (!CustomHyperionSoundModule.isActive()) return false;
        if (sound == null) return false;
        long now = System.currentTimeMillis();
        if (suppressUntilMs < 0L || now > suppressUntilMs) {
            suppressUntilMs = -1L;
            customPlayedInWindow = false;
            return false;
        }

        String soundId = safeId(sound);
        if (soundId == null) return false;
        String lowerId = soundId.toLowerCase(java.util.Locale.ROOT);

        if (lowerId.contains("zombie_villager.cure")) {
            return true;
        }
        if (lowerId.contains("enderman.teleport") || lowerId.contains("enderman_teleport")) {
            return true;
        }
        if (lowerId.contains("extinguish_fire") || lowerId.contains("fizz")) {
            return true;
        }
        // Hypixel can emit several explosion ids; cancel all explosion variants in the click window.
        if (!lowerId.contains("explode")) {
            return false;
        }

        if (!customPlayedInWindow) {
            playCustomSound();
            customPlayedInWindow = true;
        }
        return true;
    }

    public static void playPreviewSound() {
        CustomHyperionSoundModule module = CustomHyperionSoundModule.get();
        Minecraft mc = Minecraft.getInstance();
        if (module == null || mc == null || mc.level == null || mc.player == null) return;
        playCustomSound();
    }

    private static void playCustomSound() {
        CustomHyperionSoundModule module = CustomHyperionSoundModule.get();
        if (module == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) return;

        Identifier soundId = resolveSoundId(module.sound.getValue());
        var eventRef = BuiltInRegistries.SOUND_EVENT.get(soundId);
        if (eventRef.isEmpty()) return;
        SoundEvent event = eventRef.get().value();

        float volume = (float) module.volume.getValue().doubleValue();
        float effectiveVolume = Math.min(MAX_EFFECTIVE_VOLUME, volume * VOLUME_BOOST);
        float pitch = (float) module.pitch.getValue().doubleValue();
        // Play multiple local passes to make the custom hype clearly louder than vanilla.
        for (int i = 0; i < LOUDNESS_PASSES; i++) {
            mc.player.playSound(event, effectiveVolume, pitch);
        }
    }

    private static Identifier resolveSoundId(String selected) {
        if (selected == null || selected.isBlank()) {
            return Identifier.fromNamespaceAndPath("minecraft", "block.note_block.iron_xylophone");
        }
        return switch (selected) {
            case "Blaze Hurt" -> Identifier.fromNamespaceAndPath("minecraft", "entity.blaze.hurt");
            case "XP Pickup" -> Identifier.fromNamespaceAndPath("minecraft", "entity.experience_orb.pickup");
            case "Vault Break" -> Identifier.fromNamespaceAndPath("minecraft", "block.vault.break");
            case "Elder Hurt" -> Identifier.fromNamespaceAndPath("minecraft", "entity.elder_guardian.hurt_land");
            case "Totem Use" -> Identifier.fromNamespaceAndPath("minecraft", "item.totem.use");
            case "Sculk Hit" -> Identifier.fromNamespaceAndPath("minecraft", "block.sculk_catalyst.hit");
            case "Ender Chest" -> Identifier.fromNamespaceAndPath("minecraft", "block.ender_chest.close");
            case "Iron Xylophone" -> Identifier.fromNamespaceAndPath("minecraft", "block.note_block.iron_xylophone");
            default -> {
                Identifier parsed = Identifier.tryParse(selected);
                yield parsed != null ? parsed : Identifier.fromNamespaceAndPath("minecraft", "block.note_block.iron_xylophone");
            }
        };
    }

    private static String getItemId(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        CompoundTag root = customData.copyTag();
        if (root == null) return null;
        CompoundTag extra = root.getCompound("ExtraAttributes").orElse(root.getCompound("extra_attributes").orElse(null));
        if (extra != null) {
            String id = extra.getStringOr("id", "");
            if (id != null && !id.isBlank()) return id;
            id = extra.getStringOr("ID", "");
            if (id != null && !id.isBlank()) return id;
        }
        String direct = root.getStringOr("id", "");
        if (direct != null && !direct.isBlank()) return direct;
        direct = root.getStringOr("ID", "");
        if (direct != null && !direct.isBlank()) return direct;
        return null;
    }

    private static String safeId(SoundInstance sound) {
        try {
            if (sound.getIdentifier() == null) return null;
            return sound.getIdentifier().toString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    
}
