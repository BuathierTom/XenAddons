package xenus.xenaddons.features.dungeons.solvers

import xenus.xenaddons.api.dungeon.Dungeons
import xenus.xenaddons.api.events.*
import xenus.xenaddons.config.XenAddonsConfig
import xenus.xenaddons.api.utils.Render3DImmediate
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.Blocks
import net.minecraft.client.Minecraft
import java.awt.Color

object LividSolver {
    private val mapBlocks = mapOf(
        Blocks.WHITE_WOOL to "Vendetta",
        Blocks.MAGENTA_WOOL to "Crossed",
        Blocks.YELLOW_WOOL to "Arcade",
        Blocks.LIME_WOOL to "Smile",
        Blocks.GRAY_WOOL to "Doctor",
        Blocks.PURPLE_WOOL to "Purple",
        Blocks.GREEN_WOOL to "Frog",
        Blocks.BLUE_WOOL to "Scream",
        Blocks.RED_WOOL to "Hockey"
    )
    private val lividNameRegex = "^\\w+ Livid$".toRegex()
    var started = false
    var currentLivid: String? = null
    var lividEnt: Entity? = null

    fun initialize() {
        EventBus.on<ChatEvent> { event ->
            if (event.message == "[BOSS] Livid: I respect you for making it to here, but I'll be your undoing.") started = true
        }

        EventBus.on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet !is ClientboundSectionBlocksUpdatePacket) return@on

            packet.runUpdates { t, u ->
                if (t.x != 5 || t.y != 108 || t.z != 43) return@runUpdates
                if (mapBlocks[u.block] != null) currentLivid = mapBlocks[u.block]
            }
        }

        EventBus.on<TickEvent> {
            if (!XenAddonsConfig.lividSolver) return@on
            if (!started) return@on
            val name = if (currentLivid == null) return@on else "$currentLivid Livid"
            val world = Minecraft.getInstance().level ?: return@on
            
            // Optimization: Only search if lividEnt invalid
            if (lividEnt == null || !lividEnt!!.isAlive) {
                lividEnt = world.players().find { it.name.string.contains(name) }
            }
        }

        /* 
        // PreExtractRenderEntityEvent does not exist in our EventBus yet. 
        // We can skip hiding wrong livids for now or implement it later.
        EventBus.on<PreExtractRenderEntityEvent> { event ->
            if (!XenAddonsConfig.lividHideWrong || lividEnt == null) return@on
            val name = event.entity.name?.string ?: return@on
            if (!name.matches(lividNameRegex)) return@on
            if (event.entity.id == lividEnt?.id) return@on

            event.cancel()
        }
        */

        EventBus.on<RenderWorldEvent> {
            if (!XenAddonsConfig.lividSolver) return@on
            if (!started) return@on
            // Also check floor F5/M5
            if (Dungeons.floor.floorNum != 5) return@on
            
            val entity = lividEnt ?: return@on

            val pos = entity.getPosition(it.partialTicks)

            Render3DImmediate.renderWireframeBox(
                pos.x, pos.y, pos.z,
                entity.bbWidth.toDouble(), entity.bbHeight.toDouble(),
                Color(XenAddonsConfig.lividBoxColor),
                lineWidth = XenAddonsConfig.lividLineWidth.toDouble(),
                centered = true,
            )
            if (XenAddonsConfig.lividTracer) {
                 Render3DImmediate.renderTracer(
                    pos.x, pos.y + 1.0, pos.z,
                    Color(XenAddonsConfig.lividBoxColor),
                )
            }
        }
        
        EventBus.on<WorldChangeEvent> {
            started = false
            currentLivid = "Hockey" // Default fallback or null
            lividEnt = null
        }
    }
}
