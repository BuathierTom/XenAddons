package xenus.xenaddons.features.dungeons

import xenus.xenaddons.api.events.EventBus
import xenus.xenaddons.api.dungeon.DungeonEvent
import xenus.xenaddons.config.XenAddonsConfig
import net.minecraft.client.Minecraft
import net.minecraft.sounds.SoundEvents

object SecretsSound {
    fun initialize() {
        EventBus.on<DungeonEvent.SecretClicked> {
            if (XenAddonsConfig.secretsSound) playSound()
        }
        
        EventBus.on<DungeonEvent.SecretPickup> {
            if (XenAddonsConfig.secretsSound) playSound()
        }
    }
    
    private fun playSound() {
        val player = Minecraft.getInstance().player ?: return
        // Running on main thread to be safe, as events can be threaded
        Minecraft.getInstance().execute {
            player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1f, 2f)
        }
    }
}
