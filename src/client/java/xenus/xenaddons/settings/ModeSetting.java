package xenus.xenaddons.settings;

import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting<String> {
    public final List<String> modes;
    public int index;

    public ModeSetting(String name, String defaultMode, String... modes) {
        super(name, defaultMode);
        this.modes = Arrays.asList(modes);
        this.index = this.modes.indexOf(defaultMode);
    }
    
    public ModeSetting(String name, String defaultMode, List<String> modes) {
        super(name, defaultMode);
        this.modes = modes;
        this.index = this.modes.indexOf(defaultMode);
    }

    public void cycle() {
        if (index < modes.size() - 1) {
            index++;
        } else {
            index = 0;
        }
        this.setValue(modes.get(index));
    }

    @Override
    public void setValue(String value) {
        if (modes.contains(value)) {
            this.index = modes.indexOf(value);
            super.setValue(value);
        }
    }
}
