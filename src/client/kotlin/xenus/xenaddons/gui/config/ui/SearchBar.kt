package xenus.xenaddons.gui.config.ui

import net.minecraft.client.MinecraftClient
import xyz.meowing.vexel.animations.EasingType
import xyz.meowing.vexel.animations.colorTo
import xyz.meowing.vexel.elements.TextInput
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.components.base.VexelElement
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.SvgImage
import xenus.xenaddons.gui.HudEditorScreen
import xenus.xenaddons.gui.ui.Theme
import java.awt.Color

class SearchBar(
    uiScale: Float,
    private val onSearch: (String) -> Unit
) : VexelElement<SearchBar>() {
    private val s = uiScale
    var text: String = ""
        private set

    val input = TextInput(
        placeholder = "Search...",
        fontSize = 20f * s,
        backgroundColor = Theme.Bg.color,
        borderColor = Theme.Primary.color,
        hoverColor = null,
        pressedColor = Theme.BgDark.color,
        borderRadius = 8f * s,
        borderThickness = 2f,
        padding = floatArrayOf(8f * s, 12f * s, 8f * s, 12f * s)
    )
        .setSizing(310f * s, Size.Pixels, 40f * s, Size.Pixels)
        .setPositioning(0f, Pos.ParentPixels, 0f, Pos.ParentPixels)
        .childOf(this)

    val buttonContainer = Rectangle(
        backgroundColor = Theme.Bg.color,
        borderColor = Theme.Primary.color,
        hoverColor = null,
        pressedColor = Theme.BgDark.color,
        borderRadius = 8f * s,
        borderThickness = 2f,
    )
        .setSizing(40f * s, Size.Pixels, 40f * s, Size.Pixels)
        .setPositioning(6f * s, Pos.AfterSibling, 0f, Pos.ParentPixels)
        .childOf(this)

    val editGuiLocationsButton = SvgImage("/assets/gui/editLocations.svg", color = Color.white)
        .setSizing(100f, Size.ParentPerc, 100, Size.ParentPerc)
        .setPositioning(Pos.ParentCenter, Pos.ParentCenter)
        .childOf(buttonContainer)

    init {
        setSizing(350f * s, Size.Pixels, 40f * s, Size.Pixels)
        setPositioning(0f, Pos.ScreenCenter, -50f * s, Pos.ScreenPixels)
        alignBottom()

        val openHudEditor: () -> Boolean = {
            val mc = MinecraftClient.getInstance()
            mc.execute {
                mc.setScreen(HudEditorScreen(mc.currentScreen))
            }
            true
        }

        editGuiLocationsButton.onClick { _, _, _ -> openHudEditor() }
        buttonContainer.onClick { _, _, _ -> openHudEditor() }

        buttonContainer
            .onHover(
                { _, _ ->
                    buttonContainer.colorTo(Theme.BgLight.color, 200, EasingType.EASE_IN)
                    ClickGUI.updateTooltip("HUD Editor")
                },
                { _, _ ->
                    buttonContainer.colorTo(Theme.Bg.color, 200, EasingType.EASE_IN)
                    ClickGUI.updateTooltip("")
                }
            )

        input
            .onValueChange { newValue ->
                text = newValue as String
                onSearch(text)
            }
            .onHover(
                { _, _ -> input.background.colorTo(Theme.BgLight.color, 200, EasingType.EASE_IN) },
                { _, _ -> input.background.colorTo(Theme.Bg.color, 200, EasingType.EASE_IN) }
            )
            .background.dropShadow(10f * s, 3f * s, Theme.BgDark.color)
    }

    override fun onRender(mouseX: Float, mouseY: Float) {}
}
