package xenus.xenaddons.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.features.impl.dungeons.AutoPetRuleTitleModule;
import xenus.xenaddons.hud.HudState;

@Mixin(InGameHud.class)
public class MixinInGameHudRenderPetNotif {

    @Inject(method = "render", at = @At("TAIL"))
    private void xenaddons$renderPetNotif(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        var notif = HudState.PET_NOTIFICATION;
        if (!notif.isActive()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;

        TextRenderer tr = mc.textRenderer;

        Text title = notif.getTitle();
        Text subtitle = notif.getSubtitle();

        // --- Render Title (Main Pet Name) ---
        float titleScale = notif.scale; // fallback
        
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(titleScale, titleScale);
        
        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();

        int titleW = tr.getWidth(title);
        int centerX = screenW / 2;
        int hudOffsetX = 0;
        int hudOffsetY = notif.yOffset;
        AutoPetRuleTitleModule module = ModuleManager.getModule(AutoPetRuleTitleModule.class);
        if (module != null) {
            hudOffsetX = module.getHudOffsetX();
            hudOffsetY = module.getHudOffsetY();
            titleScale = (float) module.getHudScale();
        }

        int baseY = screenH / 2 + hudOffsetY;
        
        float titleInv = 1.0f / titleScale;
        int titleScreenX = centerX + hudOffsetX;
        int titleScreenY = baseY;

        // Convert screen coords to scaled coords
        int drawTitleX = (int)((titleScreenX * titleInv) - (titleW / 2));
        int drawTitleY = (int)(titleScreenY * titleInv);

        context.drawTextWithShadow(tr, title, drawTitleX, drawTitleY, 0xFFFFFFFF);
        context.getMatrices().popMatrix();


        // --- Render Subtitle ("Autopet") Smaller ---
        float subScale = titleScale * 0.6f; // ~1.5

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(subScale, subScale);
        
        int subW = tr.getWidth(subtitle);
        float subInv = 1.0f / subScale;
        
        // Position subtitle below title. Title is ~22px tall visually.
        // Let's go down 25px from baseY.
        int subScreenY = baseY + 25; 

        int drawSubX = (int)(((centerX + hudOffsetX) * subInv) - (subW / 2));
        int drawSubY = (int)(subScreenY * subInv);

        context.drawTextWithShadow(tr, subtitle, drawSubX, drawSubY, 0xFFFFFFFF);
        context.getMatrices().popMatrix();
    }
}
