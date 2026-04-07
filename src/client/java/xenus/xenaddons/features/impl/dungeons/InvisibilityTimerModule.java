package xenus.xenaddons.features.impl.dungeons;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.ActionSetting;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.settings.ModeSetting;
import xenus.xenaddons.settings.NumberSetting;
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
    private final NumberSetting hudOffsetX = new NumberSetting("HUD Offset X", 0, -1000, 1000, 1);
    private final NumberSetting hudY = new NumberSetting("HUD Y", 100, -1000, 1000, 1);
    private final NumberSetting hudScale = new NumberSetting("HUD Scale", 1.0, 0.5, 6.0, 0.05);
    private final NumberSetting alertHudOffsetX = new NumberSetting("Alert HUD Offset X", 0, -1000, 1000, 1);
    private final NumberSetting alertHudY = new NumberSetting("Alert HUD Y", 120, -1000, 1000, 1);
    private final NumberSetting alertHudScale = new NumberSetting("Alert HUD Scale", 4.0, 0.5, 6.0, 0.05);
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
        hudOffsetX.setVisibility(() -> false);
        hudY.setVisibility(() -> false);
        hudScale.setVisibility(() -> false);
        alertHudOffsetX.setVisibility(() -> false);
        alertHudY.setVisibility(() -> false);
        alertHudScale.setVisibility(() -> false);
        this.registerSetting(hudOffsetX);
        this.registerSetting(hudY);
        this.registerSetting(hudScale);
        this.registerSetting(alertHudOffsetX);
        this.registerSetting(alertHudY);
        this.registerSetting(alertHudScale);
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

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (!this.isEnabled()) return;
            if (activeTicks > 0 && activeType != null) {
                renderLine(drawContext, getDisplayText(false));
            }
            renderProcAlert(drawContext);
        });
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

    public String getDisplayText(boolean preview) {
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

    private void renderLine(DrawContext drawContext, String line) {
        renderLine(drawContext, line, getDisplayColor(false));
    }

    private void renderLine(DrawContext drawContext, String line, int color) {
        if (line == null || line.isBlank()) return;

        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();
        int textWidth = mc.textRenderer.getWidth(line);
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
        drawContext.drawTextWithShadow(mc.textRenderer, line, x, drawY, color);
        drawContext.getMatrices().popMatrix();
    }

    public void renderPreview(DrawContext drawContext) {
        renderLine(drawContext, getDisplayText(true), getDisplayColor(true));
    }

    public void renderAlertPreview(DrawContext drawContext) {
        renderProcAlertText(drawContext, "Bonzo");
    }

    private void renderProcAlert(DrawContext drawContext) {
        if (!maskProcsAlert.getValue()) return;
        if (alertTicks <= 0 || alertText == null || alertText.isBlank()) return;
        renderProcAlertText(drawContext, alertText);
    }

    private void renderProcAlertText(DrawContext drawContext, String text) {
        int width = mc.getWindow().getScaledWidth();
        int textWidth = mc.textRenderer.getWidth(text);
        float scale = (float) alertHudScale.getValue().doubleValue();
        float inv = 1.0f / scale;
        int centerX = (width / 2) + alertHudOffsetX.getValue().intValue();
        int yRaw = alertHudY.getValue().intValue();
        int x = (int) ((centerX * inv) - (textWidth / 2f));
        int y = (int) (yRaw * inv);

        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().scale(scale, scale);
        drawContext.drawTextWithShadow(mc.textRenderer, text, x, y, 0xFFFF2B2B);
        drawContext.getMatrices().popMatrix();
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
        SoundEvent event = resolveProcSound(procSound.getValue());
        if (event == null) return;
        float volume = procSoundVolume.getValue().floatValue();
        float pitch = procSoundPitch.getValue().floatValue();
        mc.player.playSound(event, volume, pitch);
    }

    private SoundEvent resolveProcSound(String selected) {
        Identifier id = switch (selected == null ? "" : selected) {
            case "Blaze Hurt" -> Identifier.of("minecraft", "entity.blaze.hurt");
            case "Vault Break" -> Identifier.of("minecraft", "block.vault.break");
            case "Elder Hurt" -> Identifier.of("minecraft", "entity.elder_guardian.hurt_land");
            case "Totem Use" -> Identifier.of("minecraft", "item.totem.use");
            case "Sculk Hit" -> Identifier.of("minecraft", "block.sculk_catalyst.hit");
            case "Ender Chest" -> Identifier.of("minecraft", "block.ender_chest.close");
            case "Iron Xylophone" -> Identifier.of("minecraft", "block.note_block.iron_xylophone");
            case "Bell" -> Identifier.of("minecraft", "block.note_block.bell");
            case "Pling" -> Identifier.of("minecraft", "block.note_block.pling");
            case "Default XP Pickup" -> Identifier.of("minecraft", "entity.experience_orb.pickup");
            default -> Identifier.of("minecraft", "entity.experience_orb.pickup");
        };
        return Registries.SOUND_EVENT.get(id);
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

    public int getAlertHudOffsetX() {
        return alertHudOffsetX.getValue().intValue();
    }

    public int getAlertHudY() {
        return alertHudY.getValue().intValue();
    }

    public double getAlertHudScale() {
        return alertHudScale.getValue();
    }

    public void setAlertHudOffsetX(int value) {
        alertHudOffsetX.setValue((double) value);
    }

    public void setAlertHudY(int value) {
        alertHudY.setValue((double) value);
    }

    public void setAlertHudScale(double value) {
        alertHudScale.setValue(value);
    }
}
