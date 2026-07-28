package xenus.xenaddons.mixin.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import xenus.xenaddons.render.music.MusicVertexConsumer;

@Mixin(VertexConsumer.class)
public interface MixinVertexConsumerMusicDisplay extends MusicVertexConsumer {
    @Override
    default VertexConsumer setMusicCustomData(float x, float y, float z, float w) {
        return (VertexConsumer) this;
    }

    @Override
    default VertexConsumer setMusicColor2(int color) {
        return (VertexConsumer) this;
    }

    @Override
    default VertexConsumer setMusicShaderProperties(float x, float y, float z, float w) {
        return (VertexConsumer) this;
    }
}
