package xenus.xenaddons.render.nvg;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2f;

public record XenNvgRenderState(
    int x,
    int y,
    int width,
    int height,
    float scale,
    Matrix3x2f poseMatrix,
    ScreenRectangle scissorArea,
    ScreenRectangle bounds,
    Runnable renderContent
) implements PictureInPictureRenderState {
    public XenNvgRenderState(int x, int y, int width, int height, float scale, Matrix3x2f poseMatrix, ScreenRectangle scissorArea, Runnable renderContent) {
        this(
            x,
            y,
            width,
            height,
            scale,
            poseMatrix,
            scissorArea,
            createBounds(x, y, x + width, y + height, poseMatrix, scissorArea),
            renderContent
        );
    }

    @Override
    public int x0() {
        return x;
    }

    @Override
    public int y0() {
        return y;
    }

    @Override
    public int x1() {
        return x + width;
    }

    @Override
    public int y1() {
        return y + height;
    }

    private static ScreenRectangle createBounds(int x0, int y0, int x1, int y1, Matrix3x2f poseMatrix, ScreenRectangle scissorArea) {
        ScreenRectangle screenRect = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(poseMatrix);
        return scissorArea != null ? scissorArea.intersection(screenRect) : screenRect;
    }
}
