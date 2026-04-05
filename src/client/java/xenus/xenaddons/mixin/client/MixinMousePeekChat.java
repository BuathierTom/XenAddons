package xenus.xenaddons.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.features.impl.misc.PeekChatController;

@Mixin(Mouse.class)
public abstract class MixinMousePeekChat {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void xenaddons$scrollPeekChat(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (!PeekChatController.shouldRenderPeek(client)) return;
        if (window != client.getWindow().getHandle()) return;
        if (client.getOverlay() != null) return;

        client.getInactivityFpsLimiter().onInput();

        int scrollAmount = xenaddons$getScrollAmount(vertical);
        if (scrollAmount != 0) {
            client.inGameHud.getChatHud().scroll(scrollAmount);
        }

        ci.cancel();
    }

    @Unique
    private int xenaddons$getScrollAmount(double vertical) {
        boolean discrete = client.options.getDiscreteMouseScroll().getValue();
        double sensitivity = client.options.getMouseWheelSensitivity().getValue();
        double adjustedVertical = (discrete ? Math.signum(vertical) : vertical) * sensitivity;
        adjustedVertical = MathHelper.clamp(adjustedVertical, -1.0D, 1.0D);
        if (!client.isShiftPressed()) {
            adjustedVertical *= 7.0D;
        }
        return (int) adjustedVertical;
    }
}
