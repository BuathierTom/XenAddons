package xenus.xenaddons.features;

import net.minecraft.client.Minecraft;

public abstract class Module {
    public final String name;
    public final String description;
    public final Category category;
    private boolean enabled = false;
    protected final Minecraft mc = Minecraft.getInstance();
    public final java.util.List<xenus.xenaddons.settings.Setting<?>> settings = new java.util.ArrayList<>();

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isToggleable() {
        return true;
    }

    public void setEnabled(boolean enabled) {
        if (!enabled && !isToggleable()) return;
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) onEnable();
            else onDisable();
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void onEnable() {}
    public void onDisable() {}
    
    // Simple way to handle click action if it's a button-like module
    public void onClick() {
        toggle();
    }

    protected void registerSetting(xenus.xenaddons.settings.Setting<?> setting) {
        settings.add(setting);
    }
}
