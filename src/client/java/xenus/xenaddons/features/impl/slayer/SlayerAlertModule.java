package xenus.xenaddons.features.impl.slayer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.hud.HudElement;
import xenus.xenaddons.hud.HudRender;
import xenus.xenaddons.settings.ActionSetting;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.settings.ModeSetting;
import xenus.xenaddons.settings.NumberSetting;
import xenus.xenaddons.settings.StringSetting;
import xenus.xenaddons.utils.AlertSounds;

/**
 * Shared shape of the slayer alerts: an editable line of text flashed on the HUD plus a ping.
 * Subclasses only decide what triggers it.
 */
public abstract class SlayerAlertModule extends Module {

    private static final int HUD_TICKS = 40;

    private final String defaultText;

    private final BooleanSetting showHud = new BooleanSetting("Alert HUD", true);
    private final StringSetting alertText;
    private final BooleanSetting playSound = new BooleanSetting("Play sound", true);
    private final ModeSetting alertSound = new ModeSetting("Alert Sound", AlertSounds.DEFAULT, AlertSounds.NAMES);
    private final NumberSetting soundVolume = new NumberSetting("Sound Volume", 2.0, 0.0, 5.0, 0.1);
    private final NumberSetting soundPitch = new NumberSetting("Sound Pitch", 0.9, 0.0, 2.0, 0.1);
    private final ActionSetting notePlay = new ActionSetting("Note play", this::playAlertSound);

    private int hudTicks = 0;

    protected SlayerAlertModule(String name, String description, String defaultText, float defaultY, float defaultScale) {
        super(name, description, Category.SLAYER);

        this.defaultText = defaultText;
        this.alertText = new StringSetting("Alert Component", defaultText, 64);

        this.registerSetting(showHud);
        this.registerSetting(alertText);
        this.registerSetting(playSound);
        this.registerSetting(alertSound);
        this.registerSetting(soundVolume);
        this.registerSetting(soundPitch);
        this.registerSetting(notePlay);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!this.isEnabled()) {
                hudTicks = 0;
                return;
            }
            if (hudTicks > 0) hudTicks--;
        });

        hudElement(name, (context, example) -> HudRender.centeredText(context, getAlertText(), 0xFFFFFFFF))
            .enabledWhen(() -> isEnabled() && showHud.getValue())
            .visibleWhen(() -> hudTicks > 0)
            .centered()
            .defaults(HudElement.CENTER, defaultY, defaultScale);
    }

    /** Flashes the HUD line and plays the ping, honouring both toggles. */
    protected final void fireAlert() {
        if (showHud.getValue()) hudTicks = HUD_TICKS;
        if (playSound.getValue()) playAlertSound();
    }

    /** The configured text with '&' colour codes resolved, like the other editable HUD texts. */
    public String getAlertText() {
        String text = alertText.getValue();
        if (text == null || text.isBlank()) text = defaultText;
        return text.replace('&', '§');
    }

    private void playAlertSound() {
        AlertSounds.play(alertSound.getValue(), soundVolume.getValue(), soundPitch.getValue());
    }
}
