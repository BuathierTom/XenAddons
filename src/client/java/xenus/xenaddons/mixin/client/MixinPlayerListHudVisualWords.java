package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import xenus.xenaddons.compat.XenTextCompat;

@Mixin(PlayerTabOverlay.class)
public class MixinPlayerListHudVisualWords {
    @ModifyArg(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"
        ),
        index = 1,
        require = 0
    )
    private Component xenaddons$replaceTabPlayerNameRenderOnly(Component original) {
        return XenTextCompat.replaceText(original);
    }
}
