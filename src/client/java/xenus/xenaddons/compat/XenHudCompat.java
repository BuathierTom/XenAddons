package xenus.xenaddons.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.features.impl.dungeons.AutoPetRuleTitleModule;
import xenus.xenaddons.features.impl.dungeons.MilestoneDisplayModule;
import xenus.xenaddons.features.impl.misc.CooldownItemsTracker;
import xenus.xenaddons.features.impl.misc.SpotifyOverlayModule;
import xenus.xenaddons.hud.HudState;

public final class XenHudCompat {
    private XenHudCompat() {}

    public static void renderEarlyHudOverlays(DrawContext context) {
        SpotifyOverlayModule spotify = ModuleManager.getModule(SpotifyOverlayModule.class);
        if (spotify != null) {
            spotify.renderInGameHud(context);
        }

        MilestoneDisplayModule milestoneDisplay = ModuleManager.getModule(MilestoneDisplayModule.class);
        if (milestoneDisplay != null && milestoneDisplay.isEnabled()) {
            milestoneDisplay.renderInGameHud(context);
        }
    }

    public static void renderPetNotification(DrawContext context) {
        var notif = HudState.PET_NOTIFICATION;
        if (!notif.isActive()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;

        TextRenderer textRenderer = mc.textRenderer;
        Text title = notif.getTitle();
        Text subtitle = notif.getSubtitle();

        float titleScale = notif.scale;
        int screenWidth = XenDrawContextCompat.scaledWidth(context);
        int screenHeight = XenDrawContextCompat.scaledHeight(context);
        int titleWidth = textRenderer.getWidth(title);
        int centerX = screenWidth / 2;
        int hudOffsetX = 0;
        int hudOffsetY = notif.yOffset;

        AutoPetRuleTitleModule module = ModuleManager.getModule(AutoPetRuleTitleModule.class);
        if (module != null) {
            hudOffsetX = module.getHudOffsetX();
            hudOffsetY = module.getHudOffsetY();
            titleScale = (float) module.getHudScale();
        }

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(titleScale, titleScale);

        int baseYRaw = screenHeight / 2 + hudOffsetY;
        int minCenterX = (int) Math.ceil((titleWidth * titleScale) / 2.0f) + 2;
        int maxCenterX = screenWidth - minCenterX;
        int clampedCenterX = MathHelper.clamp(centerX + hudOffsetX, minCenterX, Math.max(minCenterX, maxCenterX));
        int maxBaseY = screenHeight - (int) Math.ceil(textRenderer.fontHeight * titleScale) - 2;
        int baseY = MathHelper.clamp(baseYRaw, 2, Math.max(2, maxBaseY));

        float titleInv = 1.0f / titleScale;
        int drawTitleX = (int) ((clampedCenterX * titleInv) - (titleWidth / 2));
        int drawTitleY = (int) (baseY * titleInv);
        XenDrawContextCompat.drawTextWithShadow(context, textRenderer, title, drawTitleX, drawTitleY, 0xFFFFFFFF);
        context.getMatrices().popMatrix();

        float subScale = titleScale * 0.6f;
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(subScale, subScale);

        int subtitleWidth = textRenderer.getWidth(subtitle);
        float subInv = 1.0f / subScale;
        int subScreenY = baseY + 25;
        int minSubCenterX = (int) Math.ceil((subtitleWidth * subScale) / 2.0f) + 2;
        int maxSubCenterX = screenWidth - minSubCenterX;
        int subCenterX = MathHelper.clamp(clampedCenterX, minSubCenterX, Math.max(minSubCenterX, maxSubCenterX));
        int maxSubY = screenHeight - (int) Math.ceil(textRenderer.fontHeight * subScale) - 2;
        subScreenY = MathHelper.clamp(subScreenY, 2, Math.max(2, maxSubY));

        int drawSubX = (int) ((subCenterX * subInv) - (subtitleWidth / 2));
        int drawSubY = (int) (subScreenY * subInv);
        XenDrawContextCompat.drawTextWithShadow(context, textRenderer, subtitle, drawSubX, drawSubY, 0xFFFFFFFF);
        context.getMatrices().popMatrix();
    }

    public static void renderHotbarCooldown(DrawContext context, int x, int y, PlayerEntity player, ItemStack stack) {
        CooldownItemsTracker.renderBackground(context, stack, x, y, false);
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        for (CooldownItemsTracker.OverlayText overlay : CooldownItemsTracker.getOverlayTexts(stack, false)) {
            int ox = x + (overlay.alternativePosition() ? -8 : 0);
            int oy = y + (overlay.alternativePosition() ? -10 : 0);
            int width = textRenderer.getWidth(overlay.text());
            int drawX = ox + 19 - 2 - width;
            int drawY = oy + 9;
            XenDrawContextCompat.drawTextWithShadow(context, textRenderer, overlay.text(), drawX, drawY, 0xFF000000 | overlay.colorRgb());
        }
    }

    public static void pushAutoPetTitleScale(DrawContext context, Text subtitle) {
        if (!isAutoPetSubtitle(subtitle)) return;
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(0.4f, 0.4f);
    }

    public static void popAutoPetTitleScale(DrawContext context, Text subtitle) {
        if (!isAutoPetSubtitle(subtitle)) return;
        context.getMatrices().popMatrix();
    }

    private static boolean isAutoPetSubtitle(Text subtitle) {
        return subtitle != null && subtitle.getString().contains("Autopet");
    }
}
