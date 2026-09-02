package xenus.xenaddons.utils.slayer;

import java.util.List;
import java.util.Locale;

/**
 * Slayer boss names and the nametag shape Hypixel gives them.
 *
 * Name tables taken from Athen (BSD 3-Clause, Copyright (c) 2025-2026 Starred), themselves
 * derived from SkyblockAPI (MIT). Used with the author's permission.
 */
public final class SlayerTypes {

    // Both the base boss and the tier-5 variant, which carries a different name entirely.
    public static final List<String> BOSS_NAMES = List.of(
            "Revenant Horror", "Atoned Horror",
            "Tarantula Broodfather", "Conjoined Brood",
            "Sven Packmaster",
            "Voidgloom Seraph",
            "Inferno Demonlord",
            "Bloodfiend"
    );

    public static final String SPAWNED_BY = "Spawned by:";

    private SlayerTypes() {
    }

    /**
     * A boss nametag reads "☠ Sven Packmaster IV 500❤". The suffix check mirrors Athen's: some
     * bosses end in a hit counter or a star instead of a heart.
     */
    public static boolean isBossNametag(String plain) {
        if (plain == null) return false;
        String text = plain.trim();
        if (text.isEmpty()) return false;
        if (!text.startsWith("☠") && !text.endsWith("❤") && !text.endsWith("❤ ✯") && !text.endsWith(" Hits")) return false;
        return containsBossName(text);
    }

    public static boolean containsBossName(String plain) {
        if (plain == null) return false;
        for (String name : BOSS_NAMES) {
            if (plain.contains(name)) return true;
        }
        return false;
    }

    public static boolean isSpawnedByTag(String plain) {
        return plain != null && plain.contains(SPAWNED_BY);
    }

    /** Reads the player name out of a "Spawned by: Someone" tag. */
    public static String ownerOf(String plain) {
        if (!isSpawnedByTag(plain)) return null;
        int colon = plain.lastIndexOf(':');
        if (colon < 0 || colon + 1 >= plain.length()) return null;
        String owner = plain.substring(colon + 1).trim();
        return owner.isEmpty() ? null : owner;
    }

    public static String stripFormatting(String input) {
        if (input == null || input.isEmpty()) return "";
        return input.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }

    public static String normalize(String input) {
        return stripFormatting(input).trim().toLowerCase(Locale.ROOT);
    }
}
