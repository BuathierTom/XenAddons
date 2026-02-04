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

    public void setValue(T value) {
        this.value = value;
    }
}
