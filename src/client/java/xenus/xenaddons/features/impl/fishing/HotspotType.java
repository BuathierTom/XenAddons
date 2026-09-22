package xenus.xenaddons.features.impl.fishing;

import java.util.regex.Pattern;

public enum HotspotType {
    SEA_CREATURE(Pattern.compile("\\+\\d+. Sea Creature Chance"), 0x00AAAA),
    FISHING_SPEED(Pattern.compile("\\+\\d+. Fishing Speed"), 0x55FFFF),
    DOUBLE_HOOK(Pattern.compile("\\+\\d+. Double Hook Chance"), 0x5555FF),
    TREASURE(Pattern.compile("\\+\\d+. Treasure Chance"), 0xFFAA00),
    TROPHY_FISH(Pattern.compile("\\+\\d+. Trophy Chance"), 0xFFD700),
    SHARD(Pattern.compile("Chance of .+ Shard"), 0xFFFF55);

    public final Pattern pattern;
    public final int color;

    HotspotType(Pattern pattern, int color) {
        this.pattern = pattern;
        this.color = color;
    }

    public static HotspotType match(String plainText) {
        for (HotspotType type : values()) {
            if (type.pattern.matcher(plainText).matches()) return type;
        }
        return null;
    }
}
