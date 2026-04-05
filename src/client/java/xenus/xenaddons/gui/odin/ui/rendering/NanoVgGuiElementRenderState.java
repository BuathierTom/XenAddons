package xenus.xenaddons.gui.odin.ui.rendering;

import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;

public record NanoVgGuiElementRenderState(
    int x1,
    int y1,
    int x2,
    int y2,
    float scale,
    ScreenRect scissorArea,
    ScreenRect bounds,
    Runnable renderContent
) implements SpecialGuiElementRenderState {
    public NanoVgGuiElementRenderState(int x1, int y1, int x2, int y2, float scale, ScreenRect scissorArea, Runnable renderContent) {
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
