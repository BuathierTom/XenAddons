package xenus.xenaddons.compat;

import net.minecraft.client.sound.SoundInstance;
import xenus.xenaddons.features.impl.misc.CustomHyperionSoundHandler;

public final class XenSoundCompat {
    private XenSoundCompat() {}

    public static boolean shouldCancelSound(SoundInstance sound) {
        return CustomHyperionSoundHandler.onPlaySound(sound);
    }
}
