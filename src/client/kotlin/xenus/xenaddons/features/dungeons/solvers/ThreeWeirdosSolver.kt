package xenus.xenaddons.features.dungeons.solvers

import xenus.xenaddons.api.events.*
import xenus.xenaddons.config.XenAddonsConfig
import xenus.xenaddons.api.utils.Render3DImmediate
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.sqrt

object ThreeWeirdosSolver {
    private val npcRegex = "^\\[NPC] (\\w+): (.*)".toRegex()
    private val completedRegex = "^PUZZLE SOLVED! \\w+ wasn't fooled by \\w+! Good job!$".toRegex()
    private val failedRegex = "^PUZZLE FAIL! \\w+ was fooled by \\w+! Yikes!$".toRegex()
    
    private val solutions = listOf(
        "The reward is not in my chest!".toRegex(),
        "At least one of them is lying, and the reward is not in \\w+'s chest.?".toRegex(),
        "My chest doesn't have the reward\\. We are all telling the truth.?".toRegex(),
        "My chest has the reward and I'm telling the truth!".toRegex(),
        "The reward isn't in any of our chests.?".toRegex(),
        "Both of them are telling the truth\\. Also, \\w+ has the reward in their chest.?".toRegex()
    )
    private val wrongAnswers = listOf(
        "One of us is telling the truth!".toRegex(),
        "They are both telling the truth\\. The reward isn't in \\w+'s chest.".toRegex(),
        "We are all telling the truth!".toRegex(),
        "\\w+ is telling the truth and the reward is in his chest.".toRegex(),
        "My chest doesn't have the reward. At least one of the others is telling the truth!".toRegex(),
        "One of the others is lying.".toRegex(),
        "They are both telling the truth, the reward is in \\w+'s chest.".toRegex(),
        "They are both lying, the reward is in my chest!".toRegex(),
        "The reward is in my chest.".toRegex(),
        "The reward is not in my chest\\. They are both lying.".toRegex(),
        "\\w+ is telling the truth.".toRegex(),
        "My chest has the reward.".toRegex()
    )
    private val dirs = listOf(
        1 to 0,
        -1 to 0,
        0 to 1,
        0 to -1
    )
    
    private val CORRECT_OUTLINE_COLOR = Color(0, 255, 0, 255)
    private val CORRECT_FILLED_COLOR = Color(0, 255, 0, 80)
    private val WRONG_OUTLINE_COLOR = Color(255, 0, 0, 255)
    private val WRONG_FILLED_COLOR = Color(255, 0, 0, 80)
    
    val answers = CopyOnWriteArrayList<AnswerData>()

    data class AnswerData(
        var entityPos: Vec3,
        var chestPos: Pair<Double, Double>,
        var isCorrect: Boolean
    )

    fun initialize() {
        EventBus.on<ChatEvent> { event ->
            // Use config check here if we add one for 3Weirdos specifically, assuming part of Solvers
            if (event.matches(completedRegex) != null) {
                reset()
                return@on
            }

            if (event.matches(failedRegex) != null) {
                reset()
                return@on
            }

            val match = event.matches(npcRegex) ?: return@on
            val name = match[0]
            val message = match[1]
            
            if (wrongAnswers.any { it.matches(message) }) {
                getChest(name, false)?.let {
                   if (!answers.contains(it)) answers.add(it)
                }
                return@on
            }
            if (!solutions.any { it.matches(message) }) return@on

            // Cancel chat event is not easy in our system unless it's CancellableEvent
            // event.cancel() // ChatEvent is Cancelable? Check Event.kt
            // Yes, ChatEvent extends CriteriaEvent extends CancellableEvent (Wait, check Event.kt)
            // Event.kt: @Threaded open class ChatEvent(message: String, val text: Component) : CriteriaEvent(message)
            // CriteriaEvent : CancellableEvent calls EventBus.post.
            // But ChatEvent constructor calls super?
            // Actually `CriteriaEvent` inherits `CancellableEvent`. 
            // So yes it is cancellable.
            
            // event.cancel() // Optional: Hide solution message
            Minecraft.getInstance().player?.displayClientMessage(Component.literal("§e[NPC] §b§l$name: §a§l$message"), false)
            
            getChest(name, true)?.let {
                if (!answers.contains(it)) answers.add(it)
            }
        }

        EventBus.on<RenderWorldEvent> {
            if (answers.isEmpty()) return@on
            
            answers.forEach {
                val entityPos = it.entityPos
                val chestPos = it.chestPos
                val isCorrect = it.isCorrect

                // Render chest - y hardcoded to 69 as per original code? 
                // Original: Render3DImmediate.renderWireframeBox(chestPos.first, 69.0, chestPos.second, ...
                // This assumes standard room template.
                
                Render3DImmediate.renderWireframeBox(
                    chestPos.first, 69.0, chestPos.second,
                    1.0, 1.0,
                    if (isCorrect) CORRECT_OUTLINE_COLOR else WRONG_OUTLINE_COLOR,
                    phase = false,
                )
                // Skip filled for now or use wireframe with alpha if Render3DImmediate supports it
                
                Render3DImmediate.renderWireframeBox(
                    entityPos.x, entityPos.y, entityPos.z,
                    0.8, 2.0,
                    if (isCorrect) CORRECT_OUTLINE_COLOR else WRONG_OUTLINE_COLOR,
                    phase = false,
                    centered = true,
                )
            }
        }

        EventBus.on<WorldChangeEvent> {
            reset()
        }
    }

    private fun reset() {
        answers.clear()
    }

    private fun getChest(name: String, isCorrect: Boolean): AnswerData? {
        val world = Minecraft.getInstance().level ?: return null
        // Scan for entity by name
        val entity = world.entitiesForRendering().find { it.name.string.contains(name) } ?: return null
        
        val pos = entity.position()
        val x0 = pos.x - 0.5
        val z0 = pos.z - 0.5
        var chestPos: Pair<Double, Double>? = null

        for (dir in dirs) {
            val ( dx, dz ) = dir
            val blockPos = net.minecraft.core.BlockPos(((x0 + dx).toInt()), 69, ((z0 + dz).toInt()))
            val blockState = world.getBlockState(blockPos)
            if (blockState.block != Blocks.CHEST) continue
            chestPos = (blockPos.x.toDouble() to blockPos.z.toDouble())
            // Correct x/z for rendering. BlockPos x,z is corner. 
            // RenderWireframeBox expects X,Y,Z (min coords?). 
            // Original code used x0+dx which is approx center? 
            // Let's use BlockPos coordinates.
            chestPos = blockPos.x.toDouble() to blockPos.z.toDouble()
        }
        if (chestPos == null) return null

        return AnswerData(pos, chestPos, isCorrect)
    }
}
