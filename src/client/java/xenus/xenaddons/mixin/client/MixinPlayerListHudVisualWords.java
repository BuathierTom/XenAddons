package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xenus.xenaddons.config.GoatedPlayersManager;

@Mixin(PlayerListHud.class)
public class MixinPlayerListHudVisualWords {

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void xenaddons$replaceTabPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        Text original = cir.getReturnValue();
        cir.setReturnValue(GoatedPlayersManager.applyVisualWords(original));
    }
}
