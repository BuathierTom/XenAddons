package xenus.xenaddons.hud;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class HudResolution {
    private static final float REFERENCE_WIDTH = 960.0f;
    private static final float REFERENCE_HEIGHT = 540.0f;

    private static float scale = 1.0f;
    private static float width = REFERENCE_WIDTH;
    private static float height = REFERENCE_HEIGHT;

    private HudResolution() {}

    public static void refresh() {
        Window window = Minecraft.getInstance().getWindow();
        if (window == null) return;

        float guiWidth = window.getGuiScaledWidth();
        float guiHeight = window.getGuiScaledHeight();
        if (guiWidth <= 0.0f || guiHeight <= 0.0f) return;

        scale = Math.min(guiWidth / REFERENCE_WIDTH, guiHeight / REFERENCE_HEIGHT);
        width = guiWidth / scale;
        height = guiHeight / scale;
    }

    public static void push(GuiGraphicsExtractor context) {
        refresh();
        context.pose().pushMatrix();
        context.pose().scale(scale, scale);
    }

    public static void pop(GuiGraphicsExtractor context) {
        context.pose().popMatrix();
    }

    public static float scale() {
        return scale;
    }

    public static float width() {
        return width;
    }

    public static float height() {
        return height;
    }

    public static float toVirtual(double guiCoordinate) {
        return (float) (guiCoordinate / scale);
    }
}
