package xenus.xenaddons.features.impl.dungeons;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
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

public class MilestoneDisplayModule extends Module {
    private static final int REMINDER_INTERVAL_SECONDS = 30;
    private static final int MIN_SAFE_MILESTONE = 3;
    private static final int REMINDER_TITLE_TICKS = 40;
    private static final SoundEvent INVISIBILITY_TIMER_SOUND =
            SoundEvent.of(Identifier.of("minecraft", "entity.experience_orb.pickup"));
    private static final Identifier[] RANDOM_SOUND_POOL = new Identifier[]{
            Identifier.of("xenaddons", "reminder_gate.fah"),
            Identifier.of("xenaddons", "reminder_gate.nuke_bomb"),
            Identifier.of("xenaddons", "reminder_gate.android_meme"),
            Identifier.of("xenaddons", "reminder_gate.pump_shotgun"),
            Identifier.of("xenaddons", "reminder_gate.knocked"),
            Identifier.of("xenaddons", "reminder_gate.hog_rider"),
            Identifier.of("xenaddons", "reminder_gate.airhorn")
    };

    private final BooleanSetting showNotification = new BooleanSetting("Show Notification", true);
    private final BooleanSetting showHud = new BooleanSetting("Show HUD", true);
    private final BooleanSetting enableCustomSound = new BooleanSetting("Enable Custom Sound", false);
    private final StringSetting reminderText = new StringSetting("Reminder Text", "&cMilestone low!", 64);
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
    private final NumberSetting hudY = new NumberSetting("HUD Y", 30, -1000, 1000, 1);
    private final NumberSetting hudScale = new NumberSetting("HUD Scale", 1.0, 0.5, 6.0, 0.05);
    private final NumberSetting alertHudOffsetX = new NumberSetting("Alert HUD Offset X", 0, -1000, 1000, 1);
    private final NumberSetting alertHudY = new NumberSetting("Alert HUD Y", 120, -1000, 1000, 1);
    private final NumberSetting alertHudScale = new NumberSetting("Alert HUD Scale", 4.0, 0.5, 6.0, 0.05);

    private int milestoneCount = -1;
    private int runTicks = 0;
    private int lastSyncedDungeonSeconds = -1;
    private int nextReminderAtSeconds = REMINDER_INTERVAL_SECONDS;
    private int alertTicks = 0;
    private String alertText = null;
    private int debugAlertTicks = 0;
    private String debugAlertText = null;

    public MilestoneDisplayModule() {
        super("Milestone Reminder", "Displays your dungeon milestone and reminds you when it is below 3", Category.DUNGEON);

        this.registerSetting(showNotification);
        this.registerSetting(showHud);
        this.registerSetting(enableCustomSound);
        this.registerSetting(reminderText);
        sound.setVisibility(enableCustomSound::getValue);
        playSound.setVisibility(enableCustomSound::getValue);
        this.registerSetting(sound);
        this.registerSetting(playSound);
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

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (alertTicks > 0) alertTicks--;
            if (alertTicks <= 0) alertText = null;
            if (debugAlertTicks > 0) debugAlertTicks--;
            if (debugAlertTicks <= 0) debugAlertText = null;
            if (!this.isEnabled()) {
                resetAllRuntime();
                return;
            }
            updateRuntime();
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (!this.isEnabled()) return;
            renderInGameHud(drawContext);
            renderReminderAlert(drawContext);
        });
    }

    private void updateRuntime() {
        if (mc.world == null || mc.player == null || !DungeonUtils.inDungeon()) {
            resetRuntime();
            return;
        }

        milestoneCount = DungeonUtils.getMilestone();

        int dungeonSeconds = DungeonUtils.dungeonSeconds;
        if (dungeonSeconds >= 0) {
            if (lastSyncedDungeonSeconds >= 0 && dungeonSeconds < lastSyncedDungeonSeconds) {
                resetRuntime();
                milestoneCount = DungeonUtils.getMilestone();
            }
            int syncedTicks = dungeonSeconds * 20;
            if (syncedTicks > runTicks) {
                runTicks = syncedTicks;
            }
            lastSyncedDungeonSeconds = dungeonSeconds;
        }

        runTicks++;
        int currentSeconds = runTicks / 20;
        while (currentSeconds >= nextReminderAtSeconds) {
            if (shouldReminderFire()) {
                showReminder();
            }
            nextReminderAtSeconds += REMINDER_INTERVAL_SECONDS;
        }
    }

    private void showReminder() {
        if (!showNotification.getValue()) return;
        if (mc.player == null) return;

        String display = milestoneCount >= 0 ? Integer.toString(milestoneCount) : "?";
        alertText = formatReminderText(display);
        alertTicks = REMINDER_TITLE_TICKS;
        playReminderSound();
    }

    public void renderInGameHud(DrawContext drawContext) {
        if (!showHud.getValue()) return;
        if (mc.world == null || mc.player == null || !DungeonUtils.inDungeon()) return;
        renderLine(drawContext, getDisplayText(), getDisplayColor());
    }

    public void renderPreview(DrawContext drawContext) {
        renderLine(drawContext, getPreviewText(), getPreviewColor());
    }

    public void debugTriggerReminder() {
        if (!this.isEnabled()) this.setEnabled(true);
        showNotification.setValue(true);
        milestoneCount = 0;
        debugAlertText = formatReminderText("0");
        debugAlertTicks = REMINDER_TITLE_TICKS;
        playReminderSound();
    }

    private void renderReminderAlert(DrawContext drawContext) {
        if (debugAlertTicks > 0 && debugAlertText != null && !debugAlertText.isBlank()) {
            renderAlertLine(drawContext, debugAlertText, 0xFFFF5555);
            return;
        }
        if (alertTicks <= 0 || alertText == null || alertText.isBlank()) return;
        renderAlertLine(drawContext, alertText, 0xFFFF5555);
    }

    private boolean shouldReminderFire() {
        return milestoneCount >= 0 && milestoneCount < MIN_SAFE_MILESTONE;
    }

    private String formatReminderText(String milestoneDisplay) {
        String text = reminderText.getValue();
        if (text == null || text.isBlank()) {
            text = "&cMilestone low!";
        }
        return text.replace('&', '§').replace("{milestone}", milestoneDisplay);
    }

    private String getDisplayText() {
        String value = milestoneCount >= 0 ? Integer.toString(milestoneCount) : "?";
        return "§bMilestone§f: " + getMilestoneColor() + value;
    }

    private String getPreviewText() {
        return "§bMilestone§f: §c2";
    }

    private int getDisplayColor() {
        if (milestoneCount < 0) return 0xFFB0B0B0;
        if (milestoneCount < MIN_SAFE_MILESTONE) return 0xFFFF5555;
        return 0xFF55FF55;
    }

    private int getPreviewColor() {
        return 0xFFFF5555;
    }

    private String getMilestoneColor() {
        if (milestoneCount < 0) return "§7";
        if (milestoneCount < MIN_SAFE_MILESTONE) return "§c";
        return "§a";
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

    private void renderAlertLine(DrawContext drawContext, String line, int color) {
        if (line == null || line.isBlank()) return;

        int width = mc.getWindow().getScaledWidth();
        int textWidth = mc.textRenderer.getWidth(line);
        int textHeight = mc.textRenderer.fontHeight;

        float scale = (float) alertHudScale.getValue().doubleValue();
        float inv = 1.0f / scale;
        int centerXRaw = (width / 2) + alertHudOffsetX.getValue().intValue();
        int yRaw = alertHudY.getValue().intValue();

        int minCenterX = (int) Math.ceil((textWidth * scale) / 2.0f) + 2;
        int maxCenterX = width - minCenterX;
        int centerX = MathHelper.clamp(centerXRaw, minCenterX, Math.max(minCenterX, maxCenterX));

        int maxY = mc.getWindow().getScaledHeight() - (int) Math.ceil(textHeight * scale) - 2;
        int y = MathHelper.clamp(yRaw, 2, Math.max(2, maxY));

        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().scale(scale, scale);
        int x = (int) ((centerX * inv) - (textWidth / 2f));
        int drawY = (int) (y * inv);
        drawContext.drawTextWithShadow(mc.textRenderer, line, x, drawY, color);
        drawContext.getMatrices().popMatrix();
    }

    private void resetRuntime() {
        milestoneCount = -1;
        runTicks = 0;
        lastSyncedDungeonSeconds = -1;
        nextReminderAtSeconds = REMINDER_INTERVAL_SECONDS;
        alertTicks = 0;
        alertText = null;
    }

    private void resetAllRuntime() {
        resetRuntime();
        debugAlertTicks = 0;
        debugAlertText = null;
    }

    private void playSelectedSoundPreview() {
        playReminderSound();
    }

    private void playReminderSound() {
        if (mc.player == null) return;
        SoundEvent event = resolveReminderSound();
        if (event == null) return;
        mc.player.playSound(event, 8.0f, 1.0f);
    }

    private SoundEvent resolveReminderSound() {
        if (!enableCustomSound.getValue()) {
            return INVISIBILITY_TIMER_SOUND;
        }

        Identifier id = switch (sound.getValue() == null ? "" : sound.getValue()) {
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
                Identifier parsed = Identifier.tryParse(sound.getValue());
                yield parsed != null ? parsed : Identifier.of("xenaddons", "reminder_gate.fah");
            }
        };
        return SoundEvent.of(id);
    }

    @Override
    public void onDisable() {
        resetAllRuntime();
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

    public int getAlertHudOffsetX() {
        return alertHudOffsetX.getValue().intValue();
    }

    public int getAlertHudY() {
        return alertHudY.getValue().intValue();
    }

    public double getAlertHudScale() {
        return alertHudScale.getValue();
    }

    public boolean isShowNotification() {
        return showNotification.getValue();
    }

    public boolean isShowHud() {
        return showHud.getValue();
    }

    public String getReminderText() {
        return reminderText.getValue();
    }

    public boolean isEnableCustomSound() {
        return enableCustomSound.getValue();
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

    public void setAlertHudOffsetX(int value) {
        alertHudOffsetX.setValue((double) value);
    }

    public void setAlertHudY(int value) {
        alertHudY.setValue((double) value);
    }

    public void setAlertHudScale(double value) {
        alertHudScale.setValue(value);
    }

    public void setShowNotification(boolean value) {
        showNotification.setValue(value);
    }

    public void setShowHud(boolean value) {
        showHud.setValue(value);
    }

    public void setReminderText(String value) {
        reminderText.setValue(value);
    }

    public void setEnableCustomSound(boolean value) {
        enableCustomSound.setValue(value);
    }
}
