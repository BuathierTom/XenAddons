package xenus.xenaddons.features.dungeons

import xenus.xenaddons.api.dungeon.Dungeons
import xenus.xenaddons.config.XenAddonsConfig
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object DeathsDisplay {
    fun initialize() {
        HudRenderCallback.EVENT.register { ctx, _ ->
            if (!XenAddonsConfig.dungeonDeathsDisplay) return@register
            if (!Dungeons.started.value && Minecraft.getInstance().player != null) {
                // Only show if in dungeon or for testing? Devonian calls it "isActiveState" which implies "in dungeon"
                // Let's assume user wants to see it when in Dungeons.
                 // We can check if in dungeon via Dungeons.floor != None or Location.area
            }
            if (xenus.xenaddons.api.Location.area != "catacombs") return@register

            render(ctx)
        }
    }

    private fun render(ctx: GuiGraphics) {
        val count = Dungeons.deaths.value
        val color = if (count > 2) "§4" else "§c"
        val text = "§8§lDeaths§f: $color$count"
        
        ctx.drawString(Minecraft.getInstance().font, text, 10, 10, 0xFFFFFF)
    }
}
