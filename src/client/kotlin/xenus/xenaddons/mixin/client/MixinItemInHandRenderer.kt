package xenus.xenaddons.mixin.client

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.ItemInHandRenderer
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.ModifyVariable
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import xenus.xenaddons.config.XenAddonsConfig

@Mixin(ItemInHandRenderer::class)
class MixinItemInHandRenderer {

    @Shadow private var mainHandHeight: Float = 0f

    @Shadow private var oMainHandHeight: Float = 0f

    @Shadow private var offHandHeight: Float = 0f

    @Shadow private var oOffHandHeight: Float = 0f

    @ModifyVariable(method = ["renderArmWithItem"], at = At("HEAD"), argsOnly = true, ordinal = 2)
    private fun modifySwingProgress(swingProgress: Float): Float {
        if (XenAddonsConfig.noSwing) {
            return 0.0f
        }
        return swingProgress
    }

    @Inject(method = ["tick"], at = [At("TAIL")])
    fun onTick(ci: CallbackInfo) {
        if (XenAddonsConfig.noEquipReset) {
            this.mainHandHeight = 1.0f
            this.oMainHandHeight = 1.0f
            this.offHandHeight = 1.0f
            this.oOffHandHeight = 1.0f
        }
    }

    @Inject(method = ["renderItem"], at = [At("HEAD")])
    fun renderItem(
            entity: LivingEntity,
            stack: ItemStack,
            displayContext: ItemDisplayContext,
            poseStack: PoseStack,
            buffer: SubmitNodeCollector,
            combinedLight: Int,
            ci: CallbackInfo
    ) {
        if (displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ||
                        displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
        ) {

            if (XenAddonsConfig.scaleSwing) {
                // Position
                poseStack.translate(
                        XenAddonsConfig.itemScaleX,
                        XenAddonsConfig.itemScaleY,
                        XenAddonsConfig.itemScaleZ
                )

                // Scale
                var scaleBase = 1.0f + XenAddonsConfig.itemScaleSize
                if (scaleBase < 0.01f) scaleBase = 0.01f
                poseStack.scale(scaleBase, scaleBase, scaleBase)

                // Rotation
                poseStack.mulPose(Axis.XP.rotationDegrees(XenAddonsConfig.itemScalePitch))
                poseStack.mulPose(Axis.YP.rotationDegrees(XenAddonsConfig.itemScaleYaw))
                poseStack.mulPose(Axis.ZP.rotationDegrees(XenAddonsConfig.itemScaleRoll))
            }
        }
    }
}
