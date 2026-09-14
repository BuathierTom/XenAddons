package xenus.xenaddons.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xenus.xenaddons.features.impl.misc.FarmingToolKeysModule;

@Mixin(KeyMapping.class)
public abstract class MixinKeyMappingFarmingToolKeys {

    @Shadow
    protected InputConstants.Key key;

    @Shadow
    private int clickCount;

    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void xenaddons$farmingToolKeysIsDown(CallbackInfoReturnable<Boolean> cir) {
        Boolean result = FarmingToolKeysModule.resolveIsDown((KeyMapping) (Object) this, xenaddons$farmingToolKeyCode());
        if (result != null) cir.setReturnValue(result);
    }

    @Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
    private void xenaddons$farmingToolKeysConsumeClick(CallbackInfoReturnable<Boolean> cir) {
        Boolean result = FarmingToolKeysModule.resolveConsumeClick((KeyMapping) (Object) this, xenaddons$farmingToolKeyCode());
        if (result == null) return;

        this.clickCount = 0;
        cir.setReturnValue(result);
    }

    @Unique
    private int xenaddons$farmingToolKeyCode() {
        InputConstants.Key bound = this.key;
        if (bound == null) return -1;
        return bound.getType() == InputConstants.Type.MOUSE ? -100 + bound.getValue() : bound.getValue();
    }
}
