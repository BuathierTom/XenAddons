package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xenus.xenaddons.XenAddons;
import xenus.xenaddons.compat.XenHandledScreenCompat;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen extends Screen {
    private XenAddons.CustomButtonData currentHoveredBtn = null;

    protected MixinHandledScreen(Text title) {
        super(title);
    }

    @Inject(method = "renderMain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawForeground(Lnet/minecraft/client/gui/DrawContext;II)V", shift = At.Shift.BEFORE))
    private void renderInvButtons(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        currentHoveredBtn = XenHandledScreenCompat.renderInvButtons(this, context, textRenderer, mouseX, mouseY);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void drawInvTooltips(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        XenHandledScreenCompat.drawInvTooltip(context, textRenderer, currentHoveredBtn, mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (XenHandledScreenCompat.handleMouseClicked(this, click)) {
            cir.setReturnValue(true);
        }
    }
}
