package xenus.xenaddons.settings;

import org.lwjgl.glfw.GLFW;

public class KeybindSetting extends Setting<Integer> {
    
    public KeybindSetting(String name, int defaultKey) {
        super(name, defaultKey);
    }
    
    public int getKey() {
        Integer key = getValue();
        if (key == null || key == 0 || key == -1 || key == GLFW.GLFW_KEY_ESCAPE) return -1;
        return key;
    }
    
    public void setKey(int key) {
        setValue(key);
    }

    @Override
    public void setValue(Integer value) {
        int key = value == null ? -1 : value;
        if (key == 0 || key == -1 || key == GLFW.GLFW_KEY_ESCAPE) {
            super.setValue(-1);
            return;
        }
        super.setValue(key);
    }
}
