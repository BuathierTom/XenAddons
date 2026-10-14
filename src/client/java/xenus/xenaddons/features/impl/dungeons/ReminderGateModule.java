package xenus.xenaddons.features.impl.dungeons;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.AABB;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.ActionSetting;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.settings.ModeSetting;
import xenus.xenaddons.settings.StringSetting;
import xenus.xenaddons.compat.XenChatHudCompat;
import xenus.xenaddons.hud.HudElement;
import xenus.xenaddons.hud.HudRender;
import xenus.xenaddons.utils.DungeonUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReminderGateModule extends Module {
    private static final Pattern LEVER_FRACTION_PATTERN = Pattern.compile("\\(\\s*\\d+\\s*/\\s*(\\d+)\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COLOR_PATTERN = Pattern.compile("§[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);
    private static final String GATE_DESTROYED = "the gate has been destroyed!";
    private static final int CHAT_HISTORY_SYNC_TICKS = 20 * 15;
    private static final int CHAT_HISTORY_SYNC_MESSAGES = 10;
    private static final int NOTIFICATION_DURATION_TICKS = 20 * 5;
    private static final AABB[] REGIONS = {
            new AABB(90, 106, 50, 111, 145, 123),
            new AABB(17, 106, 121, 108, 145, 143),
            new AABB(-2, 106, 51, 20, 145, 142),
            new AABB(-1, 26, 29, 191, 145, 58),
            new AABB(3, 5, 0, 128, 48, 140)
    };
    private static final Identifier[] RANDOM_SOUND_POOL = new Identifier[]{
            Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.fah"),
            Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.nuke_bomb"),
            Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.android_meme"),
            Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.pump_shotgun"),
            Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.knocked"),
            Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.hog_rider"),
            Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.airhorn")
    };

    private final StringSetting displayText = new StringSetting("Display Component", "GATEEEEEE !!!!!!!", 64);
    private final BooleanSetting enableSound = new BooleanSetting("Enable Sound", true);
    private final ModeSetting sound = new ModeSetting(
            "Sound",
            "FAHHHHHHHHHHHHHH",
            "Random",
            "FAHHHHHHHHHHHHHH",
            "Nuke Bomb",
            "Android Meme",
            "Pump Shotgun",
            "Knocked",
            "Hog Rider",
            "Airhorn"
    );
    private final ActionSetting playSound = new ActionSetting("Play Sound", this::playSelectedSoundPreview);

    private int phaseIndex = 0;
    private int lastDenominator = 0;
    private boolean notificationActive = false;
    private boolean soundPlayedForCurrentGate = false;
    private boolean gateBrokenThisPhase = false;
    private int currentRegion = -1;
    private int previousRegion = -1;
    private int lastDungeonSeconds = -1;
    private int debugDisplayTicks = 0;
    private int notificationTicks = 0;
    private String lastMessage = "";
    private long lastMessageAtMs = 0L;

    public ReminderGateModule() {
        super("Reminder Gate", "Shows a gate reminder when lever phases start", Category.DUNGEON);

        this.registerSetting(displayText);
        this.registerSetting(enableSound);
        this.registerSetting(sound);
        this.registerSetting(playSound);

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (this.isEnabled()) handleChat(message.getString());
        });

        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            if (this.isEnabled()) handleChat(message.getString());
            return true;
        });
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (this.isEnabled()) handleChat(message.getString());
            return true;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!this.isEnabled()) return;
            if (debugDisplayTicks > 0) debugDisplayTicks--;
            updateRuntimeState();
        });

        hudElement("Reminder Gate", (context, example) -> HudRender.centeredText(context, getDisplayText(), 0xFFFF5555))
            .visibleWhen(() -> debugDisplayTicks > 0 || notificationActive)
            .centered()
            .defaults(HudElement.CENTER, 84.0f, 5.0f);
    }

    private void handleChat(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) return;

        String clean = COLOR_PATTERN.matcher(rawMessage).replaceAll("").trim();
        if (clean.isEmpty()) return;
        long now = System.currentTimeMillis();
        if (clean.equals(lastMessage) && (now - lastMessageAtMs) < 80L) return;
        lastMessage = clean;
        lastMessageAtMs = now;

        String lower = clean.toLowerCase();
        if (lower.contains(GATE_DESTROYED)) {
            notificationActive = false;
            notificationTicks = 0;
            soundPlayedForCurrentGate = false;
            gateBrokenThisPhase = true;
            return;
        }

        if (currentRegion < 0) return;

        String lowerLever = lower.toLowerCase();
        if (!lowerLever.contains("lever")) return;
        if (!lowerLever.contains("activated") && !lowerLever.contains("completed")) return;

        Matcher matcher = LEVER_FRACTION_PATTERN.matcher(clean);
        if (!matcher.find()) return;

        int denominator;
        try {
            denominator = Integer.parseInt(matcher.group(1));
        } catch (Exception ignored) {
            return;
        }
        if (denominator != 7 && denominator != 8) return;

        int previousPhase = phaseIndex;
        int resolvedPhase = resolvePhase(denominator);
        boolean destroyedBeforeFirstLeverOfPhase = gateBrokenThisPhase && previousPhase == 0;
        if (resolvedPhase != phaseIndex) {
            phaseIndex = resolvedPhase;
            notificationActive = false;
            notificationTicks = 0;
            soundPlayedForCurrentGate = false;
            gateBrokenThisPhase = destroyedBeforeFirstLeverOfPhase;
        }
        lastDenominator = denominator;
        
        if (gateBrokenThisPhase) {
            return;
        }

        activateNotification();
    }

    private void activateNotification() {
        if (notificationActive) {
            return;
        }
        notificationActive = true;
        notificationTicks = NOTIFICATION_DURATION_TICKS;
        if (!soundPlayedForCurrentGate) {
            playSelectedSound();
            soundPlayedForCurrentGate = true;
        }
    }

    private int resolvePhase(int denominator) {
        if (phaseIndex == 0) {
            return denominator == 8 ? 2 : 1;
        }
        if (lastDenominator == 7 && denominator == 8) return 2;
        if (lastDenominator == 8 && denominator == 7) return 3;
        if (phaseIndex < 2 && denominator == 8) return 2;
        if (phaseIndex == 2 && denominator == 7) return 3;
        return phaseIndex;
    }

    private void updateRuntimeState() {
        if (!DungeonUtils.inDungeon() || mc.player == null) {
            clearDungeonRuntime();
            return;
        }

        if (notificationActive && notificationTicks > 0) {
            notificationTicks--;
            if (notificationTicks <= 0) {
                notificationActive = false;
                gateBrokenThisPhase = true;
            }
        }

        previousRegion = currentRegion;
        currentRegion = getRegionIndex(mc.player);
        if (currentRegion < 0) {
            notificationActive = false;
            notificationTicks = 0;
        } else if (previousRegion < 0) {
            syncGateStateFromChat();
        }
        int currentSeconds = DungeonUtils.dungeonSeconds;
        if (lastDungeonSeconds >= 0 && currentSeconds >= 0 && currentSeconds < lastDungeonSeconds) {
            clearDungeonRuntime();
            currentRegion = getRegionIndex(mc.player);
        }
        lastDungeonSeconds = currentSeconds;
    }

    private int getRegionIndex(LocalPlayer player) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        for (int i = 0; i < REGIONS.length; i++) {
            AABB box = REGIONS[i];
            if (x >= Math.min(box.minX, box.maxX) && x <= Math.max(box.minX, box.maxX)
                && y >= Math.min(box.minY, box.maxY) && y <= Math.max(box.minY, box.maxY)
                && z >= Math.min(box.minZ, box.maxZ) && z <= Math.max(box.minZ, box.maxZ)) {
                return i;
            }
        }
        return -1;
    }

    private void syncGateStateFromChat() {
        if (mc.gui == null || mc.player == null) return;
        List<GuiMessage> messages = XenChatHudCompat.getMessages(mc.gui.getChat());
        if (messages == null || messages.isEmpty()) return;

        int start = Math.max(0, messages.size() - CHAT_HISTORY_SYNC_MESSAGES);
        int currentTick = mc.player.tickCount;
        for (int i = messages.size() - 1; i >= start; i--) {
            GuiMessage line = messages.get(i);
            if (line == null || line.content() == null) continue;
            if ((currentTick - line.addedTime()) > CHAT_HISTORY_SYNC_TICKS) continue;
            String clean = COLOR_PATTERN.matcher(line.content().getString()).replaceAll("").trim().toLowerCase();
            if (clean.isEmpty()) continue;
            if (clean.contains(GATE_DESTROYED)) {
                notificationActive = false;
                notificationTicks = 0;
                soundPlayedForCurrentGate = false;
                gateBrokenThisPhase = true;
                return;
            }
        }
    }

    private String getDisplayText() {
        String text = displayText.getValue();
        return (text == null || text.isBlank()) ? "GATEEEEEE !!!!!!!" : text;
    }

    private void playSelectedSoundPreview() {
        playSelectedSound();
    }

    public void debugTrigger() {
        debugDisplayTicks = 80;
        playSelectedSound();
    }

    private void playSelectedSound() {
        if (!enableSound.getValue()) return;
        if (mc.player == null || mc.level == null) return;
        Identifier id = resolveSoundId(sound.getValue());
        SoundEvent event = SoundEvent.createVariableRangeEvent(id);
        float volume = 8.0f;
        float pitch = 1.0f;
        mc.player.playSound(event, volume, pitch);
    }

    private Identifier resolveSoundId(String selected) {
        return switch (selected) {
            case "Random" -> {
                int idx = ThreadLocalRandom.current().nextInt(RANDOM_SOUND_POOL.length);
                yield RANDOM_SOUND_POOL[idx];
            }
            case "Nuke Bomb" -> Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.nuke_bomb");
            case "Android Meme" -> Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.android_meme");
            case "Pump Shotgun" -> Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.pump_shotgun");
            case "Knocked" -> Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.knocked");
            case "Hog Rider" -> Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.hog_rider");
            case "Airhorn" -> Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.airhorn");
            case "FAHHHHHHHHHHHHHH" -> Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.fah");
            default -> {
                Identifier parsed = Identifier.tryParse(selected);
                yield parsed != null ? parsed : Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.fah");
            }
        };
    }

    private void clearDungeonRuntime() {
        phaseIndex = 0;
        lastDenominator = 0;
        notificationActive = false;
        notificationTicks = 0;
        soundPlayedForCurrentGate = false;
        gateBrokenThisPhase = false;
        currentRegion = -1;
        previousRegion = -1;
        lastDungeonSeconds = -1;
        lastMessage = "";
        lastMessageAtMs = 0L;
    }

    private void resetRuntime() {
        clearDungeonRuntime();
        debugDisplayTicks = 0;
    }

    @Override
    public void onDisable() {
        resetRuntime();
    }
}
