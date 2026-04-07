package xenus.xenaddons.features.impl.dungeons;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.ActionSetting;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.settings.ModeSetting;
import xenus.xenaddons.settings.NumberSetting;
import xenus.xenaddons.settings.StringSetting;
import xenus.xenaddons.mixin.client.AccessorChatHud;
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
    private static final Box[] REGIONS = {
            new Box(90, 106, 50, 111, 145, 123),
            new Box(17, 106, 121, 108, 145, 143),
            new Box(-2, 106, 51, 20, 145, 142),
            new Box(-1, 26, 29, 191, 145, 58),
            new Box(3, 5, 0, 128, 48, 140)
    };
    private static final Identifier[] RANDOM_SOUND_POOL = new Identifier[]{
            Identifier.of("xenaddons", "reminder_gate.fah"),
            Identifier.of("xenaddons", "reminder_gate.nuke_bomb"),
            Identifier.of("xenaddons", "reminder_gate.android_meme"),
            Identifier.of("xenaddons", "reminder_gate.pump_shotgun"),
            Identifier.of("xenaddons", "reminder_gate.knocked"),
            Identifier.of("xenaddons", "reminder_gate.hog_rider"),
            Identifier.of("xenaddons", "reminder_gate.airhorn")
    };

    private final StringSetting displayText = new StringSetting("Display Text", "GATEEEEEE !!!!!!!", 64);
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
    private final NumberSetting hudOffsetX = new NumberSetting("HUD Offset X", 0, -1000, 1000, 1);
    private final NumberSetting hudY = new NumberSetting("HUD Y", 84, -1000, 1000, 1);
    private final NumberSetting hudScale = new NumberSetting("HUD Scale", 5.0, 0.5, 6.0, 0.05);

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

        hudOffsetX.setVisibility(() -> false);
        hudY.setVisibility(() -> false);
        hudScale.setVisibility(() -> false);
        this.registerSetting(hudOffsetX);
        this.registerSetting(hudY);
        this.registerSetting(hudScale);

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

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (!this.isEnabled()) return;
            if (debugDisplayTicks <= 0 && !notificationActive) return;
            renderHud(drawContext, getDisplayText());
        });
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

    private int getRegionIndex(ClientPlayerEntity player) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        for (int i = 0; i < REGIONS.length; i++) {
            Box box = REGIONS[i];
            if (x >= Math.min(box.minX, box.maxX) && x <= Math.max(box.minX, box.maxX)
                && y >= Math.min(box.minY, box.maxY) && y <= Math.max(box.minY, box.maxY)
                && z >= Math.min(box.minZ, box.maxZ) && z <= Math.max(box.minZ, box.maxZ)) {
                return i;
            }
        }
        return -1;
    }

    private void syncGateStateFromChat() {
        if (mc.inGameHud == null || mc.player == null) return;
        AccessorChatHud accessor = (AccessorChatHud) mc.inGameHud.getChatHud();
        List<ChatHudLine> messages = accessor.xenaddons$getMessages();
        if (messages == null || messages.isEmpty()) return;

        int start = Math.max(0, messages.size() - CHAT_HISTORY_SYNC_MESSAGES);
        int currentTick = mc.player.age;
        for (int i = messages.size() - 1; i >= start; i--) {
            ChatHudLine line = messages.get(i);
            if (line == null || line.content() == null) continue;
            if ((currentTick - line.creationTick()) > CHAT_HISTORY_SYNC_TICKS) continue;
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

    private void renderHud(DrawContext drawContext, String text) {
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();
        int textWidth = mc.textRenderer.getWidth(text);
        int textHeight = mc.textRenderer.fontHeight;
        float scale = (float) hudScale.getValue().doubleValue();
        float inv = 1.0f / scale;
        int centerXRaw = (width / 2) + hudOffsetX.getValue().intValue();
        int yRaw = hudY.getValue().intValue();
        int minCenterX = (int) Math.ceil((textWidth * scale) / 2.0f) + 2;
        int maxCenterX = width - minCenterX;
        int centerX = MathHelper.clamp(centerXRaw, minCenterX, Math.max(minCenterX, maxCenterX));
        int maxY = height - (int) Math.ceil(textHeight * scale) - 2;
        int y = MathHelper.clamp(yRaw, 2, Math.max(2, maxY));

        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().scale(scale, scale);
        int x = (int) ((centerX * inv) - (textWidth / 2f));
        int drawY = (int) (y * inv);
        drawContext.drawTextWithShadow(mc.textRenderer, text, x, drawY, 0xFFFF5555);
        drawContext.getMatrices().popMatrix();
    }

    public void renderPreview(DrawContext drawContext) {
        renderHud(drawContext, getDisplayText());
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
        if (mc.player == null || mc.world == null) return;
        Identifier id = resolveSoundId(sound.getValue());
        SoundEvent event = SoundEvent.of(id);
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
            case "Nuke Bomb" -> Identifier.of("xenaddons", "reminder_gate.nuke_bomb");
            case "Android Meme" -> Identifier.of("xenaddons", "reminder_gate.android_meme");
            case "Pump Shotgun" -> Identifier.of("xenaddons", "reminder_gate.pump_shotgun");
            case "Knocked" -> Identifier.of("xenaddons", "reminder_gate.knocked");
            case "Hog Rider" -> Identifier.of("xenaddons", "reminder_gate.hog_rider");
            case "Airhorn" -> Identifier.of("xenaddons", "reminder_gate.airhorn");
            case "FAHHHHHHHHHHHHHH" -> Identifier.of("xenaddons", "reminder_gate.fah");
            default -> {
                Identifier parsed = Identifier.tryParse(selected);
                yield parsed != null ? parsed : Identifier.of("xenaddons", "reminder_gate.fah");
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

    public int getHudOffsetX() {
        return hudOffsetX.getValue().intValue();
    }

    public int getHudY() {
        return hudY.getValue().intValue();
    }

    public double getHudScale() {
        return hudScale.getValue();
    }

    public void setHudOffsetX(int value) {
        hudOffsetX.setValue((double) value);
    }

    public void setHudY(int value) {
        hudY.setValue((double) value);
    }

    public void setHudScale(double value) {
        hudScale.setValue(value);
    }
}
