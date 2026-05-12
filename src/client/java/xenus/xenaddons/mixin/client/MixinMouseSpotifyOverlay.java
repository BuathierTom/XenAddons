package xenus.xenaddons.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.features.impl.misc.SpotifyOverlayModule;

@Mixin(Mouse.class)
public abstract class MixinMouseSpotifyOverlay {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void xenaddons$clickSpotifyOverlayControls(long window, MouseInput input, int action, CallbackInfo ci) {
        if (window != client.getWindow().getHandle()) return;
        if (action != GLFW.GLFW_PRESS) return;
        if (client.currentScreen == null || client.getOverlay() != null) return;
        if ("xenus.xenaddons.gui.HudEditorScreen".equals(client.currentScreen.getClass().getName())) return;

        SpotifyOverlayModule module = ModuleManager.getModule(SpotifyOverlayModule.class);
        if (module == null) return;

        double mouseX = client.mouse.getScaledX(client.getWindow());
        double mouseY = client.mouse.getScaledY(client.getWindow());
        if (module.onMouseClicked(mouseX, mouseY, input.button())) {
            client.getInactivityFpsLimiter().onInput();
            ci.cancel();
        }
    }
}
