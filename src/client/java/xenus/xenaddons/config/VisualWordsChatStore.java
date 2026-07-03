package xenus.xenaddons.config;

import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.WeakHashMap;

public final class VisualWordsChatStore {
    private static final Map<Component, Component> ORIGINAL_BY_REPLACED = new WeakHashMap<>();

    private VisualWordsChatStore() {}

    public static void put(Component replaced, Component original) {
        if (replaced == null || original == null) return;
        synchronized (ORIGINAL_BY_REPLACED) {
            ORIGINAL_BY_REPLACED.put(replaced, original);
        }
    }

    public static Component getOriginalOrNull(Component replaced) {
        if (replaced == null) return null;
        synchronized (ORIGINAL_BY_REPLACED) {
            return ORIGINAL_BY_REPLACED.get(replaced);
        }
    }
}

