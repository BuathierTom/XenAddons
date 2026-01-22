package xenus.xenaddons.gui

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractSliderButton
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import xenus.xenaddons.config.XenAddonsConfig

class XenAddonsScreen(private val parent: Screen?) : Screen(Component.literal("XenAddons")) {

        // --- Colors ---
        private val COL_CATEGORY_PANEL = 0xFF1A1A1A.toInt()
        private val COL_FEATURE_PANEL = 0xFF141414.toInt()
        private val COL_SETTINGS_PANEL = 0xFF171717.toInt()
        private val COL_TOP_BAR = 0xFF1A1A1A.toInt()
        private val COL_ACCENT = 0xFF3B82F6.toInt()
        private val COL_TEXT_MAIN = 0xFFFFFFFF.toInt()
        private val COL_TEXT_DIM = 0xFFAAAAAA.toInt()
        private val COL_HOVER = 0xFF232323.toInt()
        private val COL_DIVIDER = 0xFF2D2D2D.toInt()

        // --- Layout ---
        private val GUI_WIDTH = 380
        private val GUI_HEIGHT = 220
        private val TOP_BAR_HEIGHT = 30

        private val CAT_WIDTH_RATIO = 0.22
        private val FEAT_WIDTH_RATIO = 0.30

        // --- State ---
        private enum class Category {
                MISC
        }

        private var selectedCategory = Category.MISC
        private var selectedFeature: Feature? = null

        // Scroll State
        private var scrollOffset = 0.0
        private var maxScroll = 0
        private val settingWidgets = mutableListOf<AbstractWidget>()

        private data class Feature(
                val name: String,
                val category: Category,
                val description: String,
                val items: List<ConfigItem>
        )

        private open class ConfigItem(val label: String, val height: Int)

        private class SliderItem(
                label: String,
                val min: Float,
                val max: Float,
                val get: () -> Float,
                val set: (Float) -> Unit
        ) : ConfigItem(label, 24)
        private class ToggleItem(
                label: String,
                val get: () -> Boolean,
                val set: (Boolean) -> Unit
        ) : ConfigItem(label, 20)
        private class HeaderItem(label: String) : ConfigItem(label, 16)

        // --- Features ---
        private val allFeatures =
                listOf(
                        Feature(
                                "Animation",
                                Category.MISC,
                                "Animation Settings",
                                listOf(
                                        HeaderItem("Scaling"),
                                        SliderItem(
                                                "Size",
                                                -3f,
                                                3f,
                                                { XenAddonsConfig.itemScaleSize },
                                                { XenAddonsConfig.itemScaleSize = it }
                                        ),
                                        ToggleItem(
                                                "Scale Swing",
                                                { XenAddonsConfig.scaleSwing },
                                                { XenAddonsConfig.scaleSwing = it }
                                        ),
                                        SliderItem(
                                                "Scale X",
                                                -3f,
                                                3f,
                                                { XenAddonsConfig.itemScaleX },
                                                { XenAddonsConfig.itemScaleX = it }
                                        ),
                                        SliderItem(
                                                "Scale Y",
                                                -3f,
                                                3f,
                                                { XenAddonsConfig.itemScaleY },
                                                { XenAddonsConfig.itemScaleY = it }
                                        ),
                                        SliderItem(
                                                "Scale Z",
                                                -3f,
                                                3f,
                                                { XenAddonsConfig.itemScaleZ },
                                                { XenAddonsConfig.itemScaleZ = it }
                                        ),
                                        HeaderItem("Rotation"),
                                        SliderItem(
                                                "Yaw",
                                                -180f,
                                                180f,
                                                { XenAddonsConfig.itemScaleYaw },
                                                { XenAddonsConfig.itemScaleYaw = it }
                                        ),
                                        SliderItem(
                                                "Pitch",
                                                -180f,
                                                180f,
                                                { XenAddonsConfig.itemScalePitch },
                                                { XenAddonsConfig.itemScalePitch = it }
                                        ),
                                        SliderItem(
                                                "Roll",
                                                -180f,
                                                180f,
                                                { XenAddonsConfig.itemScaleRoll },
                                                { XenAddonsConfig.itemScaleRoll = it }
                                        ),
                                        SliderItem(
                                                "Speed",
                                                -5f,
                                                5f,
                                                { XenAddonsConfig.itemScaleSpeed },
                                                { XenAddonsConfig.itemScaleSpeed = it }
                                        ),
                                        HeaderItem("Swing"),
                                        ToggleItem(
                                                "No Swing",
                                                { XenAddonsConfig.noSwing },
                                                { XenAddonsConfig.noSwing = it }
                                        ),
                                        ToggleItem(
                                                "No Terminator Swing",
                                                { XenAddonsConfig.noTerminatorSwing },
                                                { XenAddonsConfig.noTerminatorSwing = it }
                                        ),
                                        HeaderItem("Fixes & Misc"),
                                        ToggleItem(
                                                "Ignore Haste",
                                                { XenAddonsConfig.ignoreHaste },
                                                { XenAddonsConfig.ignoreHaste = it }
                                        ),
                                        ToggleItem(
                                                "No Equip Reset",
                                                { XenAddonsConfig.noEquipReset },
                                                { XenAddonsConfig.noEquipReset = it }
                                        )
                                )
                        )
                )

        override fun init() {
                this.clearWidgets()
                this.settingWidgets.clear()

                if (selectedFeature == null) {
                        val catFeatures = allFeatures.filter { it.category == selectedCategory }
                        if (catFeatures.isNotEmpty()) selectedFeature = catFeatures[0]
                }

                val rootX = (width - GUI_WIDTH) / 2
                val rootY = (height - GUI_HEIGHT) / 2

                val catW = (GUI_WIDTH * CAT_WIDTH_RATIO).toInt()
                val featW = (GUI_WIDTH * FEAT_WIDTH_RATIO).toInt()
                val setW = GUI_WIDTH - catW - featW

                val catX = rootX
                val featX = rootX + catW
                val setX = featX + featW

                val contentStartY = rootY + TOP_BAR_HEIGHT + 5

                // 1. Categories
                var currentY = contentStartY
                Category.values().forEach { cat ->
                        val btn =
                                object :
                                        Button(
                                                catX + 2,
                                                currentY,
                                                catW - 4,
                                                20,
                                                Component.literal(cat.name),
                                                {
                                                        selectedCategory = cat
                                                        val firstF =
                                                                allFeatures.find {
                                                                        it.category == cat
                                                                }
                                                        selectedFeature = firstF
                                                        scrollOffset = 0.0
                                                        refreshWidgets()
                                                },
                                                { Supplier -> Component.empty() }
                                        ) {
                                        override fun renderWidget(
                                                graphics: GuiGraphics,
                                                mouseX: Int,
                                                mouseY: Int,
                                                partialTick: Float
                                        ) {
                                                val isSelected = (selectedCategory == cat)
                                                val isHovered =
                                                        (mouseX in x..(x + width) &&
                                                                mouseY in y..(y + height))
                                                if (isSelected) {
                                                        graphics.fill(
                                                                x,
                                                                y,
                                                                x + width,
                                                                y + height,
                                                                COL_HOVER
                                                        )
                                                        graphics.fill(
                                                                x - 2,
                                                                y,
                                                                x,
                                                                y + height,
                                                                COL_ACCENT
                                                        )
                                                } else if (isHovered) {
                                                        graphics.fill(
                                                                x,
                                                                y,
                                                                x + width,
                                                                y + height,
                                                                0x11FFFFFF
                                                        )
                                                }
                                                val color =
                                                        if (isSelected) COL_TEXT_MAIN
                                                        else COL_TEXT_DIM
                                                val label =
                                                        cat.name.lowercase().replaceFirstChar {
                                                                it.uppercase()
                                                        }

                                                graphics.drawString(
                                                        font,
                                                        label,
                                                        x + 8,
                                                        y + 6,
                                                        color,
                                                        false
                                                )
                                        }
                                }
                        this.addRenderableWidget(btn)
                        currentY += 22
                }

                // Edit HUD
                val hudBtnY = rootY + GUI_HEIGHT - 26
                val hudBtn =
                        object :
                                Button(
                                        catX + 2,
                                        hudBtnY,
                                        catW - 4,
                                        22,
                                        Component.literal("Edit HUD"),
                                        {
                                                this.onClose()
                                                net.minecraft.client.Minecraft.getInstance()
                                                        .player
                                                        ?.displayClientMessage(
                                                                Component.literal(
                                                                        "§c[XenAddons] HUD Editor coming soon!"
                                                                ),
                                                                false
                                                        )
                                        },
                                        { Supplier -> Component.empty() }
                                ) {
                                override fun renderWidget(
                                        graphics: GuiGraphics,
                                        mouseX: Int,
                                        mouseY: Int,
                                        partialTick: Float
                                ) {
                                        val isHovered =
                                                (mouseX in x..(x + width) &&
                                                        mouseY in y..(y + height))
                                        if (isHovered)
                                                graphics.fill(
                                                        x,
                                                        y,
                                                        x + width,
                                                        y + height,
                                                        COL_HOVER
                                                )

                                        graphics.drawString(
                                                font,
                                                "Edit HUD",
                                                x + 8,
                                                y + 7,
                                                COL_ACCENT,
                                                false
                                        )
                                }
                        }
                this.addRenderableWidget(hudBtn)

                // 2. Features
                currentY = contentStartY
                val feats = allFeatures.filter { it.category == selectedCategory }
                feats.forEach { feat ->
                        val btn =
                                object :
                                        Button(
                                                featX + 2,
                                                currentY,
                                                featW - 4,
                                                20,
                                                Component.literal(feat.name),
                                                {
                                                        selectedFeature = feat
                                                        scrollOffset = 0.0
                                                        refreshWidgets()
                                                },
                                                { Supplier -> Component.empty() }
                                        ) {
                                        override fun renderWidget(
                                                graphics: GuiGraphics,
                                                mouseX: Int,
                                                mouseY: Int,
                                                partialTick: Float
                                        ) {
                                                val isSelected = (selectedFeature == feat)
                                                val isHovered =
                                                        (mouseX in x..(x + width) &&
                                                                mouseY in y..(y + height))
                                                if (isSelected) {
                                                        graphics.fill(
                                                                x,
                                                                y,
                                                                x + width,
                                                                y + height,
                                                                COL_HOVER
                                                        )
                                                } else if (isHovered) {
                                                        graphics.fill(
                                                                x,
                                                                y,
                                                                x + width,
                                                                y + height,
                                                                0x11FFFFFF
                                                        )
                                                }
                                                val color =
                                                        if (isSelected) COL_TEXT_MAIN
                                                        else COL_TEXT_DIM

                                                graphics.drawString(
                                                        font,
                                                        feat.name,
                                                        x + 8,
                                                        y + 6,
                                                        color,
                                                        false
                                                )
                                        }
                                }
                        this.addRenderableWidget(btn)
                        currentY += 22
                }

                // 3. Settings
                if (selectedFeature != null) {
                        initSettingsWidgets(setX, setW, contentStartY)
                }
        }

        private fun initSettingsWidgets(setX: Int, setW: Int, startY: Int) {
                val items = selectedFeature?.items ?: return
                items.forEach { item ->
                        when (item) {
                                is HeaderItem -> {
                                        val w =
                                                object :
                                                        Button(
                                                                0,
                                                                0,
                                                                setW - 14,
                                                                item.height,
                                                                Component.literal(item.label),
                                                                {},
                                                                { s -> Component.empty() }
                                                        ) {
                                                        override fun renderWidget(
                                                                graphics: GuiGraphics,
                                                                mouseX: Int,
                                                                mouseY: Int,
                                                                partialTick: Float
                                                        ) {
                                                                graphics.drawString(
                                                                        font,
                                                                        item.label,
                                                                        x,
                                                                        y + 4,
                                                                        COL_ACCENT,
                                                                        false
                                                                )
                                                                graphics.fill(
                                                                        x,
                                                                        y + 14,
                                                                        x + width,
                                                                        y + 15,
                                                                        COL_DIVIDER
                                                                )
                                                        }
                                                }
                                        w.active = false
                                        settingWidgets.add(w)
                                        this.addWidget(w)
                                }
                                is SliderItem -> {
                                        val initial =
                                                ((item.get() - item.min) / (item.max - item.min))
                                                        .coerceIn(0f, 1f)
                                                        .toDouble()
                                        val slider =
                                                object :
                                                        AbstractSliderButton(
                                                                0,
                                                                0,
                                                                setW - 14,
                                                                item.height,
                                                                Component.literal(item.label),
                                                                initial
                                                        ) {
                                                        init {
                                                                this.updateMessage()
                                                        }
                                                        override fun updateMessage() {
                                                                this.message = Component.empty()
                                                        }
                                                        override fun applyValue() {
                                                                val newVal =
                                                                        item.min +
                                                                                (item.max -
                                                                                        item.min) *
                                                                                        this.value
                                                                                                .toFloat()
                                                                item.set(newVal)
                                                        }

                                                        override fun renderWidget(
                                                                graphics: GuiGraphics,
                                                                mouseX: Int,
                                                                mouseY: Int,
                                                                partialTick: Float
                                                        ) {
                                                                val value = item.get()
                                                                val progress =
                                                                        ((value - item.min) /
                                                                                        (item.max -
                                                                                                item.min))
                                                                                .coerceIn(0f, 1f)
                                                                val str =
                                                                        "${item.label}: ${String.format("%.2f", value)}"

                                                                graphics.drawString(
                                                                        font,
                                                                        str,
                                                                        x,
                                                                        y + 2,
                                                                        0xFFDDDDDD.toInt(),
                                                                        false
                                                                )

                                                                val trackY = y + 14
                                                                graphics.fill(
                                                                        x,
                                                                        trackY,
                                                                        x + width,
                                                                        trackY + 4,
                                                                        0xFF333333.toInt()
                                                                )
                                                                val handleX =
                                                                        x +
                                                                                (width * progress)
                                                                                        .toInt() - 3
                                                                val handleY = trackY - 2
                                                                val col =
                                                                        if (this.isHovered)
                                                                                0xFF5B92FF.toInt()
                                                                        else COL_ACCENT
                                                                graphics.fill(
                                                                        handleX,
                                                                        handleY,
                                                                        handleX + 6,
                                                                        handleY + 8,
                                                                        col
                                                                )
                                                        }
                                                }
                                        settingWidgets.add(slider)
                                        this.addWidget(slider)
                                }
                                is ToggleItem -> {
                                        val toggle =
                                                object :
                                                        Button(
                                                                0,
                                                                0,
                                                                setW - 14,
                                                                item.height,
                                                                Component.literal(item.label),
                                                                { item.set(!item.get()) },
                                                                { s -> Component.empty() }
                                                        ) {
                                                        override fun renderWidget(
                                                                graphics: GuiGraphics,
                                                                mouseX: Int,
                                                                mouseY: Int,
                                                                partialTick: Float
                                                        ) {
                                                                val on = item.get()

                                                                graphics.drawString(
                                                                        font,
                                                                        item.label,
                                                                        x,
                                                                        y + 6,
                                                                        0xFFFFFFFF.toInt(),
                                                                        false
                                                                )

                                                                val boxSize = 10
                                                                val boxX = x + width - boxSize - 2
                                                                val boxY = y + 5
                                                                val col =
                                                                        if (on) COL_ACCENT
                                                                        else 0xFF333333.toInt()
                                                                graphics.fill(
                                                                        boxX,
                                                                        boxY,
                                                                        boxX + boxSize,
                                                                        boxY + boxSize,
                                                                        col
                                                                )
                                                                if (on)
                                                                        graphics.fill(
                                                                                boxX + 2,
                                                                                boxY + 2,
                                                                                boxX + boxSize - 2,
                                                                                boxY + boxSize - 2,
                                                                                0xFFFFFFFF.toInt()
                                                                        )
                                                        }
                                                }
                                        settingWidgets.add(toggle)
                                        this.addWidget(toggle)
                                }
                        }
                }

                val contentHeight = items.sumOf { it.height + 4 } + 10
                val viewportHeight = GUI_HEIGHT - TOP_BAR_HEIGHT - 10
                maxScroll = (contentHeight - viewportHeight).coerceAtLeast(0)
        }

        private fun refreshWidgets() {
                this.clearWidgets()
                this.init()
        }

        override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
                graphics.fill(0, 0, this.width, this.height, 0xAA000000.toInt())

                val rootX = (width - GUI_WIDTH) / 2
                val rootY = (height - GUI_HEIGHT) / 2

                val catW = (GUI_WIDTH * CAT_WIDTH_RATIO).toInt()
                val featW = (GUI_WIDTH * FEAT_WIDTH_RATIO).toInt()
                val setW = GUI_WIDTH - catW - featW
                val catX = rootX
                val featX = rootX + catW
                val setX = featX + featW

                // Backgrounds
                graphics.fill(catX, rootY, catX + catW, rootY + GUI_HEIGHT, COL_CATEGORY_PANEL)
                graphics.fill(featX, rootY, featX + featW, rootY + GUI_HEIGHT, COL_FEATURE_PANEL)
                graphics.fill(setX, rootY, setX + setW, rootY + GUI_HEIGHT, COL_SETTINGS_PANEL)

                // Render Categories and Features (via super.render)
                super.render(graphics, mouseX, mouseY, partialTick)

                // Settings Layout: Manually Rendered with Scissor
                val viewportTop = rootY + TOP_BAR_HEIGHT + 5
                val viewportBottom = rootY + GUI_HEIGHT - 5
                var currentY = (viewportTop - scrollOffset).toInt()

                settingWidgets.forEach { widget ->
                        widget.x = setX + 7
                        widget.y = currentY
                        val visible =
                                (currentY + widget.height > viewportTop) &&
                                        (currentY < viewportBottom)
                        widget.visible = visible
                        currentY += widget.height + 4
                }

                val scale =
                        net.minecraft.client.Minecraft.getInstance()
                                .window
                                .guiScale
                                .toInt()
                                .coerceAtLeast(1)
                graphics.enableScissor(setX, viewportTop, setX + setW, viewportBottom)
                settingWidgets.forEach { widget ->
                        // Manual visible check in loop handles logic, checking widget.visible is
                        // redundant if set above but good for safety
                        if (widget.visible) {
                                widget.render(graphics, mouseX, mouseY, partialTick)
                        }
                }
                graphics.disableScissor()

                // Top Bar (Draw over everything)
                graphics.fill(rootX, rootY, rootX + GUI_WIDTH, rootY + TOP_BAR_HEIGHT, COL_TOP_BAR)
                graphics.fill(
                        rootX,
                        rootY + TOP_BAR_HEIGHT,
                        rootX + GUI_WIDTH,
                        rootY + TOP_BAR_HEIGHT + 1,
                        COL_DIVIDER
                )
                graphics.drawString(font, "XenAddons", rootX + 12, rootY + 11, COL_ACCENT, false)

                // Vertical Dividers
                graphics.fill(catX + catW, rootY, catX + catW + 1, rootY + GUI_HEIGHT, COL_DIVIDER)
                graphics.fill(
                        featX + featW,
                        rootY,
                        featX + featW + 1,
                        rootY + GUI_HEIGHT,
                        COL_DIVIDER
                )
        }

        override fun mouseScrolled(
                mouseX: Double,
                mouseY: Double,
                horizontalAmount: Double,
                verticalAmount: Double
        ): Boolean {
                val rootX = (width - GUI_WIDTH) / 2
                val catW = (GUI_WIDTH * CAT_WIDTH_RATIO).toInt()
                val featW = (GUI_WIDTH * FEAT_WIDTH_RATIO).toInt()
                val setX = rootX + catW + featW

                if (mouseX >= setX && mouseX <= setX + (GUI_WIDTH - catW - featW)) {
                        scrollOffset =
                                (scrollOffset - verticalAmount * 20).coerceIn(
                                        0.0,
                                        maxScroll.toDouble()
                                )
                        return true
                }
                return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
        }

        override fun onClose() {
                xenus.xenaddons.config.XenAddonsConfig.save()
                super.onClose()
        }
}
