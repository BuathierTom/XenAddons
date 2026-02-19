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
        modules.add(new xenus.xenaddons.features.impl.misc.Cursor189Module());
        modules.add(new xenus.xenaddons.features.impl.misc.CustomSizeModule());
        modules.add(new xenus.xenaddons.features.impl.misc.CopyChatModule());
        modules.add(new xenus.xenaddons.features.impl.misc.ItemPriceModule());
        modules.add(new xenus.xenaddons.features.impl.misc.SpotifyOverlayModule());
        
        modules.add(new xenus.xenaddons.features.impl.dungeons.AutoPetRuleTitleModule());
        modules.add(new xenus.xenaddons.features.impl.dungeons.PosMsgModule());
        modules.add(new xenus.xenaddons.features.impl.dungeons.PosTitlesModule());
        modules.add(new xenus.xenaddons.features.impl.dungeons.SecretRoutesModule());
        modules.add(new xenus.xenaddons.features.impl.dungeons.PlayersLeapedModule());
        
        modules.add(new xenus.xenaddons.features.impl.misc.CommandAliasesModule());
        modules.add(new xenus.xenaddons.features.impl.misc.CommandKeybindsModule());
        modules.add(new xenus.xenaddons.features.impl.dev.EditHudModule());
        modules.add(new xenus.xenaddons.features.impl.dev.DebugModule()); // Register DebugModule
        
        xenus.xenaddons.features.impl.misc.zguegaddons.ZguegAddonsFeatures.init();
        xenus.xenaddons.features.impl.misc.CommandAliasesModule.init();
    }

    public static List<Module> getModulesByCategory(Category category) {
        return modules.stream()
                .filter(m -> m.category == category)
                .sorted(java.util.Comparator.comparing(m -> m.name))
                .collect(Collectors.toList());
    }

    public static Module getModule(String name) {
        return modules.stream().filter(m -> m.name.equalsIgnoreCase(name)).findFirst().orElse(null);
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends Module> T getModule(Class<T> clazz) {
        for (Module module : modules) {
            if (clazz.isInstance(module)) {
                return (T) module;
            }
        }
        return null;
    }
}
