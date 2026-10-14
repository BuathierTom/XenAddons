package xenus.xenaddons.features.impl.dungeons;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.network.chat.Component;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.hud.HudElement;
import xenus.xenaddons.hud.HudRender;
import xenus.xenaddons.settings.ActionSetting;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.settings.ModeSetting;
import xenus.xenaddons.settings.StringSetting;
import xenus.xenaddons.utils.DungeonUtils;

import java.util.concurrent.ThreadLocalRandom;

public class MilestoneDisplayModule extends Module {
    private static final int REMINDER_INTERVAL_SECONDS = 30;
    private static final int MIN_SAFE_MILESTONE = 3;
    private static final int REMINDER_TITLE_TICKS = 40;
    private static final SoundEvent INVISIBILITY_TIMER_SOUND =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("minecraft", "entity.experience_orb.pickup"));
    private static final Identifier[] RANDOM_SOUND_POOL = new Identifier[]{
            Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.fah"),
            Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.nuke_bomb"),
            Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.android_meme"),
            Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.pump_shotgun"),
            Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.knocked"),
            Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.hog_rider"),
            Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.airhorn")
    };

    private final BooleanSetting showNotification = new BooleanSetting("Show Notification", true);
    private final BooleanSetting showHud = new BooleanSetting("Show HUD", true);
    private final BooleanSetting enableCustomSound = new BooleanSetting("Enable Custom Sound", false);
    private final StringSetting reminderText = new StringSetting("Reminder Component", "&cMilestone low!", 64);
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

    private int milestoneCount = -1;
    private int runTicks = 0;
    private int nextReminderAtSeconds = REMINDER_INTERVAL_SECONDS;
    private boolean timerArmed = false;
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

        hudElement("Milestone Display", (context, example) -> example
            ? HudRender.centeredText(context, getPreviewText(), getPreviewColor())
            : HudRender.centeredText(context, getDisplayText(), getDisplayColor()))
            .enabledWhen(() -> isEnabled() && showHud.getValue())
            .visibleWhen(() -> mc.level != null && mc.player != null && DungeonUtils.inDungeon())
            .centered()
            .defaults(HudElement.CENTER, 30.0f, 1.0f);

        hudElement("Milestone Alert", (context, example) ->
            HudRender.centeredText(context, example ? formatReminderText("2") : getActiveAlertText(), 0xFFFF5555))
            .visibleWhen(() -> getActiveAlertText() != null)
            .centered()
            .defaults(HudElement.CENTER, 120.0f, 4.0f);
    }

    private void updateRuntime() {
        if (mc.level == null || mc.player == null || !DungeonUtils.inDungeon()) {
            resetRuntime();
            return;
        }

        int previousMilestone = milestoneCount;
        milestoneCount = DungeonUtils.getMilestone();

        if (!timerArmed) {
            if (previousMilestone < 0 && milestoneCount == 0) {
                timerArmed = true;
                runTicks = 0;
                nextReminderAtSeconds = REMINDER_INTERVAL_SECONDS;
            } else {
                return;
            }
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

    public void debugTriggerReminder() {
        if (!this.isEnabled()) this.setEnabled(true);
        showNotification.setValue(true);
        milestoneCount = 0;
        debugAlertText = formatReminderText("0");
        debugAlertTicks = REMINDER_TITLE_TICKS;
        playReminderSound();
    }

    private String getActiveAlertText() {
        if (debugAlertTicks > 0 && debugAlertText != null && !debugAlertText.isBlank()) return debugAlertText;
        if (alertTicks <= 0 || alertText == null || alertText.isBlank()) return null;
        return alertText;
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

    private void resetRuntime() {
        milestoneCount = -1;
        runTicks = 0;
        nextReminderAtSeconds = REMINDER_INTERVAL_SECONDS;
        timerArmed = false;
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
            case "Nuke Bomb" -> Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.nuke_bomb");
            case "Android Meme" -> Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.android_meme");
            case "Pump Shotgun" -> Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.pump_shotgun");
            case "Knocked" -> Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.knocked");
            case "Hog Rider" -> Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.hog_rider");
            case "Airhorn" -> Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.airhorn");
            case "FAHHHHHHHHHHHHHH" -> Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.fah");
            default -> {
                Identifier parsed = Identifier.tryParse(sound.getValue());
                yield parsed != null ? parsed : Identifier.fromNamespaceAndPath("xenaddons", "reminder_gate.fah");
            }
        };
        return SoundEvent.createVariableRangeEvent(id);
    }

    @Override
    public void onDisable() {
        resetAllRuntime();
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
