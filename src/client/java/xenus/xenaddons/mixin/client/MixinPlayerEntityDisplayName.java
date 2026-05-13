package xenus.xenaddons.mixin.client;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xenus.xenaddons.compat.XenTextCompat;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntityDisplayName {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void xenaddons$replaceDisplayName(CallbackInfoReturnable<Text> cir) {
        Text original = cir.getReturnValue();
        cir.setReturnValue(XenTextCompat.replaceText(original));
    }
}
