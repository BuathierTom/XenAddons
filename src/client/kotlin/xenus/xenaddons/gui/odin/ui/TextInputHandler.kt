package xenus.xenaddons.gui.odin.ui

import net.minecraft.client.Minecraft
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.util.StringUtil
import com.mojang.blaze3d.platform.InputConstants
import xenus.xenaddons.render.XenRenderer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class TextInputHandler(
    private val textProvider: () -> String,
    private val textSetter: (String) -> Unit,
    private val maxLength: Int = Int.MAX_VALUE
) {
    private inline val text: String get() = textProvider()

    var x = 0f
    var y = 0f
    var width = 0f
    var height = 20f

    var listening = false
        private set

    private var caret = 0
        set(value) {
            field = value.coerceIn(0, text.length)
            caretBlinkTime = System.currentTimeMillis()
        }

    private var selection = 0
    private var selectionWidth = 0f
    private var textOffset = 0f
    private var caretX = 0f

    private var caretBlinkTime = System.currentTimeMillis()
    private var lastClickTime = 0L
    private var dragging = false
    private var clickCount = 1

    private val history = mutableListOf<String>()
    private var historyIndex = -1
    private var lastSavedText = ""
    private var previousMouse = 0f to 0f

    init {
        caret = text.length
        selection = caret
        saveState()
        updateCaretPosition()
    }

    fun draw(mouseX: Float, mouseY: Float, selectionColor: Int, textColor: Int, caretColor: Int) {
        if (caret > text.length) {
            caret = text.length
        } else {
            updateCaretPosition()
        }

        if (previousMouse != mouseX to mouseY) mouseDragged(mouseX)
        previousMouse = mouseX to mouseY

        val startX = x + 4f + min(caretX, caretX + selectionWidth) - textOffset
        if (selectionWidth != 0f) {
            XenRenderer.pushScissor(x, y, width, height)
            XenRenderer.rect(startX, y + 1f, abs(selectionWidth), height - 2f, selectionColor, 3f)
            XenRenderer.popScissor()
        }

        if (listening) {
            val time = System.currentTimeMillis()
            if (time - caretBlinkTime < 500) {
                XenRenderer.line(
                    x + caretX + 4f - textOffset,
                    y + 2f,
                    x + caretX + 4f - textOffset,
                    y + height - 2f,
                    1.5f,
                    caretColor
                )
            } else if (time - caretBlinkTime > 1000) {
                caretBlinkTime = time
            }
        }

        XenRenderer.pushScissor(x, y, width, height)
        XenRenderer.text(text, x + 4f - textOffset, y + 2f, height - 4f, textColor, XenRenderer.defaultFont)
        XenRenderer.popScissor()
    }

    fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (!isHovered(mouseX, mouseY)) {
            stopListening()
            return false
        }
        if (button != 0) return false

        listening = true
        dragging = true

        val current = System.currentTimeMillis()
        clickCount = if (current - lastClickTime < 200) clickCount + 1 else 1
        lastClickTime = current

        when (clickCount) {
            1 -> {
                caretFromMouse(mouseX)
                clearSelection()
            }
            2 -> selectWord()
            3 -> selectAll()
            else -> clickCount = 0
        }
        return true
    }

    fun mouseReleased() {
        dragging = false
    }

    fun keyPressed(input: KeyEvent): Boolean {
        if (!listening) return false
        val ctrl = input.hasControlDown()
        val shift = input.hasShiftDown()

        val handled = when (input.key) {
            InputConstants.KEY_BACKSPACE -> {
                if (selection != caret) deleteSelection()
                else if (ctrl) {
                    val previousSpace = getPreviousSpace()
                    textSetter(text.removeRangeSafe(previousSpace, caret))
                    caret = previousSpace
                    saveState()
                } else if (caret != 0) {
                    textSetter(text.dropAt(caret, -1))
                    caret--
                    saveState()
                }
                clearSelection()
                true
            }
            InputConstants.KEY_DELETE -> {
                if (selection != caret) deleteSelection()
                else if (ctrl) {
                    val nextSpace = getNextSpace()
                    textSetter(text.removeRangeSafe(caret, nextSpace))
                    saveState()
                } else if (caret != text.length) {
                    textSetter(text.dropAt(caret, 1))
                    saveState()
                }
                clearSelection()
                true
            }
            InputConstants.KEY_RIGHT -> {
                if (caret != text.length) {
                    caret = if (ctrl) getNextSpace() else caret + 1
                    if (!shift) selection = caret
                    true
                } else false
            }
            InputConstants.KEY_LEFT -> {
                if (caret != 0) {
                    caret = if (ctrl) getPreviousSpace() else caret - 1
                    if (!shift) selection = caret
                    true
                } else false
            }
            InputConstants.KEY_HOME -> {
                caret = 0
                if (!shift) selection = 0
                true
            }
            InputConstants.KEY_END -> {
                caret = text.length
                if (!shift) selection = caret
                true
            }
            InputConstants.KEY_ESCAPE, InputConstants.KEY_RETURN -> {
                stopListening()
                true
            }
            else -> {
                if (ctrl && !shift) {
                    when (input.key) {
                        InputConstants.KEY_V -> {
                            insert(Minecraft.getInstance().keyboardHandler.clipboard)
                            true
                        }
                        InputConstants.KEY_C -> {
                            if (caret != selection) {
                                Minecraft.getInstance().keyboardHandler.clipboard = text.substringSafe(caret, selection)
                                true
                            } else false
                        }
                        InputConstants.KEY_X -> {
                            if (caret != selection) {
                                Minecraft.getInstance().keyboardHandler.clipboard = text.substringSafe(caret, selection)
                                deleteSelection()
                                true
                            } else false
                        }
                        InputConstants.KEY_A -> {
                            selectAll()
                            true
                        }
                        InputConstants.KEY_W -> {
                            selectWord()
                            true
                        }
                        InputConstants.KEY_Z -> {
                            undo()
                            true
                        }
                        InputConstants.KEY_Y -> {
                            redo()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
        }

        updateCaretPosition()
        return handled
    }

    fun charTyped(input: CharacterEvent): Boolean {
        if (!listening) return false
        insert(StringUtil.filterText(input.codepointAsString()))
        return true
    }

    fun stopListening() {
        listening = false
        dragging = false
        clearSelection()
    }

    private fun mouseDragged(mouseX: Float) {
        if (dragging) caretFromMouse(mouseX)
    }

    private fun insert(string: String) {
        if (string.isEmpty()) return
        if (caret != selection) {
            textSetter(text.removeRangeSafe(caret, selection))
            caret = min(caret, selection)
        }

        val available = maxLength - text.length
        if (available <= 0) {
            clearSelection()
            updateCaretPosition()
            return
        }

        val filtered = StringUtil.filterText(string).take(available)
        if (filtered.isEmpty()) return

        textSetter(text.substringSafe(0, caret) + filtered + text.substring(caret))
        caret += filtered.length
        clearSelection()
        updateCaretPosition()
        saveState()
    }

    private fun deleteSelection() {
        if (caret == selection) return
        textSetter(text.removeRangeSafe(caret, selection))
        caret = min(caret, selection)
        clearSelection()
        updateCaretPosition()
        saveState()
    }

    private fun caretFromMouse(mouseX: Float) {
        val targetX = mouseX - (x + 4f - textOffset)
        var currentWidth = 0f
        var newCaret = 0
        for (index in text.indices) {
            val charWidth = textWidth(text[index].toString())
            if (currentWidth + charWidth / 2f > targetX) break
            currentWidth += charWidth
            newCaret = index + 1
        }
        caret = newCaret
        updateCaretPosition()
    }

    private fun updateCaretPosition() {
        selectionWidth = if (selection != caret) {
            val width = textWidth(text.substringSafe(selection, caret))
            if (selection <= caret) -width else width
        } else {
            0f
        }

        if (caret != 0) {
            val previousX = caretX
            caretX = textWidth(text.substringSafe(0, caret))
            if (previousX < caretX) {
                if (caretX - textOffset >= width - 8f) textOffset = caretX - (width - 8f)
            } else {
                if (caretX - textOffset <= 0f) textOffset = textWidth(text.substringSafe(0, max(caret - 1, 0)))
            }

            val fullWidth = textWidth(text)
            if (textOffset > 0f && fullWidth - textOffset < width - 8f) {
                textOffset = (fullWidth - (width - 8f)).coerceAtLeast(0f)
            }
        } else {
            caretX = 0f
            textOffset = 0f
        }
    }

    private fun clearSelection() {
        selection = caret
        selectionWidth = 0f
    }

    private fun selectWord() {
        var start = caret
        var end = caret
        while (start > 0 && !text[start - 1].isWhitespace()) start--
        while (end < text.length && !text[end].isWhitespace()) end++
        selection = start
        caret = end
        updateCaretPosition()
    }

    private fun selectAll() {
        selection = 0
        caret = text.length
        updateCaretPosition()
    }

    private fun getPreviousSpace(): Int {
        var start = caret
        while (start > 0) {
            if (start != caret && text[start - 1].isWhitespace()) break
            start--
        }
        return start
    }

    private fun getNextSpace(): Int {
        var end = caret
        while (end < text.length) {
            if (end != caret && text[end].isWhitespace()) break
            end++
        }
        return end
    }

    private fun saveState() {
        if (text == lastSavedText) return
        if (historyIndex < history.size - 1) {
            history.subList(historyIndex + 1, history.size).clear()
        }
        history.add(text)
        historyIndex = history.size - 1
        lastSavedText = text
    }

    private fun undo() {
        if (historyIndex <= 0) return
        historyIndex--
        textSetter(history[historyIndex])
        caret = text.length
        selection = caret
        lastSavedText = text
        updateCaretPosition()
    }

    private fun redo() {
        if (historyIndex >= history.size - 1) return
        historyIndex++
        textSetter(history[historyIndex])
        caret = text.length
        selection = caret
        lastSavedText = text
        updateCaretPosition()
    }

    private fun textWidth(value: String): Float = XenRenderer.textWidth(value, height - 4f, XenRenderer.defaultFont)

    private fun isHovered(mouseX: Float, mouseY: Float): Boolean =
        mouseX in x..(x + width) && mouseY in y..(y + height)

    private fun String.substringSafe(from: Int, to: Int): String {
        val start = min(from, to).coerceAtLeast(0)
        val end = max(from, to).coerceAtMost(length)
        if (start >= end) return ""
        return substring(start, end)
    }

    private fun String.removeRangeSafe(from: Int, to: Int): String =
        removeRange(min(from, to).coerceAtLeast(0), max(from, to).coerceAtMost(length))

    private fun String.dropAt(at: Int, amount: Int): String =
        removeRangeSafe(at, at + amount)
}
