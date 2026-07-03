package xenus.xenaddons.features.impl.misc.cooldown;

import net.minecraft.world.item.ItemStack;
import xenus.xenaddons.utils.DungeonUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum ItemAbility {
    WITHER_IMPACT(5, false, true, "HYPERION", "ASTRAEA", "SCYLLA", "VALKYRIE"),
    WITHER_SHIELD_SCROLL(10, true, true),
    SHADOW_WARP_SCROLL(10),
    IMPLOSION_SCROLL(10),
    GYROKINETIC_WAND_LEFT(30, true, false, "GYROKINETIC_WAND"),
    GYROKINETIC_WAND_RIGHT(10, false, false, "GYROKINETIC_WAND"),
    ICE_SPRAY_WAND(5),
    SWORD_OF_BAD_HEALTH(5),
    RAGNAROCK_AXE(20),
    WEIRD_TUBA(20),
    WEIRDER_TUBA(20),
    WITHER_CLOAK(10),
    ROGUE_SWORD("Speed Boost", 30, true, "Rogue Sword");

    public final String abilityName;
    private final int cooldownInSeconds;
    public final boolean alternativePosition;
    public final String[] itemNames;
    public final boolean actionBarDetection;
    private final boolean ignoreMageCooldownReduction;
    public final boolean newVariant;
    public final Set<String> internalNames = new HashSet<>();

    private long lastActivationMs = Long.MIN_VALUE / 4;
    private long lastItemClickMs = Long.MIN_VALUE / 4;
    private AbilityColor specialColor = null;

    ItemAbility(int cooldownInSeconds, String... alternateInternalNames) {
        this(cooldownInSeconds, false, false, alternateInternalNames);
    }

    ItemAbility(int cooldownInSeconds, boolean alternativePosition, boolean ignoreMageCooldownReduction, String... alternateInternalNames) {
        this.abilityName = "no name";
        this.cooldownInSeconds = cooldownInSeconds;
        this.itemNames = new String[0];
        this.alternativePosition = alternativePosition;
        this.actionBarDetection = false;
        this.ignoreMageCooldownReduction = ignoreMageCooldownReduction;
        this.newVariant = true;
        this.internalNames.add(name());
        this.internalNames.addAll(Arrays.asList(alternateInternalNames));
    }

    ItemAbility(String abilityName, int cooldownInSeconds, String... itemNames) {
        this(abilityName, cooldownInSeconds, false, itemNames);
    }

    ItemAbility(String abilityName, int cooldownInSeconds, boolean ignoreMageCooldownReduction, String... itemNames) {
        this.abilityName = abilityName;
        this.cooldownInSeconds = cooldownInSeconds;
        this.itemNames = itemNames;
        this.alternativePosition = false;
        this.actionBarDetection = true;
        this.ignoreMageCooldownReduction = ignoreMageCooldownReduction;
        this.newVariant = false;
    }

    public static ItemAbility getByInternalName(String internalName) {
        if (internalName == null || internalName.isBlank()) return null;
        for (ItemAbility ability : values()) {
            if (ability.newVariant && ability.internalNames.contains(internalName)) {
                return ability;
            }
        }
        return null;
    }

    public static Set<ItemAbility> getAllAbilityScrolls(ItemStack stack) {
        return AbilityScrollsParser.getAllAbilityScrolls(stack);
    }

    public void activate() {
        activate(null, cooldownInSeconds * 1000);
    }

    public void activate(AbilityColor color, int customCooldownMs) {
        this.specialColor = color;
        long total = getCooldownMs();
        this.lastActivationMs = System.currentTimeMillis() - Math.max(0L, total - customCooldownMs);
    }

    public void setItemClick() {
        this.lastItemClickMs = System.currentTimeMillis();
    }

    public void activateIfClickedRecently() {
        if (timeSinceItemClickMs() < 400) {
            if (!isOnCooldown()) {
                activate();
            }
        }
    }

    public long timeSinceItemClickMs() {
        return System.currentTimeMillis() - lastItemClickMs;
    }

    public boolean isOnCooldown() {
        return elapsedSinceActivationMs() < getCooldownMs();
    }

    public long remainingCooldownMs() {
        return Math.max(0L, getCooldownMs() - elapsedSinceActivationMs());
    }

    public String getDurationText() {
        double remaining = remainingCooldownMs() / 1000.0;
        if (remaining < 2.0) {
            return String.format(java.util.Locale.US, "%.1f", Math.round(remaining * 10.0) / 10.0);
        }
        return Integer.toString((int) Math.floor(remaining));
    }

    public AbilityColor getSpecialColor() {
        return specialColor;
    }

    public void clearSpecialColor() {
        this.specialColor = null;
    }

    public long getCooldownMs() {
        if (this == RAGNAROCK_AXE) {
            return cooldownInSeconds * 1000L;
        }
        return (long) (cooldownInSeconds * 1000L * getMultiplier());
    }

    public double getMultiplier() {
        if (ignoreMageCooldownReduction) return 1.0;
        if (!DungeonUtils.inDungeon()) return 1.0;
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null || mc.player == null) return 1.0;
        String playerClass = DungeonUtils.getPlayerClass(mc.player);
        if (playerClass == null) {
            playerClass = DungeonUtils.getPlayerClass(mc.player.getGameProfile().name());
        }
        if (!"Mage".equals(playerClass)) return 1.0;

        int classLevel = Math.max(0, DungeonUtils.getPlayerClassLevel(mc.player));
        double abilityCooldownMultiplier = 1.0;
        abilityCooldownMultiplier -= DungeonUtils.isUniqueClass() ? 0.5 : 0.25;
        abilityCooldownMultiplier -= 0.01 * Math.floor(classLevel / 2.0);
        return Math.max(0.0, abilityCooldownMultiplier);
    }

    public void resetState() {
        this.lastActivationMs = Long.MIN_VALUE / 4;
        this.lastItemClickMs = Long.MIN_VALUE / 4;
        this.specialColor = null;
    }

    private long elapsedSinceActivationMs() {
        return System.currentTimeMillis() - lastActivationMs;
    }
}
