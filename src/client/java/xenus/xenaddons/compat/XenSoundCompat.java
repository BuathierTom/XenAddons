package xenus.xenaddons.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import xenus.xenaddons.features.impl.misc.CustomHyperionSoundHandler;

public final class XenSoundCompat {
    private XenSoundCompat() {}

    public static boolean shouldCancelSound(SoundInstance sound) {
        return CustomHyperionSoundHandler.onPlaySound(sound);
    }

    public static void playButtonClick(Minecraft client) {
        if (client == null) return;
        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
