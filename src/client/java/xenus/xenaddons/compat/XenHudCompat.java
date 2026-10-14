package xenus.xenaddons.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import xenus.xenaddons.features.impl.misc.CooldownItemsTracker;

public final class XenHudCompat {
    private XenHudCompat() {}

    public static void renderHotbarCooldown(GuiGraphicsExtractor context, int x, int y, Player player, ItemStack stack) {
        CooldownItemsTracker.renderBackground(context, stack, x, y, false);
        Font textRenderer = Minecraft.getInstance().font;
        for (CooldownItemsTracker.OverlayText overlay : CooldownItemsTracker.getOverlayTexts(stack, false)) {
            int ox = x + (overlay.alternativePosition() ? -8 : 0);
            int oy = y + (overlay.alternativePosition() ? -10 : 0);
            int width = textRenderer.width(overlay.text());
            int drawX = ox + 19 - 2 - width;
            int drawY = oy + 9;
            XenDrawContextCompat.text(context, textRenderer, overlay.text(), drawX, drawY, 0xFF000000 | overlay.colorRgb());
        }
    }

    public static void pushAutoPetTitleScale(GuiGraphicsExtractor context, Component subtitle) {
        if (!isAutoPetSubtitle(subtitle)) return;
        context.pose().pushMatrix();
        context.pose().scale(0.4f, 0.4f);
    }

    public static void popAutoPetTitleScale(GuiGraphicsExtractor context, Component subtitle) {
        if (!isAutoPetSubtitle(subtitle)) return;
        context.pose().popMatrix();
    }

    private static boolean isAutoPetSubtitle(Component subtitle) {
        return subtitle != null && subtitle.getString().contains("Autopet");
    }
}
