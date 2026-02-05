package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.text.Text;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud {
    
    @Shadow private Text subtitle;

    // We target the main render method to avoid issues with specific title method signatures in 1.21.10
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderTitle(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"))
    private void onBeforeRenderTitle(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (subtitle != null && subtitle.getString().contains("Autopet")) {
            // Apply scale before the title is rendered
            context.getMatrices().pushMatrix();
            context.getMatrices().scale(0.4f, 0.4f);
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderTitle(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", shift = At.Shift.AFTER))
    private void onAfterRenderTitle(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (subtitle != null && subtitle.getString().contains("Autopet")) {
            // Restore matrix after rendering
            context.getMatrices().popMatrix();
        }
    }
}
