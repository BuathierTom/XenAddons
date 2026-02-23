package xenus.xenaddons.gui

import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text
import net.minecraft.util.math.MathHelper
import org.lwjgl.glfw.GLFW
import xenus.xenaddons.config.ConfigManager
import xenus.xenaddons.features.ModuleManager
import xenus.xenaddons.features.impl.dungeons.AutoPetRuleTitleModule
import xenus.xenaddons.features.impl.dungeons.PlayersLeapedModule
import xenus.xenaddons.features.impl.dungeons.PosTitlesModule
import xenus.xenaddons.features.impl.misc.SpotifyOverlayModule
import xenus.xenaddons.hud.HudState
import xenus.xenaddons.utils.RenderUtils
import java.util.Locale

open class HudEditorScreen(private val parent: Screen?) : Screen(Text.literal("Edit HUD")) {
    private var posTitlesModule: PosTitlesModule? = null
    private var autoPetModule: AutoPetRuleTitleModule? = null
    private var playersLeapedModule: PlayersLeapedModule? = null
    private var spotifyOverlayModule: SpotifyOverlayModule? = null

    private var dragging: ComponentType? = null
    private var dragOffsetX = 0
    private var dragOffsetY = 0
    private var lastMouseX = 0
    private var lastMouseY = 0

    private var gridEnabled = false
    private var snapToGrid = false
    private var showHelp = false

    override fun init() {
        posTitlesModule = ModuleManager.getModule(PosTitlesModule::class.java)
        autoPetModule = ModuleManager.getModule(AutoPetRuleTitleModule::class.java)
        playersLeapedModule = ModuleManager.getModule(PlayersLeapedModule::class.java)
        spotifyOverlayModule = ModuleManager.getModule(SpotifyOverlayModule::class.java)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        lastMouseX = mouseX
        lastMouseY = mouseY

        if (dragging != null) applyDrag(mouseX, mouseY)

        context.fill(0, 0, width, height, 0x8A000000.toInt())
        if (gridEnabled) renderGrid(context)

        context.drawCenteredTextWithShadow(textRenderer, "HUD Editor", width / 2, 12, 0xFFFFFFFF.toInt())
        context.drawTextWithShadow(textRenderer, "Left click drag | Scroll = scale | ESC = save", 8, height - 14, 0xFFD0D0D0.toInt())

        renderComponent(context, mouseX, mouseY, ComponentType.POS_TITLES, getPosTitlesBounds())
        renderComponent(context, mouseX, mouseY, ComponentType.AUTOPET_TITLE, getAutoPetBounds())
        renderComponent(context, mouseX, mouseY, ComponentType.PLAYERS_LEAPED, getPlayersLeapedBounds())
        renderComponent(context, mouseX, mouseY, ComponentType.SPOTIFY_OVERLAY, getSpotifyBounds())

        renderResetButton(context, mouseX, mouseY)
        renderHelpButton(context, mouseX, mouseY)
        renderActiveTooltip(context, mouseX, mouseY)
        if (showHelp) renderHelpPanel(context)
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        if (click.button() != 0) return super.mouseClicked(click, doubled)

        val mouseX = click.x().toInt()
        val mouseY = click.y().toInt()

        if (isOverHelpButton(mouseX, mouseY)) {
            showHelp = !showHelp
            return true
        }

        if (isOverResetButton(mouseX, mouseY)) {
            resetAllPlacement()
            return true
        }

        val target = getActiveComponent(mouseX, mouseY)
        if (target != null) {
            val bounds = getBounds(target)
            dragging = target
            dragOffsetX = mouseX - bounds.x
            dragOffsetY = mouseY - bounds.y
            return true
        }

        return super.mouseClicked(click, doubled)
    }

    override fun mouseReleased(click: Click): Boolean {
        dragging = null
        return super.mouseReleased(click)
    }

    override fun mouseDragged(click: Click, deltaX: Double, deltaY: Double): Boolean {
        if (dragging == null) return super.mouseDragged(click, deltaX, deltaY)
        applyDrag(click.x().toInt(), click.y().toInt())
        return true
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (verticalAmount == 0.0) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)

        val target = dragging ?: getActiveComponent(mouseX.toInt(), mouseY.toInt())
        if (target == null) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)

        val scaleStep = if (verticalAmount > 0) 0.05 else -0.05
        setScale(target, getScale(target) + scaleStep)
        return true
    }

    override fun keyPressed(input: KeyInput): Boolean {
        val keyCode = input.key()
        val modifiers = input.modifiers()

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close()
            return true
        }

        if (keyCode == GLFW.GLFW_KEY_G) {
            if ((modifiers and GLFW.GLFW_MOD_CONTROL) != 0) snapToGrid = !snapToGrid
            else gridEnabled = !gridEnabled
            return true
        }

        if (keyCode == GLFW.GLFW_KEY_F1) {
            showHelp = !showHelp
            return true
        }

        val target = dragging ?: getActiveComponent(lastMouseX, lastMouseY)
        if (target == null) return super.keyPressed(input)

        val step = if ((modifiers and GLFW.GLFW_MOD_SHIFT) != 0) 8 else 1
        when (keyCode) {
            GLFW.GLFW_KEY_H -> centerHorizontally(target)
            GLFW.GLFW_KEY_V -> centerVertically(target)
            GLFW.GLFW_KEY_LEFT -> moveBy(target, -step, 0)
            GLFW.GLFW_KEY_RIGHT -> moveBy(target, step, 0)
            GLFW.GLFW_KEY_UP -> moveBy(target, 0, -step)
            GLFW.GLFW_KEY_DOWN -> moveBy(target, 0, step)
            GLFW.GLFW_KEY_R -> {
                val ctrl = (modifiers and GLFW.GLFW_MOD_CONTROL) != 0
                val shift = (modifiers and GLFW.GLFW_MOD_SHIFT) != 0
                if (ctrl && shift) resetAllPlacement() else resetPlacement(target)
            }
            else -> return super.keyPressed(input)
        }
        return true
    }

    override fun close() {
        ConfigManager.save()
        client?.setScreen(parent)
    }

    private fun renderGrid(context: DrawContext) {
        for (x in 0..width step GRID_SIZE) context.fill(x, 0, x + 1, height, 0x33FFFFFF)
        for (y in 0..height step GRID_SIZE) context.fill(0, y, width, y + 1, 0x33FFFFFF)
    }

    private fun renderComponent(context: DrawContext, mouseX: Int, mouseY: Int, type: ComponentType, bounds: Bounds) {
        renderComponentBox(context, mouseX, mouseY, type, bounds)
        when (type) {
            ComponentType.POS_TITLES -> renderPosTitlesPreview(context, bounds)
            ComponentType.AUTOPET_TITLE -> renderAutoPetPreview(context, bounds)
            ComponentType.PLAYERS_LEAPED -> renderPlayersLeapedPreview(context, bounds)
            ComponentType.SPOTIFY_OVERLAY -> renderSpotifyPreview(context, bounds)
        }
    }

    private fun applyDrag(mouseX: Int, mouseY: Int) {
        val target = dragging ?: return
        val current = getBounds(target)
        var newX = mouseX - dragOffsetX
        var newY = mouseY - dragOffsetY

        if (snapToGrid) {
            newX = snap(newX)
            newY = snap(newY)
        }

        setTopLeft(target, newX, newY, current, true)
    }

    private fun snap(value: Int): Int {
        return ((value - SNAP_PADDING + (GRID_SIZE / 2)) / GRID_SIZE) * GRID_SIZE + SNAP_PADDING
    }

    private fun moveBy(type: ComponentType, dx: Int, dy: Int) {
        val bounds = getBounds(type)
        setTopLeft(type, bounds.x + dx, bounds.y + dy, bounds, true)
    }

    private fun centerHorizontally(type: ComponentType) {
        val bounds = getBounds(type)
        setTopLeft(type, (width - bounds.w) / 2, bounds.y, bounds, true)
    }

    private fun centerVertically(type: ComponentType) {
        val bounds = getBounds(type)
        setTopLeft(type, bounds.x, (height - bounds.h) / 2, bounds, true)
    }

    private fun resetPlacement(type: ComponentType) {
        when (type) {
            ComponentType.POS_TITLES -> {
                posTitlesModule?.setHudOffsetX(0)
                posTitlesModule?.setHudY(30)
                posTitlesModule?.setHudScale(1.0)
            }
            ComponentType.AUTOPET_TITLE -> {
                autoPetModule?.setHudOffsetX(0)
                autoPetModule?.setHudOffsetY(-20)
                autoPetModule?.setHudScale(2.5)
            }
            ComponentType.PLAYERS_LEAPED -> {
                playersLeapedModule?.setHudOffsetX(0)
                playersLeapedModule?.setHudY(68)
                playersLeapedModule?.setHudScale(1.0)
            }
            ComponentType.SPOTIFY_OVERLAY -> {
                spotifyOverlayModule?.setHudOffsetX(0)
                spotifyOverlayModule?.setHudY(84)
                spotifyOverlayModule?.setHudScale(1.0)
            }
        }
    }

    private fun resetAllPlacement() {
        ComponentType.entries.forEach { resetPlacement(it) }
    }

    private fun getScale(type: ComponentType): Double {
        return when (type) {
            ComponentType.POS_TITLES -> posTitlesModule?.hudScale ?: 1.0
            ComponentType.AUTOPET_TITLE -> autoPetModule?.hudScale ?: 2.5
            ComponentType.PLAYERS_LEAPED -> playersLeapedModule?.hudScale ?: 1.0
            ComponentType.SPOTIFY_OVERLAY -> spotifyOverlayModule?.hudScale ?: 1.0
        }
    }

    private fun setScale(type: ComponentType, value: Double) {
        val clamped = MathHelper.clamp(value, 0.5, 6.0)
        when (type) {
            ComponentType.POS_TITLES -> posTitlesModule?.setHudScale(clamped)
            ComponentType.AUTOPET_TITLE -> autoPetModule?.setHudScale(clamped)
            ComponentType.PLAYERS_LEAPED -> playersLeapedModule?.setHudScale(clamped)
            ComponentType.SPOTIFY_OVERLAY -> spotifyOverlayModule?.setHudScale(clamped)
        }
    }

    private fun getActiveComponent(mouseX: Int, mouseY: Int): ComponentType? {
        val priority = listOf(
            ComponentType.SPOTIFY_OVERLAY,
            ComponentType.PLAYERS_LEAPED,
            ComponentType.AUTOPET_TITLE,
            ComponentType.POS_TITLES
        )
        return priority.firstOrNull { getBounds(it).contains(mouseX, mouseY) }
    }

    private fun getBounds(type: ComponentType): Bounds {
        return when (type) {
            ComponentType.POS_TITLES -> getPosTitlesBounds()
            ComponentType.AUTOPET_TITLE -> getAutoPetBounds()
            ComponentType.PLAYERS_LEAPED -> getPlayersLeapedBounds()
            ComponentType.SPOTIFY_OVERLAY -> getSpotifyBounds()
        }
    }

    private fun setTopLeft(type: ComponentType, newX: Int, newY: Int, referenceBounds: Bounds, clampToScreen: Boolean) {
        var x = newX
        var y = newY
        if (clampToScreen) {
            x = MathHelper.clamp(x, 0, (width - referenceBounds.w).coerceAtLeast(0))
            y = MathHelper.clamp(y, 0, (height - referenceBounds.h).coerceAtLeast(0))
        }

        when (type) {
            ComponentType.POS_TITLES -> {
                val centerX = x + (referenceBounds.w / 2)
                posTitlesModule?.setHudOffsetX(centerX - (width / 2))
                posTitlesModule?.setHudY(y)
            }
            ComponentType.AUTOPET_TITLE -> {
                val centerX = x + (referenceBounds.w / 2)
                autoPetModule?.setHudOffsetX(centerX - (width / 2))
                autoPetModule?.setHudOffsetY(y - (height / 2))
            }
            ComponentType.PLAYERS_LEAPED -> {
                val centerX = x + (referenceBounds.w / 2)
                playersLeapedModule?.setHudOffsetX(centerX - (width / 2))
                playersLeapedModule?.setHudY(y)
            }
            ComponentType.SPOTIFY_OVERLAY -> {
                spotifyOverlayModule?.setHudOffsetX(x - (width / 2))
                spotifyOverlayModule?.setHudY(y)
            }
        }
    }

    private fun renderResetButton(context: DrawContext, mouseX: Int, mouseY: Int) {
        val x = width - RESET_BTN_W - RESET_BTN_MARGIN
        val y = height - RESET_BTN_H - RESET_BTN_MARGIN
        val hovered = isOverResetButton(mouseX, mouseY)
        val bg = if (hovered) 0xCC8A2525.toInt() else 0xCC5E1C1C.toInt()
        val border = if (hovered) 0xFFFF8A8A.toInt() else 0xFFE07070.toInt()

        context.fill(x, y, x + RESET_BTN_W, y + RESET_BTN_H, bg)
        RenderUtils.drawBorder(context, x, y, RESET_BTN_W, RESET_BTN_H, border)
        context.drawCenteredTextWithShadow(textRenderer, "Reset all", x + RESET_BTN_W / 2, y + 5, 0xFFFFFFFF.toInt())
    }

    private fun isOverResetButton(mouseX: Int, mouseY: Int): Boolean {
        val x = width - RESET_BTN_W - RESET_BTN_MARGIN
        val y = height - RESET_BTN_H - RESET_BTN_MARGIN
        return mouseX in x..(x + RESET_BTN_W) && mouseY in y..(y + RESET_BTN_H)
    }

    private fun renderHelpButton(context: DrawContext, mouseX: Int, mouseY: Int) {
        val x = width - HELP_BTN_W - RESET_BTN_MARGIN
        val y = height - RESET_BTN_H - RESET_BTN_MARGIN - HELP_BTN_H - 6
        val hovered = isOverHelpButton(mouseX, mouseY)
        val active = showHelp

        val bg = when {
            active -> 0xCC24406E.toInt()
            hovered -> 0xCC293A52.toInt()
            else -> 0xCC1C2633.toInt()
        }
        val border = if (active) 0xFF7AA2FF.toInt() else 0xFF6D7A8A.toInt()

        context.fill(x, y, x + HELP_BTN_W, y + HELP_BTN_H, bg)
        RenderUtils.drawBorder(context, x, y, HELP_BTN_W, HELP_BTN_H, border)
        context.drawCenteredTextWithShadow(textRenderer, "Help", x + HELP_BTN_W / 2, y + 5, 0xFFFFFFFF.toInt())
    }

    private fun isOverHelpButton(mouseX: Int, mouseY: Int): Boolean {
        val x = width - HELP_BTN_W - RESET_BTN_MARGIN
        val y = height - RESET_BTN_H - RESET_BTN_MARGIN - HELP_BTN_H - 6
        return mouseX in x..(x + HELP_BTN_W) && mouseY in y..(y + HELP_BTN_H)
    }

    private fun renderActiveTooltip(context: DrawContext, mouseX: Int, mouseY: Int) {
        val target = dragging ?: getActiveComponent(mouseX, mouseY) ?: return
        val bounds = getBounds(target)
        val text = "${target.label} | x=${bounds.x} y=${bounds.y} | scale=${String.format(Locale.US, "%.2f", getScale(target))}"
        val textW = textRenderer.getWidth(text)
        val textH = textRenderer.fontHeight

        val boxX = (mouseX + 12).coerceAtMost(width - textW - 10)
        val boxY = (mouseY - 4).coerceAtLeast(6)

        context.fill(boxX - 4, boxY - 4, boxX + textW + 4, boxY + textH + 4, 0xD0101010.toInt())
        RenderUtils.drawBorder(context, boxX - 4, boxY - 4, textW + 8, textH + 8, 0xFFCCCCCC.toInt())
        context.drawTextWithShadow(textRenderer, text, boxX, boxY, 0xFFFFFFFF.toInt())
    }

    private fun renderHelpPanel(context: DrawContext) {
        val lines = arrayOf(
            "Left click: drag component",
            "Scroll on component: scale",
            "Arrow keys: move selected component",
            "Shift + arrows: move by 8px",
            "H / V: center horizontal / vertical",
            "R: reset selected component",
            "Ctrl + Shift + R: reset all",
            "G: toggle grid",
            "Ctrl + G: toggle snap",
            "F1: toggle this help"
        )

        val panelX = 10
        val panelY = 42
        val rowHeight = textRenderer.fontHeight + 1
        var maxWidth = 0
        lines.forEach { maxWidth = maxWidth.coerceAtLeast(textRenderer.getWidth(it)) }

        val panelW = maxWidth + 14
        val panelH = lines.size * rowHeight + 10
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xAA111111.toInt())
        RenderUtils.drawBorder(context, panelX, panelY, panelW, panelH, 0xFF6A8DFF.toInt())

        var y = panelY + 5
        lines.forEach { line ->
            val color = when {
                line.contains("grid") && gridEnabled -> 0xFF7CFF8A.toInt()
                line.contains("snap") && snapToGrid -> 0xFF7CFF8A.toInt()
                else -> 0xFFE0E0E0.toInt()
            }
            context.drawTextWithShadow(textRenderer, line, panelX + 7, y, color)
            y += rowHeight
        }
    }

    private fun getPosTitlesBounds(): Bounds {
        val textWidth = textRenderer.getWidth(POS_TITLES_PREVIEW)
        val scale = (posTitlesModule?.hudScale ?: 1.0).toFloat()
        val scaledWidth = kotlin.math.ceil(textWidth * scale).toInt()
        var x = (width - scaledWidth) / 2
        var y = 30
        posTitlesModule?.let {
            x += it.hudOffsetX
            y = it.hudY
        }
        return Bounds(x, y, scaledWidth, kotlin.math.ceil(textRenderer.fontHeight * scale).toInt())
    }

    private fun getAutoPetBounds(): Bounds {
        var centerX = width / 2
        var baseY = height / 2 + HudState.PET_NOTIFICATION.yOffset
        autoPetModule?.let {
            centerX += it.hudOffsetX
            baseY = (height / 2) + it.hudOffsetY
        }

        val titleScale = (autoPetModule?.hudScale ?: HudState.PET_NOTIFICATION.scale.toDouble()).toFloat()
        val subScale = titleScale * 0.6f
        val titleW = textRenderer.getWidth(AUTOPET_TITLE_PREVIEW)
        val subW = textRenderer.getWidth(AUTOPET_SUB_PREVIEW)

        val widthPx = kotlin.math.ceil(maxOf(titleW * titleScale, subW * subScale).toDouble()).toInt()
        val titleH = kotlin.math.ceil(textRenderer.fontHeight * titleScale).toInt()
        val subBottom = kotlin.math.ceil(25 + (textRenderer.fontHeight * subScale).toDouble()).toInt()
        val heightPx = maxOf(titleH, subBottom)

        val left = centerX - (widthPx / 2)
        return Bounds(left, baseY, widthPx, heightPx)
    }

    private fun getPlayersLeapedBounds(): Bounds {
        val textWidth = textRenderer.getWidth(PLAYERS_LEAPED_PREVIEW)
        val scale = (playersLeapedModule?.hudScale ?: 1.0).toFloat()
        val scaledWidth = kotlin.math.ceil(textWidth * scale).toInt()
        var x = (width - scaledWidth) / 2
        var y = 68
        playersLeapedModule?.let {
            x += it.hudOffsetX
            y = it.hudY
        }
        return Bounds(x, y, scaledWidth, kotlin.math.ceil(textRenderer.fontHeight * scale).toInt())
    }

    private fun getSpotifyBounds(): Bounds {
        val textWidth = textRenderer.getWidth(SPOTIFY_PREVIEW)
        val scale = (spotifyOverlayModule?.hudScale ?: 1.0).toFloat()
        val scaledWidth = kotlin.math.ceil(textWidth * scale).toInt()
        var x = width / 2
        var y = 84
        spotifyOverlayModule?.let {
            x += it.hudOffsetX
            y = it.hudY
        }
        return Bounds(x, y, scaledWidth, kotlin.math.ceil(textRenderer.fontHeight * scale).toInt())
    }

    private fun renderPosTitlesPreview(context: DrawContext, bounds: Bounds) {
        val scale = (posTitlesModule?.hudScale ?: 1.0).toFloat()
        val inv = 1.0f / scale
        context.matrices.pushMatrix()
        context.matrices.scale(scale, scale)
        context.drawTextWithShadow(textRenderer, POS_TITLES_PREVIEW, (bounds.x * inv).toInt(), (bounds.y * inv).toInt(), 0xFFFFFFFF.toInt())
        context.matrices.popMatrix()
    }

    private fun renderAutoPetPreview(context: DrawContext, bounds: Bounds) {
        val centerX = bounds.x + (bounds.w / 2)
        val baseY = bounds.y
        val titleScale = (autoPetModule?.hudScale ?: HudState.PET_NOTIFICATION.scale.toDouble()).toFloat()
        val subScale = titleScale * 0.6f

        val titleW = textRenderer.getWidth(AUTOPET_TITLE_PREVIEW)
        val subW = textRenderer.getWidth(AUTOPET_SUB_PREVIEW)

        context.matrices.pushMatrix()
        context.matrices.scale(titleScale, titleScale)
        val titleInv = 1.0f / titleScale
        val drawTitleX = ((centerX * titleInv) - (titleW / 2f)).toInt()
        val drawTitleY = (baseY * titleInv).toInt()
        context.drawTextWithShadow(textRenderer, AUTOPET_TITLE_PREVIEW, drawTitleX, drawTitleY, 0xFFFFAA00.toInt())
        context.matrices.popMatrix()

        context.matrices.pushMatrix()
        context.matrices.scale(subScale, subScale)
        val subInv = 1.0f / subScale
        val drawSubX = ((centerX * subInv) - (subW / 2f)).toInt()
        val drawSubY = ((baseY + 25) * subInv).toInt()
        context.drawTextWithShadow(textRenderer, AUTOPET_SUB_PREVIEW, drawSubX, drawSubY, 0xFFFF5555.toInt())
        context.matrices.popMatrix()
    }

    private fun renderPlayersLeapedPreview(context: DrawContext, bounds: Bounds) {
        val scale = (playersLeapedModule?.hudScale ?: 1.0).toFloat()
        val inv = 1.0f / scale
        context.matrices.pushMatrix()
        context.matrices.scale(scale, scale)
        context.drawTextWithShadow(textRenderer, PLAYERS_LEAPED_PREVIEW, (bounds.x * inv).toInt(), (bounds.y * inv).toInt(), 0xFF55AAFF.toInt())
        context.matrices.popMatrix()
    }

    private fun renderSpotifyPreview(context: DrawContext, bounds: Bounds) {
        val scale = (spotifyOverlayModule?.hudScale ?: 1.0).toFloat()
        val inv = 1.0f / scale
        context.matrices.pushMatrix()
        context.matrices.scale(scale, scale)
        context.drawTextWithShadow(textRenderer, "§6Spotify §7> §aAuteur §7- §bNom musique", (bounds.x * inv).toInt(), (bounds.y * inv).toInt(), 0xFFFFFFFF.toInt())
        context.matrices.popMatrix()
    }

    private fun renderComponentBox(context: DrawContext, mouseX: Int, mouseY: Int, type: ComponentType, bounds: Bounds) {
        val hovered = bounds.contains(mouseX, mouseY)
        val active = dragging == type
        val fillColor = when {
            active -> 0x443E8EFF
            hovered -> 0x333E8EFF
            else -> 0x22111111
        }
        val borderColor = when {
            active -> 0xFF7AA2FF.toInt()
            hovered -> 0xFF9FB8FF.toInt()
            else -> 0xFF7F7F7F.toInt()
        }

        context.fill(bounds.x - 2, bounds.y - 2, bounds.x + bounds.w + 2, bounds.y + bounds.h + 2, fillColor)
        RenderUtils.drawBorder(context, bounds.x - 2, bounds.y - 2, bounds.w + 4, bounds.h + 4, borderColor)
        context.drawTextWithShadow(textRenderer, type.label, bounds.x, bounds.y - 12, 0xFFE0E0E0.toInt())
    }

    private enum class ComponentType(val label: String) {
        POS_TITLES("Pos Titles"),
        AUTOPET_TITLE("AutoPet Title"),
        PLAYERS_LEAPED("Players Leaped"),
        SPOTIFY_OVERLAY("Spotify Overlay")
    }

    private data class Bounds(val x: Int, val y: Int, val w: Int, val h: Int) {
        fun contains(px: Int, py: Int): Boolean {
            return px >= x && px <= x + w.coerceAtLeast(1) && py >= y && py <= y + h.coerceAtLeast(1)
        }
    }

    companion object {
        private val POS_TITLES_PREVIEW = Text.literal("Player (M) At P2!")
        private val AUTOPET_TITLE_PREVIEW = Text.literal("Golden Dragon")
        private val AUTOPET_SUB_PREVIEW = Text.literal("Autopet")
        private val PLAYERS_LEAPED_PREVIEW = Text.literal("2/4 Players Leaped")
        private val SPOTIFY_PREVIEW = Text.literal("Spotify > Auteur - Nom musique")

        private const val RESET_BTN_W = 120
        private const val RESET_BTN_H = 16
        private const val RESET_BTN_MARGIN = 10

        private const val HELP_BTN_W = 80
        private const val HELP_BTN_H = 16

        private const val GRID_SIZE = 8
        private const val SNAP_PADDING = 4
    }
}
