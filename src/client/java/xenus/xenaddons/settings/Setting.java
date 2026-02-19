package xenus.xenaddons.settings;

public class Setting<T> {
    public final String name;
    protected T value;
    private final T defaultValue;

    public Setting(String name, T defaultValue) {
        this.name = name;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public T getValue() {
        return value;
    }

    private java.util.function.Supplier<Boolean> visibility = () -> true;

    public void setVisibility(java.util.function.Supplier<Boolean> visibility) {
        this.visibility = visibility;
    }

    public boolean isVisible() {
        if (this.parent != null) {
            // First check if parent is disabled (if boolean)
            if (this.parent instanceof BooleanSetting) {
                BooleanSetting boolParent = (BooleanSetting) this.parent;
                // If parent disabled, we hide
                if (!boolParent.getValue()) return false;
                // If parent NOT expanded, we hide
                if (!boolParent.expanded) return false;
            }
        }
        return visibility.get();
    }
    
    // Sub-settings support
    private Setting<?> parent = null;
    public void setParent(Setting<?> parent) {
        this.parent = parent;
    }

    public Setting<?> getParentSetting() {
        return parent;
    }
    
    public boolean isSubSetting() {
        return parent != null;
    }

    private java.util.function.Consumer<T> listener = null;

    public void setListener(java.util.function.Consumer<T> listener) {
        this.listener = listener;
    }

    public void setValue(T value) {
        this.value = value;
        if (listener != null) listener.accept(value);
    }
}
