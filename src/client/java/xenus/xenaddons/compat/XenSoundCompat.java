package xenus.xenaddons.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import xenus.xenaddons.features.impl.misc.CustomHyperionSoundHandler;

public final class XenSoundCompat {
    private XenSoundCompat() {}

    public static boolean shouldCancelSound(SoundInstance sound) {
        return CustomHyperionSoundHandler.onPlaySound(sound);
    }

    public static void playButtonClick(MinecraftClient client) {
        if (client == null) return;
        client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
