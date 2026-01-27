package xenus.xenaddons.mixin

import xenus.xenaddons.api.events.EventBus
import xenus.xenaddons.api.events.RenderWorldEvent
import xenus.xenaddons.api.utils.Render3DImmediate
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.Camera
import net.minecraft.client.DeltaTracker
import org.joml.Matrix4f
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft

@Mixin(LevelRenderer::class)
class MixinLevelRenderer {
    @Inject(method = ["renderLevel"], at = [At("RETURN")])
    private fun onRenderLevel(
        deltaTracker: DeltaTracker,
        renderBlockOutline: Boolean,
        camera: Camera,
        gameRenderer: GameRenderer,
        lightTexture: LightTexture,
        matrix4f: Matrix4f,
        matrix4f2: Matrix4f,
        ci: CallbackInfo
    ) {
        val stack = PoseStack() // New empty stack?
        // In 1.21 we might need to initialize stack with camera transform or modelview?
        // But usually "RETURN" of renderLevel implies we are done.
        // If we want to render in world space, we need the camera transform.
        // "matrix4f" (projection) and "matrix4f2" (modelview) are passed.
        // We can ignore them if we use "render relative to camera" logic in Render3DImmediate.
        // Render3DImmediate.setup(stack, consumers)
        
        // Consumers: LevelRenderer has "renderBuffers.bufferSource()".
        // But we don't have access to it here easily without Shadow.
        // We can use Minecraft.getInstance().renderBuffers().bufferSource()
        
        val consumers = Minecraft.getInstance().renderBuffers().bufferSource()
        Render3DImmediate.setup(stack, consumers)
        
        val partialTicks = deltaTracker.getGameTimeDeltaPartialTick(true)
        EventBus.post(RenderWorldEvent(stack, partialTicks, consumers))
        
        // Flush? If we use bufferSource from Minecraft, we might need to flush if we want it drawn now?
        // Usually drawing happens at end of frame.
        consumers.endBatch() // Force draw? Safer?
    }
}
