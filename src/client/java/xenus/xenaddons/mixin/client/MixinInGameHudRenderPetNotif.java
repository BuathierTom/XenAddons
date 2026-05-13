package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.compat.XenHudCompat;

@Mixin(InGameHud.class)
public class MixinInGameHudRenderPetNotif {

    @Inject(method = "render", at = @At("TAIL"))
    private void xenaddons$renderPetNotif(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        XenHudCompat.renderPetNotification(context);
    }
}
