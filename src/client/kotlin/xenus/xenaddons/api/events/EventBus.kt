package xenus.xenaddons.api.events

import xenus.xenaddons.api.utils.StringUtils.clearCodes
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents
import net.minecraft.network.protocol.game.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback

object EventBus {
    val events = ConcurrentHashMap<KClass<*>, MutableList<EventListener<Event>>>()
    private var clientTicks = 0
    private val teamRegex = "^team_(\\d+)$".toRegex()

    init {
        ClientTickEvents.START_CLIENT_TICK.register { 
            post(TickEvent(clientTicks++)) 
        }

        ClientEntityEvents.ENTITY_LOAD.register { entity, _ ->
           post(EntityJoinEvent(entity))
        }

        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register { _, _ ->
            post(WorldChangeEvent())
        }

/*
        WorldRenderEvents.AFTER_ENTITIES.register { context: WorldRenderContext ->
            post(RenderWorldEvent(context.matrixStack(), context.tickCounter().getGameTimeDeltaPartialTick(true), context.consumers()))
        }
*/
        HudRenderCallback.EVENT.register { ctx, tickCounter ->
            // tickCounter is DeltaTracker
            post(RenderOverlayEvent(ctx, tickCounter.getGameTimeDeltaPartialTick(true)))
        }
    }

    // Packet Listener hook - must be triggered from a PacketMixin
    // Since I cannot create mixins easily without restarting, I will create a method to be called by a Mixin I will create next.
    fun onPacketReceived(packet: net.minecraft.network.protocol.Packet<*>) {
        if (PacketReceivedEvent(packet).post()) return // If cancelled

        when (packet) {
             is ClientboundSetPlayerTeamPacket -> {
                if (packet.parameters.isEmpty) return
                val team = packet.parameters.get()
                val teamPrefix = team.playerPrefix.string
                val teamSuffix = team.playerSuffix.string
                if (teamPrefix.isEmpty()) return
                if (!packet.name.matches(teamRegex)) return
                ScoreboardEvent("${teamPrefix}${teamSuffix.trim()}").post()
            }
            is ClientboundSystemChatPacket -> {
                val content = packet.content ?: return
                val message = content.string.replace(Regex("[\\u00a7&][0-9a-fk-or]"), "")
                val specialized = ChatChannelEvent.from(message, content)
                ChatEvent(message, content).post()
                specialized?.post()
            }
            is ClientboundPlayerInfoUpdatePacket -> {
                val action = packet.actions().firstOrNull() ?: return
                if (action === ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME) {
                     packet.entries().forEach {
                        val name = it.displayName ?: return@forEach
                        TabUpdateEvent(name.string.replace(Regex("[\\u00a7&][0-9a-fk-or]"), "")).post()
                    }
                }
            }
            is ClientboundSoundPacket -> {
                 SoundPlayEvent(packet.sound.value().location.toString(), packet.volume, packet.pitch, packet.x, packet.y, packet.z).post()
            }
        }
    }
    
    fun onPacketSent(packet: net.minecraft.network.protocol.Packet<*>) {
         PrePacketSentEvent(packet).post()
    }

    inline fun <reified T : Event> on(noinline cb: (T) -> Unit): EventListener<T> = on<T>(cb, true)

    inline fun <reified T : Event> on(noinline cb: (T) -> Unit, add: Boolean = true): EventListener<T> {
        val obj = EventListener(cb, T::class)
        if (add) obj.register()
        return obj
    }

    private fun removeImpl(T: KClass<*>, listener: EventListener<*>) {
        events[T]?.remove(listener)
    }

    fun remove(T: KClass<*>, listener: EventListener<*>) {
        removeImpl(T, listener)
    }

    private fun addImpl(T: KClass<*>, listener: EventListener<Event>) {
        // Fix getOrPut unresolved by using computeIfAbsent
        val arr = events.computeIfAbsent(T) { CopyOnWriteArrayList() }
        arr.add(listener)
    }

    fun add(T: KClass<*>, listener: EventListener<Event>) {
        addImpl(T, listener)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Event> post(event: T): Boolean {
        val listeners = events[event::class] ?: return false
        if (listeners.isEmpty()) return false

        listeners.forEach { cb ->
            cb.trigger(event)
        }
        
        if (event is CancellableEvent) return event.isCancelled()
        return false
    }
}
