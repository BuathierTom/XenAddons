package xenus.xenaddons.gui.odin.ui

import xenus.xenaddons.gui.odin.ui.animations.LinearAnimation

class HoverHandler(delay: Long) {
    private val animation = LinearAnimation<Float>(delay)
    var isHovered: Boolean = false
        private set

    fun percent(): Float {
        if (!animation.isAnimating()) return if (isHovered) 100f else 0f
        return if (isHovered) animation.getPercent() else 100f - animation.getPercent()
    }

    fun handle(mouseX: Float, mouseY: Float, x: Float, y: Float, width: Float, height: Float) {
        val currentlyHovered = mouseX in x..(x + width) && mouseY in y..(y + height)
        if (currentlyHovered != isHovered) {
            animation.start()
            isHovered = currentlyHovered
        }
    }
}
