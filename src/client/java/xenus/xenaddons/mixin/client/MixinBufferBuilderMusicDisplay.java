package xenus.xenaddons.mixin.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.util.ARGB;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xenus.xenaddons.render.music.MusicRenderPipelines;
import xenus.xenaddons.render.music.MusicVertexConsumer;

@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilderMusicDisplay implements VertexConsumer, MusicVertexConsumer {
    @Shadow
    protected abstract long beginElement(VertexFormatElement element);

    @Override
    public VertexConsumer setMusicCustomData(float x, float y, float z, float w) {
        long pointer = beginElement(MusicRenderPipelines.CUSTOM_DATA);
        if (pointer != -1L) {
            MemoryUtil.memPutFloat(pointer, x);
            MemoryUtil.memPutFloat(pointer + 4L, y);
            MemoryUtil.memPutFloat(pointer + 8L, z);
            MemoryUtil.memPutFloat(pointer + 12L, w);
        }
        return this;
    }

    @Override
    public VertexConsumer setMusicColor2(int color) {
        long pointer = beginElement(MusicRenderPipelines.COLOR_2);
        if (pointer != -1L) {
            MemoryUtil.memPutInt(pointer, ARGB.toABGR(color));
        }
        return this;
    }

    @Override
    public VertexConsumer setMusicShaderProperties(float x, float y, float z, float w) {
        long pointer = beginElement(MusicRenderPipelines.SHADER_PROPERTIES);
        if (pointer != -1L) {
            MemoryUtil.memPutFloat(pointer, x);
            MemoryUtil.memPutFloat(pointer + 4L, y);
            MemoryUtil.memPutFloat(pointer + 8L, z);
            MemoryUtil.memPutFloat(pointer + 12L, w);
        }
        return this;
    }
}
