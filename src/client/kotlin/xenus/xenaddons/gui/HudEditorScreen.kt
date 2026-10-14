package xenus.xenaddons.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import xenus.xenaddons.config.ConfigManager
import xenus.xenaddons.gui.ui.Theme
import xenus.xenaddons.hud.HudElement
import xenus.xenaddons.hud.HudManager
import xenus.xenaddons.hud.HudResolution
import xenus.xenaddons.utils.RenderUtils
import java.util.Locale
import kotlin.math.roundToInt

open class HudEditorScreen(private val parent: Screen?) : Screen(Component.literal("Edit HUD")) {
    private val elements = HudManager.elements().filter { it.isEnabled }
    private val buttons = mutableListOf<EditorButton>()

    private var dragging: HudElement? = null
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var gridEnabled = false
    private var gridSize = 10
    private var resetConfirm = false

    override fun init() {
        HudManager.setEditing(true)
        buttons.clear()

        var x = width / 2 - BUTTONS_WIDTH / 2
        val y = height - 30
        buttons += EditorButton(x, y, GRID_BUTTON_WIDTH, {
            if (gridEnabled) "§aGrid: ON §7(${gridSize}px)" else "§7Grid: OFF"
        }) { gridEnabled = !gridEnabled }
        x += GRID_BUTTON_WIDTH + BUTTON_GAP
        buttons += EditorButton(x, y, STEP_BUTTON_WIDTH, { "§7-" }) {
            gridSize = (gridSize - GRID_STEP).coerceAtLeast(MIN_GRID_SIZE)
            gridEnabled = true
        }
        x += STEP_BUTTON_WIDTH + BUTTON_GAP
        buttons += EditorButton(x, y, STEP_BUTTON_WIDTH, { "§7+" }) {
            gridSize = (gridSize + GRID_STEP).coerceAtMost(MAX_GRID_SIZE)
            gridEnabled = true
        }
        x += STEP_BUTTON_WIDTH + BUTTON_GAP
        buttons += EditorButton(x, y, RESET_BUTTON_WIDTH, { if (resetConfirm) "§c§lConfirm?" else "§cReset" }) {
            if (resetConfirm) HudManager.resetAll()
            resetConfirm = !resetConfirm
        }
    }

    override fun extractBackground(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, BACKGROUND_COLOR)
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        HudResolution.push(context)
        val virtualX = HudResolution.toVirtual(mouseX.toDouble())
        val virtualY = HudResolution.toVirtual(mouseY.toDouble())

        if (gridEnabled) drawGrid(context)
        dragging?.let { drag(it, virtualX, virtualY) }

        val active = dragging ?: elements.lastOrNull { it.contains(virtualX, virtualY) }
        for (element in elements) {
            drawFrame(context, element, element === active)
            element.render(context, true)
        }
        HudResolution.pop(context)

        active?.let { drawInfo(context, it) }
        val font = Minecraft.getInstance().font
        context.text(font, HINT, 8, height - font.lineHeight - 8, HINT_COLOR)
        buttons.forEach { it.render(context, mouseX, mouseY) }
        super.extractRenderState(context, mouseX, mouseY, delta)
    }

    override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
        if (click.button() != 0) return super.mouseClicked(click, doubled)

        val button = buttons.firstOrNull { it.contains(click.x, click.y) }
        if (button != null) {
            if (button !== buttons.last()) resetConfirm = false
            button.onClick()
            return true
        }
        resetConfirm = false

        val virtualX = HudResolution.toVirtual(click.x)
        val virtualY = HudResolution.toVirtual(click.y)
        val target = elements.lastOrNull { it.contains(virtualX, virtualY) } ?: return super.mouseClicked(click, doubled)
        dragging = target
        dragOffsetX = virtualX - target.x()
        dragOffsetY = virtualY - target.y()
        return true
    }

    override fun mouseReleased(click: MouseButtonEvent): Boolean {
        dragging = null
        return super.mouseReleased(click)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (verticalAmount == 0.0) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)

        val virtualX = HudResolution.toVirtual(mouseX)
        val virtualY = HudResolution.toVirtual(mouseY)
        val target = dragging ?: elements.lastOrNull { it.contains(virtualX, virtualY) }
        if (target != null) {
            target.setScale(target.scale() + (verticalAmount * SCALE_STEP).toFloat())
            return true
        }

        if (gridEnabled) {
            val step = if (verticalAmount > 0) GRID_STEP else -GRID_STEP
            gridSize = (gridSize + step).coerceIn(MIN_GRID_SIZE, MAX_GRID_SIZE)
            return true
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun onClose() {
        ConfigManager.save()
        Minecraft.getInstance().setScreen(parent)
    }

    override fun removed() {
        HudManager.setEditing(false)
        super.removed()
    }

    private fun drag(element: HudElement, mouseX: Float, mouseY: Float) {
        var x = mouseX - dragOffsetX
        var y = mouseY - dragOffsetY
        if (gridEnabled) {
            x = (x / gridSize).roundToInt() * gridSize.toFloat()
            y = (y / gridSize).roundToInt() * gridSize.toFloat()
        }

        val anchorOffset = if (element.isCentered) element.scaledWidth() / 2f else 0f
        val maxX = (HudResolution.width() - element.scaledWidth() + anchorOffset).coerceAtLeast(anchorOffset)
        val maxY = (HudResolution.height() - element.scaledHeight()).coerceAtLeast(0f)
        element.setPosition(x.coerceIn(anchorOffset, maxX), y.coerceIn(0f, maxY))
    }

    private fun drawGrid(context: GuiGraphicsExtractor) {
        val gridWidth = HudResolution.width().toInt()
        val gridHeight = HudResolution.height().toInt()

        var x = 0
        while (x <= gridWidth) {
            context.fill(x, 0, x + 1, gridHeight, GRID_COLOR)
            x += gridSize
        }

        var y = 0
        while (y <= gridHeight) {
            context.fill(0, y, gridWidth, y + 1, GRID_COLOR)
            y += gridSize
        }

        context.fill(0, gridHeight / 2, gridWidth, gridHeight / 2 + 1, CENTER_LINE_COLOR)
        context.fill(gridWidth / 2, 0, gridWidth / 2 + 1, gridHeight, CENTER_LINE_COLOR)
    }

    private fun drawFrame(context: GuiGraphicsExtractor, element: HudElement, active: Boolean) {
        val left = element.left().roundToInt()
        val top = element.top().roundToInt()
        val right = (element.left() + element.scaledWidth()).roundToInt().coerceAtLeast(left + 1)
        val bottom = (element.top() + element.scaledHeight()).roundToInt().coerceAtLeast(top + 1)
        val border = if (active) Theme.Primary.color else IDLE_BORDER_COLOR

        context.fill(left, top, right, bottom, FRAME_COLOR)
        context.fill(left, top, right, top + 1, border)
        context.fill(left, bottom - 1, right, bottom, border)
    }

    private fun drawInfo(context: GuiGraphicsExtractor, element: HudElement) {
        val font = Minecraft.getInstance().font
        val scale = String.format(Locale.US, "%.2f", element.scale())
        val text = "${element.name()} §7(${element.x().roundToInt()}, ${element.y().roundToInt()}) @ §f${scale}x"
        context.text(font, text, width / 2 - font.width(text) / 2, 10, 0xFFFFFFFF.toInt())
    }

    private class EditorButton(
        private val x: Int,
        private val y: Int,
        private val width: Int,
        private val label: () -> String,
        val onClick: () -> Unit
    ) {
        fun contains(mouseX: Double, mouseY: Double): Boolean =
            mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + BUTTON_HEIGHT

        fun render(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
            val hovered = contains(mouseX.toDouble(), mouseY.toDouble())
            val font = Minecraft.getInstance().font
            val text = label()

            context.fill(x, y, x + width, y + BUTTON_HEIGHT, if (hovered) BUTTON_HOVER_COLOR else BUTTON_COLOR)
            RenderUtils.drawBorder(context, x, y, width, BUTTON_HEIGHT, if (hovered) Theme.Primary.color else IDLE_BORDER_COLOR)
            context.text(font, text, x + width / 2 - font.width(text) / 2, y + (BUTTON_HEIGHT - font.lineHeight) / 2 + 1, 0xFFFFFFFF.toInt())
        }
    }

    companion object {
        private const val HINT = "Drag to move | Scroll to scale | Esc to save"

        private const val GRID_BUTTON_WIDTH = 100
        private const val STEP_BUTTON_WIDTH = 24
        private const val RESET_BUTTON_WIDTH = 70
        private const val BUTTON_GAP = 6
        private const val BUTTON_HEIGHT = 20
        private const val BUTTONS_WIDTH = GRID_BUTTON_WIDTH + STEP_BUTTON_WIDTH * 2 + RESET_BUTTON_WIDTH + BUTTON_GAP * 3

        private const val GRID_STEP = 5
        private const val MIN_GRID_SIZE = 5
        private const val MAX_GRID_SIZE = 50
        private const val SCALE_STEP = 0.1

        private const val BACKGROUND_COLOR = 0x40000000
        private const val GRID_COLOR = 0x1EFFFFFF
        private const val CENTER_LINE_COLOR = 0x80FFFFFF.toInt()
        private const val FRAME_COLOR = 0x960A0A0A.toInt()
        private const val IDLE_BORDER_COLOR = 0x28FFFFFF
        private const val BUTTON_COLOR = 0xC0101010.toInt()
        private const val BUTTON_HOVER_COLOR = 0xE0202020.toInt()
        private const val HINT_COLOR = 0xFFB0B0B0.toInt()
    }
}
