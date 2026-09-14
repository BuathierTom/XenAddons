package xenus.xenaddons.mixin.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xenus.xenaddons.features.impl.dungeons.MuteBarkRuneModule;

@Mixin(SoundManager.class)
public class MixinSoundManagerMuteBarkRune {

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At("HEAD"), cancellable = true)
    private void xenaddons$muteBarkRune(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (MuteBarkRuneModule.shouldMute(sound)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "playDelayed(Lnet/minecraft/client/resources/sounds/SoundInstance;I)V", at = @At("HEAD"), cancellable = true)
    private void xenaddons$muteBarkRuneDelayed(SoundInstance sound, int delay, CallbackInfo ci) {
        if (MuteBarkRuneModule.shouldMute(sound)) {
            ci.cancel();
        }
    }
}
