package xenus.xenaddons.features.impl.slayer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
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
    private final NumberSetting hudOffsetX = new NumberSetting("HUD Offset X", 0, -1000, 1000, 1);
    private final NumberSetting hudY;
    private final NumberSetting hudScale;

    private int hudTicks = 0;

    protected SlayerAlertModule(String name, String description, String hudId, String defaultText, int defaultY, double defaultScale) {
        super(name, description, Category.SLAYER);

        this.defaultText = defaultText;
        this.alertText = new StringSetting("Alert Component", defaultText, 64);
        this.hudY = new NumberSetting("HUD Y", defaultY, -1000, 1000, 1);
        this.hudScale = new NumberSetting("HUD Scale", defaultScale, 0.5, 6.0, 0.05);

        this.registerSetting(showHud);
        this.registerSetting(alertText);
        this.registerSetting(playSound);
        this.registerSetting(alertSound);
        this.registerSetting(soundVolume);
        this.registerSetting(soundPitch);
        this.registerSetting(notePlay);
        hudOffsetX.setVisibility(() -> false);
        hudY.setVisibility(() -> false);
        hudScale.setVisibility(() -> false);
        this.registerSetting(hudOffsetX);
        this.registerSetting(hudY);
        this.registerSetting(hudScale);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!this.isEnabled()) {
                hudTicks = 0;
                return;
            }
            if (hudTicks > 0) hudTicks--;
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("xenaddons", hudId), (drawContext, tickDelta) -> {
            if (!this.isEnabled() || !showHud.getValue()) return;
            if (hudTicks <= 0) return;
            renderText(drawContext, getAlertText());
        });
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

    private void renderText(GuiGraphicsExtractor drawContext, String text) {
        if (text == null || text.isEmpty()) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int textWidth = mc.font.width(text);

        float scale = (float) hudScale.getValue().doubleValue();
        float inv = 1.0f / scale;

        int minCenterX = (int) Math.ceil((textWidth * scale) / 2.0f) + 2;
        int maxCenterX = screenWidth - minCenterX;
        int centerX = Mth.clamp((screenWidth / 2) + hudOffsetX.getValue().intValue(), minCenterX, Math.max(minCenterX, maxCenterX));

        int maxY = screenHeight - (int) Math.ceil(mc.font.lineHeight * scale) - 2;
        int y = Mth.clamp(hudY.getValue().intValue(), 2, Math.max(2, maxY));

        drawContext.pose().pushMatrix();
        drawContext.pose().scale(scale, scale);
        drawContext.text(mc.font, text, (int) ((centerX * inv) - (textWidth / 2f)), (int) (y * inv), 0xFFFFFFFF, true);
        drawContext.pose().popMatrix();
    }

    public void renderPreview(GuiGraphicsExtractor drawContext) {
        renderText(drawContext, getAlertText());
    }

    /** The alert HUD only draws when this is on, so the HUD editor hides it too. */
    public boolean isHudEnabled() {
        return showHud.getValue();
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
