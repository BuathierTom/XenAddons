package xenus.xenaddons.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.features.impl.misc.PeekChatController;

@Mixin(InGameHud.class)
public abstract class MixinInGameHudPeekChat {
    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private ChatHud chatHud;
    @Shadow private int ticks;

    @Inject(method = "renderChat", at = @At("HEAD"), cancellable = true)
    private void xenaddons$renderPeekChat(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!PeekChatController.shouldRenderPeek(client)) return;

        var window = client.getWindow();
        int mouseX = MathHelper.floor(client.mouse.getScaledX(window));
        int mouseY = MathHelper.floor(client.mouse.getScaledY(window));

        context.createNewRootLayer();
        chatHud.render(context, ticks, mouseX, mouseY, true);
        ci.cancel();
    }
}
