package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import xenus.xenaddons.config.GoatedPlayersManager;

@Mixin(PlayerListHud.class)
public class MixinPlayerListHudVisualWords {
    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I"
        ),
        index = 1,
        require = 0
    )
    private Text xenaddons$replaceTabPlayerNameRenderOnly(Text original) {
        return GoatedPlayersManager.applyVisualWords(original);
    }
}
