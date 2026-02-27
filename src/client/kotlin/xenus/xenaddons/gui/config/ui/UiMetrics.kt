package xenus.xenaddons.gui.config.ui

import xyz.meowing.knit.api.render.KnitResolution

object UiMetrics {
    var scale: Float = 1.0f
        private set

    fun updateFromResolution() {
        val widthFactor = KnitResolution.windowWidth / 1920f
        val heightFactor = KnitResolution.windowHeight / 1080f
        scale = minOf(widthFactor, heightFactor).coerceIn(0.80f, 1.35f)
    }

    fun px(value: Float): Float = value * scale
}
