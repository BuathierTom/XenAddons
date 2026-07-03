package xenus.xenaddons.hud;

import net.minecraft.network.chat.Component;

public final class PetNotification {
    private Component title = Component.empty();
    private Component subtitle = Component.empty();
    private int ticksLeft = 0;

    // Réglages
    public float scale = 2.5f;   // Larger scale to mimic title size (was 0.85f which is tiny)
    public int yOffset = -20;    // ajustable
    public boolean enabled = true;

    public void show(Component title, Component subtitle, int durationTicks) {
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

    public Component getTitle() { return title; }
    public Component getSubtitle() { return subtitle; }
}
