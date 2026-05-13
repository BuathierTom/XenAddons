package xenus.xenaddons.mixin.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.compat.XenCooldownCompat;

@Mixin(DrawContext.class)
public class MixinDrawContextCooldownItems {

    @Inject(
        method = "drawItem(Lnet/minecraft/item/ItemStack;II)V",
        at = @At("HEAD"),
        require = 0
    )
    private void xenaddons$cooldownBackgroundOnDrawItem(ItemStack stack, int x, int y, CallbackInfo ci) {
        XenCooldownCompat.renderDrawItemBackground((DrawContext) (Object) this, stack, x, y);
    }

    @Inject(
        method = "drawItem(Lnet/minecraft/item/ItemStack;II)V",
        at = @At("TAIL"),
        require = 0
    )
    private void xenaddons$cooldownTextOnDrawItem(ItemStack stack, int x, int y, CallbackInfo ci) {
        XenCooldownCompat.renderDrawItemText((DrawContext) (Object) this, stack, x, y);
    }

    @Inject(
        method = "drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
        at = @At("HEAD"),
        require = 0
    )
    private void xenaddons$cooldownBackground(TextRenderer textRenderer, ItemStack stack, int x, int y, String countOverride, CallbackInfo ci) {
        XenCooldownCompat.renderStackOverlayBackground((DrawContext) (Object) this, stack, x, y);
    }

    @Inject(
        method = "drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
        at = @At("TAIL"),
        require = 0
    )
    private void xenaddons$cooldownText(TextRenderer textRenderer, ItemStack stack, int x, int y, String countOverride, CallbackInfo ci) {
        XenCooldownCompat.renderStackOverlayText((DrawContext) (Object) this, textRenderer, stack, x, y);
    }

    @ModifyVariable(
        method = "drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 4,
        require = 0
    )
    private String xenaddons$replaceOverlayText(String stackCountText, TextRenderer textRenderer, ItemStack stack, int x, int y) {
        return stackCountText;
    }
}
