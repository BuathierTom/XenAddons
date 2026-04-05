package xenus.xenaddons.gui.odin.ui.animations

import kotlin.math.pow

class EaseInOutAnimation(duration: Long) : Animation<Float>(duration) {
    override fun get(start: Float, end: Float, reverse: Boolean): Float {
        if (!isAnimating()) return if (reverse) start else end
        return if (reverse) end + (start - end) * easeInOutCubic() else start + (end - start) * easeInOutCubic()
    }

    private fun easeInOutCubic(): Float {
        val x = getPercent() / 100f
        return if (x < 0.5f) {
            4f * x * x * x
        } else {
            1f - (-2f * x + 2f).pow(3) / 2f
        }
    }
}
