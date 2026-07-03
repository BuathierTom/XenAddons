package xenus.xenaddons.mixin.client;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xenus.xenaddons.XenAddons;
import xenus.xenaddons.compat.XenHandledScreenCompat;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinHandledScreen extends Screen {
    private XenAddons.CustomButtonData currentHoveredBtn = null;

    protected MixinHandledScreen(Component title) {
        super(title);
    }

    @Inject(
            method = "extractContents",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/joml/Matrix3x2fStack;popMatrix()Lorg/joml/Matrix3x2fStack;",
                    shift = At.Shift.BEFORE
            )
    )
    private void renderInvButtons(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        currentHoveredBtn = XenHandledScreenCompat.renderInvButtonsInContainerSpace(this, context, font, mouseX, mouseY);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void renderInvButtonTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        XenHandledScreenCompat.drawInvTooltip(context, font, currentHoveredBtn, mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (XenHandledScreenCompat.handleMouseClicked(this, click)) {
            cir.setReturnValue(true);
        }
    }
}
