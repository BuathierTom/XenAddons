package xenus.xenaddons.features.impl.dungeons;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.ActionSetting;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.settings.ModeSetting;
import xenus.xenaddons.settings.NumberSetting;
import xenus.xenaddons.settings.StringSetting;
import xenus.xenaddons.utils.DungeonUtils;

import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReminderGateModule extends Module {
    private static final Pattern LEVER_FRACTION_PATTERN = Pattern.compile("\\(\\s*\\d+\\s*/\\s*(\\d+)\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COLOR_PATTERN = Pattern.compile("§[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);
    private static final String GATE_DESTROYED = "the gate has been destroyed!";
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
    private boolean waitingGateDestroyed = false;
    private boolean notificationActive = false;
    private boolean soundPlayedForCurrentGate = false;
    private int debugDisplayTicks = 0;
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
            if (waitingGateDestroyed && !notificationActive && shouldNotifyLocalClass(phaseIndex)) {
                activateNotification();
            }
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
            waitingGateDestroyed = false;
            notificationActive = false;
            soundPlayedForCurrentGate = false;
            return;
        }

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

        int resolvedPhase = resolvePhase(denominator);
        if (resolvedPhase != phaseIndex) {
            phaseIndex = resolvedPhase;
            waitingGateDestroyed = false;
            notificationActive = false;
            soundPlayedForCurrentGate = false;
        }
        lastDenominator = denominator;

        if (!waitingGateDestroyed) {
            waitingGateDestroyed = true;
            if (shouldNotifyLocalClass(phaseIndex)) {
                activateNotification();
            }
        }
    }

    private void activateNotification() {
        notificationActive = true;
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

    private boolean shouldNotifyLocalClass(int phase) {
        String clazz = getLocalClass();
        // If class parsing is late/missing on some setups, do not miss the first lever reminder.
        if (clazz == null) return true;
        return switch (phase) {
            case 1 -> "Mage".equals(clazz) || "Archer".equals(clazz);
            case 2 -> "Healer".equals(clazz);
            case 3 -> "Archer".equals(clazz);
            default -> false;
        };
    }

    private String getLocalClass() {
        if (mc.player == null) return null;
        String clazz = DungeonUtils.getPlayerClass(mc.player);
        if (clazz == null && mc.player.getGameProfile() != null) {
            clazz = DungeonUtils.getPlayerClass(mc.player.getGameProfile().name());
        }
        return clazz;
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
        waitingGateDestroyed = false;
        notificationActive = false;
        soundPlayedForCurrentGate = false;
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
