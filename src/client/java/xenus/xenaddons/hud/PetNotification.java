package xenus.xenaddons.hud;

import net.minecraft.text.Text;

public final class PetNotification {
    private Text title = Text.empty();
    private Text subtitle = Text.empty();
    private int ticksLeft = 0;

    // Réglages
    public float scale = 2.5f;   // Larger scale to mimic title size (was 0.85f which is tiny)
    public int yOffset = -20;    // ajustable
    public boolean enabled = true;

    public void show(Text title, Text subtitle, int durationTicks) {
        this.title = title;
        this.subtitle = subtitle;
        this.ticksLeft = durationTicks;
    }

    public void tick() {
        if (ticksLeft > 0) ticksLeft--;
    }

    public boolean isActive() {
        return enabled && ticksLeft > 0;
    }

    public Text getTitle() { return title; }
    public Text getSubtitle() { return subtitle; }
}
