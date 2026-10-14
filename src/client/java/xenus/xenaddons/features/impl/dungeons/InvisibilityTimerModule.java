package xenus.xenaddons.features.impl.dungeons;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.Identifier;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.hud.HudElement;
import xenus.xenaddons.hud.HudRender;
import xenus.xenaddons.settings.ActionSetting;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.settings.ModeSetting;
import xenus.xenaddons.settings.NumberSetting;
import xenus.xenaddons.utils.AlertSounds;
import xenus.xenaddons.utils.DungeonUtils;

import java.util.Locale;
import java.util.regex.Pattern;

public class InvisibilityTimerModule extends Module {
    private static final Pattern SPIRIT_REGEX = Pattern.compile("^Second Wind Activated! Your Spirit Mask saved your life!$");
    private static final Pattern BONZO_REGEX = Pattern.compile("^Your (?:. )?Bonzo's Mask saved your life!$");
    private static final Pattern PHOENIX_REGEX = Pattern.compile("^Your Phoenix Pet saved you from certain death!$");

    private static final int SPIRIT_INVINC_TICKS = 60;
    private static final int BONZO_INVINC_TICKS = 60;
    private static final int PHOENIX_INVINC_TICKS = 60;
    private static final long PROC_MESSAGE_DEBOUNCE_MS = 10_000L;

    private final BooleanSetting maskProcsAlert = new BooleanSetting("Mask procs alert", true);
    private final ModeSetting displayMode = new ModeSetting("Display Mode", "Seconds", "Seconds", "Ticks");
    private final ModeSetting procSound = new ModeSetting(
            "Proc Sound",
            "Default XP Pickup",
            "Default XP Pickup",
            "Blaze Hurt",
            "Vault Break",
            "Elder Hurt",
            "Totem Use",
            "Sculk Hit",
            "Ender Chest",
            "Iron Xylophone",
            "Bell",
            "Pling"
    );
    private final NumberSetting procSoundVolume = new NumberSetting("Sound Volume", 2.0, 0.0, 5.0, 0.1);
    private final NumberSetting procSoundPitch = new NumberSetting("Sound Pitch", 0.9, 0.0, 2.0, 0.1);
    private final ActionSetting notePlay = new ActionSetting("Note play", this::playProcSoundPreview);

    private int activeTicks = 0;
    private String activeType = null;
    private int alertTicks = 0;
    private String alertText = null;
    private String lastProcKey = "";
    private long lastProcAt = 0L;

    public InvisibilityTimerModule() {
        super("Invisibility timer", "Shows invincibility duration after mask/phoenix procs", Category.DUNGEON);

        this.registerSetting(maskProcsAlert);
        this.registerSetting(displayMode);
        this.registerSetting(procSound);
        this.registerSetting(procSoundVolume);
        this.registerSetting(procSoundPitch);
        this.registerSetting(notePlay);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (activeTicks > 0) activeTicks--;
            if (activeTicks <= 0) activeType = null;
            if (alertTicks > 0) alertTicks--;
            if (alertTicks <= 0) alertText = null;
            if (!this.isEnabled()) {
                activeTicks = 0;
                activeType = null;
                alertTicks = 0;
                alertText = null;
            }
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!this.isEnabled()) return;
            handleChat(message.getString());
        });

        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            if (this.isEnabled()) handleChat(message.getString());
            return true;
        });

        hudElement("Invisibility Timer", (context, example) ->
            HudRender.centeredText(context, getDisplayText(example), getDisplayColor(example)))
            .visibleWhen(() -> activeTicks > 0 && activeType != null)
            .centered()
            .defaults(HudElement.CENTER, 100.0f, 1.0f);

        hudElement("Mask Proc Alert", (context, example) ->
            HudRender.centeredText(context, example ? "Bonzo" : alertText, 0xFFFF2B2B))
            .enabledWhen(() -> isEnabled() && maskProcsAlert.getValue())
            .visibleWhen(() -> alertTicks > 0 && alertText != null && !alertText.isBlank())
            .centered()
            .defaults(HudElement.CENTER, 120.0f, 4.0f);
    }

    private void handleChat(String rawMessage) {
        if (!DungeonUtils.inDungeon()) return;
        if (rawMessage == null || rawMessage.isEmpty()) return;

        String clean = rawMessage.replaceAll("§[0-9a-fk-or]", "");

        int ticks = 0;
        String type = null;
        if (SPIRIT_REGEX.matcher(clean).matches()) {
            ticks = SPIRIT_INVINC_TICKS;
            type = "Spirit";
        } else if (BONZO_REGEX.matcher(clean).matches()) {
            ticks = BONZO_INVINC_TICKS;
            type = "Bonzo";
        } else if (PHOENIX_REGEX.matcher(clean).matches()) {
            ticks = PHOENIX_INVINC_TICKS;
            type = "Phoenix";
        }

        if (ticks <= 0 || type == null) return;

        long now = System.currentTimeMillis();
        String key = type + "|" + clean;
        if (key.equals(lastProcKey) && (now - lastProcAt) < PROC_MESSAGE_DEBOUNCE_MS) return;

        lastProcKey = key;
        lastProcAt = now;
        boolean newProcWindow = activeType == null || !type.equals(activeType);
        activeType = type;
        activeTicks = ticks;
        if (maskProcsAlert.getValue() && newProcWindow) {
            alertText = type;
            alertTicks = 22;
            playProcSound();
        }
    }

    private String getDisplayText(boolean preview) {
        if (preview) return isTicksDisplay() ? formatTicks(50) : formatMillis(2500);
        if (activeTicks <= 0 || activeType == null) return "";
        return isTicksDisplay() ? formatTicks(activeTicks) : formatMillis(activeTicks * 50);
    }

    private int getDisplayColor(boolean preview) {
        int millis = preview ? 2500 : activeTicks * 50;
        if (millis <= 700) return 0xFFFF2B2B;     // red
        if (millis <= 1500) return 0xFFFFD54A;    // yellow
        return 0xFF45E164;                         // green
    }

    private static String formatMillis(int millis) {
        int clamped = Math.max(0, millis);
        int seconds = clamped / 1000;
        int ms = clamped % 1000;
        return String.format(Locale.ROOT, "%d.%03d", seconds, ms);
    }

    private static String formatTicks(int ticks) {
        return Math.max(0, ticks) + "t";
    }

    private boolean isTicksDisplay() {
        return "Ticks".equals(displayMode.getValue());
    }

    public void debugTriggerProcAlert() {
        alertText = "Bonzo";
        alertTicks = 22;
        playProcSound();
    }

    public void debugTriggerProc(String procType) {
        if (procType == null) return;
        String normalized = procType.toLowerCase(Locale.ROOT);

        int ticks;
        String type;
        switch (normalized) {
            case "spirit" -> {
                ticks = SPIRIT_INVINC_TICKS;
                type = "Spirit";
            }
            case "bonzo" -> {
                ticks = BONZO_INVINC_TICKS;
                type = "Bonzo";
            }
            case "phoenix", "pheonix" -> {
                ticks = PHOENIX_INVINC_TICKS;
                type = "Phoenix";
            }
            default -> {
                return;
            }
        }

        activeType = type;
        activeTicks = ticks;
        if (maskProcsAlert.getValue()) {
            alertText = type;
            alertTicks = 22;
            playProcSound();
        }
    }

    private void playProcSoundPreview() {
        if (mc.player == null) return;
        playProcSound();
    }

    private void playProcSound() {
        if (mc.player == null) return;
        // Shared player: a single sound instance is capped at volume 1.0 by the engine, so the
        // slider only goes past that by layering instances.
        AlertSounds.play(resolveProcSound(procSound.getValue()), procSoundVolume.getValue(), procSoundPitch.getValue());
    }

    private SoundEvent resolveProcSound(String selected) {
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
            case "Default XP Pickup" -> Identifier.fromNamespaceAndPath("minecraft", "entity.experience_orb.pickup");
            default -> Identifier.fromNamespaceAndPath("minecraft", "entity.experience_orb.pickup");
        };
        return BuiltInRegistries.SOUND_EVENT.get(id)
                .map(holder -> holder.value())
                .orElse(SoundEvent.createVariableRangeEvent(id));
    }
}
