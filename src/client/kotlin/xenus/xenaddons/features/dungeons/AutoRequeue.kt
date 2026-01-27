package xenus.xenaddons.features.dungeons

import xenus.xenaddons.api.events.EventBus
import xenus.xenaddons.api.events.ChatEvent
import xenus.xenaddons.config.XenAddonsConfig
import net.minecraft.client.Minecraft

object AutoRequeue {
    private val extraStatsRegex = "^ *[:>]+ EXTRA STATS [:>]+$".toRegex()

    fun initialize() {
        EventBus.on<ChatEvent> { event ->
            if (!XenAddonsConfig.autoRequeue) return@on
            if (!extraStatsRegex.matches(event.message.trim())) return@on
            
            Minecraft.getInstance().player?.connection?.sendCommand("instancerequeue")
        }
    }
}
