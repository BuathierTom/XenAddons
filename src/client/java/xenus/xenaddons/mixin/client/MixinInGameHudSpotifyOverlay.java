package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.features.impl.dungeons.MilestoneDisplayModule;
import xenus.xenaddons.features.impl.misc.SpotifyOverlayModule;

@Mixin(InGameHud.class)
public class MixinInGameHudSpotifyOverlay {

    @Inject(method = "render", at = @At("HEAD"))
    private void xenaddons$renderSpotifyBeforeHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        SpotifyOverlayModule module = ModuleManager.getModule(SpotifyOverlayModule.class);
        if (module != null) {
            module.renderInGameHud(context);
        }

        MilestoneDisplayModule milestoneDisplay = ModuleManager.getModule(MilestoneDisplayModule.class);
        if (milestoneDisplay != null) {
            milestoneDisplay.renderInGameHud(context);
        }
    }
}
