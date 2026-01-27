package xenus.xenaddons.api.events

import xenus.xenaddons.api.utils.StringUtils.clearCodes
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack

@Target(AnnotationTarget.CLASS)
annotation class Threaded
@Target(AnnotationTarget.CLASS)
annotation class Ordered

abstract class Event {
    open fun post(): Boolean {
        return EventBus.post(this)
    }
}

abstract class CancellableEvent : Event() {
    private var shouldCancel = false

    fun cancel() {
        shouldCancel = true
    }

    fun isCancelled() = shouldCancel

    override fun post(): Boolean {
        EventBus.post(this)
        return isCancelled()
    }
}

@Threaded class PacketReceivedEvent(val packet: Packet<*>) : CancellableEvent()
@Threaded class PacketSentEvent(val packet: Packet<*>) : CancellableEvent()
@Threaded class PrePacketSentEvent(val packet: Packet<*>) : CancellableEvent()

class EntityJoinEvent(val entity: Entity) : Event()
class EntityLeaveEvent(val entity: Entity) : Event()
class ItemPickupEvent(val entity: ItemEntity, val entityId: Int) : Event()
class EntityDeathEvent(val entity: Entity) : Event()

class RenderWorldEvent(val matrixStack: com.mojang.blaze3d.vertex.PoseStack, val partialTicks: Float, val consumers: net.minecraft.client.renderer.MultiBufferSource? = null) : Event()
class RenderOverlayEvent(val ctx: net.minecraft.client.gui.GuiGraphics, val partialTicks: Float) : Event()

@Threaded class ScoreboardEvent(message: String) : CriteriaEvent(message)
@Threaded class TabUpdateEvent(message: String) : CriteriaEvent(message)
@Threaded class SoundPlayEvent(val sound: String, val volume: Float, val pitch: Float, val x: Double, val y: Double, val z: Double) : CancellableEvent()

open class CriteriaEvent(val message: String) : CancellableEvent() {
    fun matches(criteria: Regex): List<String>? {
        val matches = criteria.matchEntire(message) ?: return null
        return matches.groupValues.drop(1)
    }
}

@Threaded open class ChatEvent(message: String, val text: Component) : CriteriaEvent(message)
@Threaded class AreaEvent(val area: String?) : Event()
@Threaded class SubAreaEvent(val subarea: String?) : Event()
@Threaded class WorldChangeEvent : Event()
@Threaded class TickEvent(val tick: Int) : Event()

abstract class ChatChannelEvent(message: String, text: Component, val name: String, val userMessage: String) :
    ChatEvent(message, text) {
    @Threaded class PartyChatEvent(message: String, text: Component, name: String, userMessage: String) :
        ChatChannelEvent(message, text, name, userMessage)

    companion object {
        private val partyChatRegex = "^Party > (?:\\[[^]]+] )?(?<name>\\w{1,16}): (?<msg>.+)\$".toRegex()
        
        fun from(message: String, text: Component): ChatChannelEvent? {
             partyChatRegex.matchEntire(message)?.let {
                return PartyChatEvent(
                    message, text,
                    it.groups["name"]?.value ?: "",
                    it.groups["msg"]?.value ?: "",
                )
            }
            return null
        }
    }
}
