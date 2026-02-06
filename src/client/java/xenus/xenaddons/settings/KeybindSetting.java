package xenus.xenaddons.settings;

public class KeybindSetting extends Setting<Integer> {
    
    public KeybindSetting(String name, int defaultKey) {
        super(name, defaultKey);
    }
    
    public int getKey() {
        return getValue();
    }
    
    public void setKey(int key) {
        setValue(key);
    }
}
