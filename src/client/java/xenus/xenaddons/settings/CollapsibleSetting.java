package xenus.xenaddons.settings;

import java.util.ArrayList;
import java.util.List;

public class CollapsibleSetting extends Setting<Boolean> {
    private final List<Setting<?>> childSettings = new ArrayList<>();
    
    public CollapsibleSetting(String name) {
        super(name, false); // false = collapsed by default
    }
    
    public void addChild(Setting<?> setting) {
        childSettings.add(setting);
    }
    
    public List<Setting<?>> getChildren() {
        return childSettings;
    }
    
    public boolean isExpanded() {
        return getValue();
    }
    
    public void toggle() {
        setValue(!getValue());
    }
}
