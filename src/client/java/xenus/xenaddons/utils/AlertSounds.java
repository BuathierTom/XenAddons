package xenus.xenaddons.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** The alert sound list used by the mask proc alert, shared so new alerts ping the same way. */
public final class AlertSounds {

    public static final String DEFAULT = "Default XP Pickup";

    // Matches the sliders' upper bound; also bounds the layering loop.
    private static final float MAX_VOLUME = 5.0f;
    private static final float SILENCE_THRESHOLD = 0.01f;
    private static final float[] NO_LAYERS = new float[0];

    public static final String[] NAMES = {
            DEFAULT,
            "Blaze Hurt",
            "Vault Break",
            "Elder Hurt",
            "Totem Use",
            "Sculk Hit",
            "Ender Chest",
            "Iron Xylophone",
            "Bell",
            "Pling"
    };

    private AlertSounds() {
    }

    public static void play(String selected, double volume, double pitch) {
        play(resolve(selected), volume, pitch);
    }

    /**
     * Minecraft clamps a single sound instance's volume to 1.0 in SoundEngine.calculateVolume, so
     * anything the slider offered above that did nothing at all. Layering identical instances is
     * what actually gets louder: they are mixed together, so N copies carry N times the amplitude.
     */
    public static void play(SoundEvent event, double volume, double pitch) {
        if (event == null) return;

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) return;

        for (float layer : layers(volume)) {
            client.player.playSound(event, layer, (float) pitch);
        }
    }

    /**
     * Splits a requested volume into instances of at most 1.0, which together carry it. A volume of
     * 2.5 comes out as 1.0 + 1.0 + 0.5, so the amplitude really is two and a half times a single
     * play instead of being flattened to one.
     */
    static float[] layers(double volume) {
        float remaining = (float) Math.min(MAX_VOLUME, Math.max(0.0, volume));
        int count = (int) Math.ceil((remaining - SILENCE_THRESHOLD) / 1.0f);
        if (count <= 0) return NO_LAYERS;

        float[] layers = new float[count];
        for (int i = 0; i < count; i++) {
            layers[i] = Math.min(1.0f, remaining);
            remaining -= layers[i];
        }
        return layers;
    }

    public static SoundEvent resolve(String selected) {
        Identifier id = switch (selected == null ? "" : selected) {
            case "Blaze Hurt" -> Identifier.fromNamespaceAndPath("minecraft", "entity.blaze.hurt");
            case "Vault Break" -> Identifier.fromNamespaceAndPath("minecraft", "block.vault.break");
            case "Elder Hurt" -> Identifier.fromNamespaceAndPath("minecraft", "entity.elder_guardian.hurt_land");
            case "Totem Use" -> Identifier.fromNamespaceAndPath("minecraft", "item.totem.use");
            case "Sculk Hit" -> Identifier.fromNamespaceAndPath("minecraft", "block.sculk_catalyst.hit");
            case "Ender Chest" -> Identifier.fromNamespaceAndPath("minecraft", "block.ender_chest.close");
            case "Iron Xylophone" -> Identifier.fromNamespaceAndPath("minecraft", "block.note_block.iron_xylophone");
            case "Bell" -> Identifier.fromNamespaceAndPath("minecraft", "block.note_block.bell");
            case "Pling" -> Identifier.fromNamespaceAndPath("minecraft", "block.note_block.pling");
            default -> Identifier.fromNamespaceAndPath("minecraft", "entity.experience_orb.pickup");
        };
        return BuiltInRegistries.SOUND_EVENT.get(id)
                .map(holder -> holder.value())
                .orElse(SoundEvent.createVariableRangeEvent(id));
    }
}
