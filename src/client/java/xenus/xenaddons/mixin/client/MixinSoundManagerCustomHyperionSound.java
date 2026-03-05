package xenus.xenaddons.mixin.client;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xenus.xenaddons.features.impl.misc.CustomHyperionSoundHandler;

@Mixin(SoundManager.class)
public class MixinSoundManagerCustomHyperionSound {

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;", at = @At("HEAD"), cancellable = true)
    private void xenaddons$replaceHypeSound(SoundInstance sound, CallbackInfoReturnable<SoundSystem.PlayResult> cir) {
        if (CustomHyperionSoundHandler.onPlaySound(sound)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;I)V", at = @At("HEAD"), cancellable = true)
    private void xenaddons$replaceHypeSoundDelayed(SoundInstance sound, int delay, CallbackInfo ci) {
        if (CustomHyperionSoundHandler.onPlaySound(sound)) {
            ci.cancel();
        }
    }
}
