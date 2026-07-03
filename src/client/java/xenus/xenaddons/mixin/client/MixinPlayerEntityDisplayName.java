package xenus.xenaddons.mixin.client;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xenus.xenaddons.compat.XenTextCompat;

@Mixin(Player.class)
public class MixinPlayerEntityDisplayName {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void xenaddons$replaceDisplayName(CallbackInfoReturnable<Component> cir) {
        Component original = cir.getReturnValue();
        cir.setReturnValue(XenTextCompat.replaceText(original));
    }
}
