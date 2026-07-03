package xenus.xenaddons.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import xenus.xenaddons.mixin.client.AccessorPlayerListHud;

public final class XenPlayerListCompat {
    private XenPlayerListCompat() {}

    public static Component getHeader(Minecraft client) {
        PlayerTabOverlay playerListHud = client.gui.getTabList();
        return ((AccessorPlayerListHud) playerListHud).getHeader();
    }

    public static Component getFooter(Minecraft client) {
        PlayerTabOverlay playerListHud = client.gui.getTabList();
        return ((AccessorPlayerListHud) playerListHud).getFooter();
    }
}
