package xenus.xenaddons.compat;

import net.minecraft.util.Identifier;

public final class XenIdentifierCompat {
    private XenIdentifierCompat() {}

    public static Identifier of(String namespace, String path) {
        return Identifier.of(namespace, path);
    }

    public static Identifier xen(String path) {
        return of("xenaddons", path);
    }

    public static Identifier minecraft(String path) {
        return of("minecraft", path);
    }
}
