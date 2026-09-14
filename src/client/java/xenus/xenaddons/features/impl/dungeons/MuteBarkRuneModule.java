package xenus.xenaddons.features.impl.dungeons;

import net.minecraft.client.resources.sounds.SoundInstance;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.utils.DungeonUtils;

import java.util.Locale;

public class MuteBarkRuneModule extends Module {

    private static final String WOLF_MARKER = "wolf";
    private static final String GROWL_SUFFIX = ".growl";
    private static final String BARK_SUFFIX = ".ambient";

    public final BooleanSetting onlyInDungeons = new BooleanSetting("Only in Dungeons", true);

    public MuteBarkRuneModule() {
        super("Mute Bark Rune", "Mutes the wolf growl and bark sounds played by the Bark Rune", Category.DUNGEON);
        this.registerSetting(onlyInDungeons);
    }

    public static boolean shouldMute(SoundInstance sound) {
        MuteBarkRuneModule module = ModuleManager.getModule(MuteBarkRuneModule.class);
        if (module == null || !module.isEnabled()) return false;
        if (module.onlyInDungeons.getValue() && !DungeonUtils.inDungeon()) return false;

        return isWolfBarkOrGrowl(safeId(sound));
    }

    static boolean isWolfBarkOrGrowl(String soundId) {
        if (soundId == null || soundId.isBlank()) return false;

        String id = soundId.toLowerCase(Locale.ROOT);
        if (!id.contains(WOLF_MARKER)) return false;

        return id.endsWith(GROWL_SUFFIX) || id.endsWith(BARK_SUFFIX);
    }

    private static String safeId(SoundInstance sound) {
        if (sound == null) return null;
        try {
            if (sound.getIdentifier() == null) return null;
            return sound.getIdentifier().toString();
        } catch (Throwable ignored) {
            return null;
        }
    }
}
