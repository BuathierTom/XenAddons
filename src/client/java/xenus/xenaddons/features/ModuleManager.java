package xenus.xenaddons.features;

import xenus.xenaddons.features.impl.misc.InvButtonsModule;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {
    public static final List<Module> modules = new ArrayList<>();

    public static void init() {
        modules.add(new InvButtonsModule());
        // Add more modules here
    }

    public static List<Module> getModulesByCategory(Category category) {
        return modules.stream().filter(m -> m.category == category).collect(Collectors.toList());
    }
}
