package xenus.xenaddons.config;

import net.minecraft.text.Text;

import java.util.Map;
import java.util.WeakHashMap;

public final class VisualWordsChatStore {
    private static final Map<Text, Text> ORIGINAL_BY_REPLACED = new WeakHashMap<>();

    private VisualWordsChatStore() {}

    public static void put(Text replaced, Text original) {
        if (replaced == null || original == null) return;
        synchronized (ORIGINAL_BY_REPLACED) {
            ORIGINAL_BY_REPLACED.put(replaced, original);
        }
    }

    public static Text getOriginalOrNull(Text replaced) {
        if (replaced == null) return null;
        synchronized (ORIGINAL_BY_REPLACED) {
            return ORIGINAL_BY_REPLACED.get(replaced);
        }
    }
}

