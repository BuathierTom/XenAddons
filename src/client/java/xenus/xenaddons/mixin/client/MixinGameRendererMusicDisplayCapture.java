package xenus.xenaddons.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.features.impl.misc.SpotifyOverlayModule;
import xenus.xenaddons.render.music.MusicCaptureManager;

@Mixin(GameRenderer.class)
public class MixinGameRendererMusicDisplayCapture {
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void xenaddons$captureWorldForMusicLiquidGlass(DeltaTracker deltaTracker, CallbackInfo ci) {
        SpotifyOverlayModule module = ModuleManager.getModule(SpotifyOverlayModule.class);
        if (module != null && module.isEnabled() && module.isBetterDisplayEnabled()) {
            MusicCaptureManager.update(Minecraft.getInstance());
        } else {
            MusicCaptureManager.cleanup();
        }
    }
}
