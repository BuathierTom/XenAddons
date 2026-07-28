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
public record MusicImageState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2fc pose,
    float x,
    float y,
    float width,
    float height,
    int tintColor,
    float radius,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public MusicImageState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2fc pose,
        float x,
        float y,
        float width,
        float height,
        int tintColor,
        float radius,
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
            tintColor,
            radius,
            scissorArea,
            bounds(x, y, width, height, pose, scissorArea)
        );
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        MusicVertexConsumer musicConsumer = (MusicVertexConsumer) consumer;
        addVertex(consumer, musicConsumer, x, y, 0.0f, 0.0f);
        addVertex(consumer, musicConsumer, x, y + height, 0.0f, 1.0f);
        addVertex(consumer, musicConsumer, x + width, y + height, 1.0f, 1.0f);
        addVertex(consumer, musicConsumer, x + width, y, 1.0f, 0.0f);
    }

    private void addVertex(VertexConsumer consumer, MusicVertexConsumer musicConsumer, float vx, float vy, float u, float v) {
        consumer.addVertexWith2DPose(pose, vx, vy).setColor(tintColor).setUv(u, v);
        musicConsumer.setMusicCustomData(width, height, radius, 0.0f);
        musicConsumer.setMusicColor2(0);
        musicConsumer.setMusicShaderProperties(0.0f, 0.0f, 1.0f, 1.0f);
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
