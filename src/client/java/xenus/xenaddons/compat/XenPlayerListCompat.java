package xenus.xenaddons.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.text.Text;
import xenus.xenaddons.mixin.client.AccessorPlayerListHud;

public final class XenPlayerListCompat {
    private XenPlayerListCompat() {}

    public static Text getHeader(MinecraftClient client) {
        PlayerListHud playerListHud = client.inGameHud.getPlayerListHud();
        return ((AccessorPlayerListHud) playerListHud).getHeader();
    }

    public static Text getFooter(MinecraftClient client) {
        PlayerListHud playerListHud = client.inGameHud.getPlayerListHud();
        return ((AccessorPlayerListHud) playerListHud).getFooter();
    }
}
