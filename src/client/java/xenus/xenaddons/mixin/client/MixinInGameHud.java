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
import xenus.xenaddons.compat.XenHudCompat;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud {
    
    @Shadow private Text subtitle;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderTitleAndSubtitle(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"))
    private void onBeforeRenderTitle(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        XenHudCompat.pushAutoPetTitleScale(context, subtitle);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderTitleAndSubtitle(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", shift = At.Shift.AFTER))
    private void onAfterRenderTitle(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        XenHudCompat.popAutoPetTitleScale(context, subtitle);
    }
}
