package xenus.xenaddons.render.music;

import com.mojang.blaze3d.vertex.VertexConsumer;

public interface MusicVertexConsumer {
    VertexConsumer setMusicCustomData(float x, float y, float z, float w);

    VertexConsumer setMusicColor2(int color);

    VertexConsumer setMusicShaderProperties(float x, float y, float z, float w);
}
