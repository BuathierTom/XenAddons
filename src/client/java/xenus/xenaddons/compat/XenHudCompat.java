package xenus.xenaddons.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.features.impl.dungeons.AutoPetRuleTitleModule;
import xenus.xenaddons.features.impl.dungeons.MilestoneDisplayModule;
import xenus.xenaddons.features.impl.misc.CooldownItemsTracker;
import xenus.xenaddons.features.impl.misc.SpotifyOverlayModule;
import xenus.xenaddons.hud.HudState;

public final class XenHudCompat {
    private XenHudCompat() {}

    public static void renderEarlyHudOverlays(GuiGraphicsExtractor context) {
        SpotifyOverlayModule spotify = ModuleManager.getModule(SpotifyOverlayModule.class);
        if (spotify != null) {
            spotify.renderInGameHud(context);
        }

        MilestoneDisplayModule milestoneDisplay = ModuleManager.getModule(MilestoneDisplayModule.class);
        if (milestoneDisplay != null && milestoneDisplay.isEnabled()) {
            milestoneDisplay.renderInGameHud(context);
        }
    }

    public static void renderPetNotification(GuiGraphicsExtractor context) {
        var notif = HudState.PET_NOTIFICATION;
        if (!notif.isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;

        Font textRenderer = mc.font;
        Component title = notif.getTitle();
        Component subtitle = notif.getSubtitle();

        float titleScale = notif.scale;
        int screenWidth = XenDrawContextCompat.scaledWidth(context);
        int screenHeight = XenDrawContextCompat.scaledHeight(context);
        int titleWidth = textRenderer.width(title);
        int centerX = screenWidth / 2;
        int hudOffsetX = 0;
        int hudOffsetY = notif.yOffset;

        AutoPetRuleTitleModule module = ModuleManager.getModule(AutoPetRuleTitleModule.class);
        if (module != null) {
            hudOffsetX = module.getHudOffsetX();
            hudOffsetY = module.getHudOffsetY();
            titleScale = (float) module.getHudScale();
        }

        context.pose().pushMatrix();
        context.pose().scale(titleScale, titleScale);

        int baseYRaw = screenHeight / 2 + hudOffsetY;
        int minCenterX = (int) Math.ceil((titleWidth * titleScale) / 2.0f) + 2;
        int maxCenterX = screenWidth - minCenterX;
        int clampedCenterX = Mth.clamp(centerX + hudOffsetX, minCenterX, Math.max(minCenterX, maxCenterX));
        int maxBaseY = screenHeight - (int) Math.ceil(textRenderer.lineHeight * titleScale) - 2;
        int baseY = Mth.clamp(baseYRaw, 2, Math.max(2, maxBaseY));

        float titleInv = 1.0f / titleScale;
        int drawTitleX = (int) ((clampedCenterX * titleInv) - (titleWidth / 2));
        int drawTitleY = (int) (baseY * titleInv);
        XenDrawContextCompat.text(context, textRenderer, title, drawTitleX, drawTitleY, 0xFFFFFFFF);
        context.pose().popMatrix();

        float subScale = titleScale * 0.6f;
        context.pose().pushMatrix();
        context.pose().scale(subScale, subScale);

        int subtitleWidth = textRenderer.width(subtitle);
        float subInv = 1.0f / subScale;
        int subScreenY = baseY + 25;
        int minSubCenterX = (int) Math.ceil((subtitleWidth * subScale) / 2.0f) + 2;
        int maxSubCenterX = screenWidth - minSubCenterX;
        int subCenterX = Mth.clamp(clampedCenterX, minSubCenterX, Math.max(minSubCenterX, maxSubCenterX));
        int maxSubY = screenHeight - (int) Math.ceil(textRenderer.lineHeight * subScale) - 2;
        subScreenY = Mth.clamp(subScreenY, 2, Math.max(2, maxSubY));

        int drawSubX = (int) ((subCenterX * subInv) - (subtitleWidth / 2));
        int drawSubY = (int) (subScreenY * subInv);
        XenDrawContextCompat.text(context, textRenderer, subtitle, drawSubX, drawSubY, 0xFFFFFFFF);
        context.pose().popMatrix();
    }

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
