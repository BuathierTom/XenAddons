package xenus.xenaddons.render.music.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;
import xenus.xenaddons.render.music.MusicVertexConsumer;

@Environment(EnvType.CLIENT)
public record MusicLiquidGlassState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2fc pose,
    float x,
    float y,
    float width,
    float height,
    int color,
    float radius,
    float blurStrength,
    float bloom,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public MusicLiquidGlassState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2fc pose,
        float x,
        float y,
        float width,
        float height,
        int color,
        float radius,
        float blurStrength,
        float bloom,
        @Nullable ScreenRectangle scissorArea
    ) {
        this(
            pipeline,
            textureSetup,
            pose,
            x,
            y,
            width,
            height,
            color,
            radius,
            blurStrength,
            bloom,
            scissorArea,
            bounds(x, y, width, height, pose, scissorArea)
        );
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        MusicVertexConsumer musicConsumer = (MusicVertexConsumer) consumer;
        float x0 = x - bloom;
        float y0 = y - bloom;
        float x1 = x + width + bloom;
        float y1 = y + height + bloom;
        addVertex(consumer, musicConsumer, x0, y0, 0.0f, 0.0f);
        addVertex(consumer, musicConsumer, x0, y1, 0.0f, 1.0f);
        addVertex(consumer, musicConsumer, x1, y1, 1.0f, 1.0f);
        addVertex(consumer, musicConsumer, x1, y0, 1.0f, 0.0f);
    }

    private void addVertex(VertexConsumer consumer, MusicVertexConsumer musicConsumer, float vx, float vy, float u, float v) {
        consumer.addVertexWith2DPose(pose, vx, vy).setColor(color).setUv(u, v);
        musicConsumer.setMusicCustomData(width, height, radius, 0.0f);
        musicConsumer.setMusicColor2(color);
        musicConsumer.setMusicShaderProperties(blurStrength, bloom, 0.0f, 0.0f);
    }

    private static ScreenRectangle bounds(
        float x,
        float y,
        float width,
        float height,
        Matrix3x2fc pose,
        @Nullable ScreenRectangle scissor
    ) {
        ScreenRectangle result = new ScreenRectangle((int) x, (int) y, (int) width, (int) height)
            .transformMaxBounds(pose);
        return scissor != null ? scissor.intersection(result) : result;
    }
}
