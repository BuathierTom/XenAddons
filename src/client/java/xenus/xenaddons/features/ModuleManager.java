package xenus.xenaddons.features;

import xenus.xenaddons.features.impl.misc.InvButtonsModule;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {
    public static final List<Module> modules = new ArrayList<>();
    public static xenus.xenaddons.features.impl.dev.ColorsModule colors;

    public static void init() {
        colors = new xenus.xenaddons.features.impl.dev.ColorsModule();
        modules.add(colors);
        modules.add(new InvButtonsModule());
        modules.add(new xenus.xenaddons.features.impl.misc.zguegaddons.ZguegAddonsModule());
        
        modules.add(new xenus.xenaddons.features.impl.dungeons.AutoPetRuleTitleModule());
        modules.add(new xenus.xenaddons.features.impl.dungeons.PosMsgModule());
        modules.add(new xenus.xenaddons.features.impl.dungeons.PosTitlesModule());
        
        xenus.xenaddons.features.impl.misc.zguegaddons.ZguegAddonsFeatures.init();
    }

    public static List<Module> getModulesByCategory(Category category) {
        return modules.stream().filter(m -> m.category == category).collect(Collectors.toList());
    }

    public static Module getModule(String name) {
        return modules.stream().filter(m -> m.name.equalsIgnoreCase(name)).findFirst().orElse(null);
    }
}
