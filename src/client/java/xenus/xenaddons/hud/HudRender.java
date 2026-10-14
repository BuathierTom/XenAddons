package xenus.xenaddons.hud;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class HudRender {
    private HudRender() {}

    public static HudElement.Size text(GuiGraphicsExtractor context, String text, int color) {
        Font font = Minecraft.getInstance().font;
        context.text(font, text, 0, 0, color);
        return new HudElement.Size(font.width(text), font.lineHeight);
    }

    public static HudElement.Size centeredText(GuiGraphicsExtractor context, String text, int color) {
        Font font = Minecraft.getInstance().font;
        int width = font.width(text);
        context.text(font, text, -width / 2, 0, color);
        return new HudElement.Size(width, font.lineHeight);
    }

    public static HudElement.Size centeredText(GuiGraphicsExtractor context, Component text, int color) {
        Font font = Minecraft.getInstance().font;
        int width = font.width(text);
        context.text(font, text, -width / 2, 0, color);
        return new HudElement.Size(width, font.lineHeight);
    }

    public static HudElement.Size centeredLines(GuiGraphicsExtractor context, List<Component> lines, int color) {
        if (lines.isEmpty()) return HudElement.Size.NONE;

        Font font = Minecraft.getInstance().font;
        int lineHeight = font.lineHeight + 1;
        int widest = 0;
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            int width = font.width(line);
            widest = Math.max(widest, width);
            context.text(font, line, -width / 2, i * lineHeight, color);
        }
        return new HudElement.Size(widest, lineHeight * lines.size());
    }
}
