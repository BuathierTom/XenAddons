package xenus.xenaddons.render.music.font;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;
import xenus.xenaddons.render.music.MusicRenderPipelines;
import xenus.xenaddons.render.music.MusicVertexConsumer;

@Environment(EnvType.CLIENT)
public record MusicMsdfTextState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    MusicMsdfFont font,
    String text,
    Matrix3x2fc pose,
    float x,
    float y,
    float size,
    int color,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public MusicMsdfTextState(
        MusicMsdfFont font,
        String text,
        Matrix3x2fc pose,
        float x,
        float y,
        float size,
        int color,
        @Nullable ScreenRectangle scissorArea
    ) {
        this(
            MusicRenderPipelines.MSDF_FONT,
            font.textureSetup(),
            font,
            text,
            pose,
            x,
            y,
            size,
            color,
            scissorArea,
            bounds(font, text, x, y, size, pose, scissorArea)
        );
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        font.draw(consumer, (MusicVertexConsumer) consumer, pose, text, x, y, size, color);
    }

    private static ScreenRectangle bounds(
        MusicMsdfFont font,
        String text,
        float x,
        float y,
        float size,
        Matrix3x2fc pose,
        @Nullable ScreenRectangle scissor
    ) {
        ScreenRectangle result = new ScreenRectangle(
            (int) x,
            (int) y,
            (int) Math.ceil(font.width(text, size)),
            (int) Math.ceil(font.height(size))
        ).transformMaxBounds(pose);
        return scissor != null ? scissor.intersection(result) : result;
    }
}
