package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.compat.XenCooldownCompat;

@Mixin(GuiGraphicsExtractor.class)
public class MixinDrawContextCooldownItems {

    @Inject(
        method = "item(Lnet/minecraft/world/item/ItemStack;II)V",
        at = @At("HEAD"),
        require = 0
    )
    private void xenaddons$cooldownBackgroundOnDrawItem(ItemStack stack, int x, int y, CallbackInfo ci) {
        XenCooldownCompat.renderDrawItemBackground((GuiGraphicsExtractor) (Object) this, stack, x, y);
    }

    @Inject(
        method = "item(Lnet/minecraft/world/item/ItemStack;II)V",
        at = @At("TAIL"),
        require = 0
    )
    private void xenaddons$cooldownTextOnDrawItem(ItemStack stack, int x, int y, CallbackInfo ci) {
        XenCooldownCompat.renderDrawItemText((GuiGraphicsExtractor) (Object) this, stack, x, y);
    }

    @Inject(
        method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
        at = @At("HEAD"),
        require = 0
    )
    private void xenaddons$cooldownBackground(Font textRenderer, ItemStack stack, int x, int y, String countOverride, CallbackInfo ci) {
        XenCooldownCompat.renderStackOverlayBackground((GuiGraphicsExtractor) (Object) this, stack, x, y);
    }

    @Inject(
        method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
        at = @At("TAIL"),
        require = 0
    )
    private void xenaddons$cooldownText(Font textRenderer, ItemStack stack, int x, int y, String countOverride, CallbackInfo ci) {
        XenCooldownCompat.renderStackOverlayText((GuiGraphicsExtractor) (Object) this, textRenderer, stack, x, y);
    }

    @ModifyVariable(
        method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0,
        require = 0
    )
    private String xenaddons$replaceOverlayText(String stackCountText, Font textRenderer, ItemStack stack, int x, int y) {
        return stackCountText;
    }
}
