package xenus.xenaddons.hud;

import net.minecraft.network.chat.Component;

public final class PetNotification {
    private Component title = Component.empty();
    private Component subtitle = Component.empty();
    private int ticksLeft = 0;

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
