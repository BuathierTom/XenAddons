package xenus.xenaddons.settings;

public class BooleanSetting extends Setting<Boolean> {
    public boolean expanded = false; // For GUI expansion state

    public BooleanSetting(String name, Boolean defaultValue) {
        super(name, defaultValue);
    }
    
    public void toggle() {
        this.value = !this.value;
    }
}
