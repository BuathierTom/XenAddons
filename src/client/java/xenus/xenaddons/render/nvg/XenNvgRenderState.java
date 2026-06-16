package xenus.xenaddons.render.nvg;

import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;

public record XenNvgRenderState(
    int x1,
    int y1,
    int x2,
    int y2,
    float scale,
    ScreenRect scissorArea,
    ScreenRect bounds,
    Runnable renderContent
) implements SpecialGuiElementRenderState {
    public XenNvgRenderState(int x1, int y1, int x2, int y2, float scale, ScreenRect scissorArea, Runnable renderContent) {
        this(
            x1,
            y1,
            x2,
            y2,
            scale,
            scissorArea,
            SpecialGuiElementRenderState.createBounds(x1, y1, x2, y2, scissorArea),
            renderContent
        );
    }
}
