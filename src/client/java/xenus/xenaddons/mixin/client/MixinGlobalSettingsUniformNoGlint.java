package xenus.xenaddons.mixin.client;

import net.minecraft.client.renderer.GlobalSettingsUniform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xenus.xenaddons.features.impl.misc.NoGlintModule;

@Mixin(GlobalSettingsUniform.class)
public class MixinGlobalSettingsUniformNoGlint {

    // Argument 3 of update() is the glint strength, uploaded to the globals UBO as GlintAlpha.
    // glint.fsh multiplies the glint colour by it and the pass blends additively, so 0 makes
    // every glint invisible at once - GUI items, held items, dropped items, worn armor and
    // shields all go through the single GLINT pipeline. Vanilla's own "Glint Strength" option
    // feeds the same value, so this leaves the user's video settings untouched.
    @ModifyVariable(method = "update", at = @At("HEAD"), argsOnly = true, index = 3)
    private double xenaddons$removeGlint(double glintStrength) {
        return NoGlintModule.isActive() ? 0.0 : glintStrength;
    }
}
