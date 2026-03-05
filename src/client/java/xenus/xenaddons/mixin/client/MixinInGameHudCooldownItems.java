package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xenus.xenaddons.features.impl.misc.CooldownItemsTracker;

@Mixin(InGameHud.class)
public class MixinInGameHudCooldownItems {

    @Inject(
        method = "renderHotbarItem",
        at = @At("TAIL"),
        require = 0
    )
    private void xenaddons$renderCooldownOnHotbar(DrawContext context, int x, int y, RenderTickCounter tickCounter, PlayerEntity player, ItemStack stack, int seed, CallbackInfo ci) {
        CooldownItemsTracker.renderBackground(context, stack, x, y, false);
        var textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        for (xenus.xenaddons.features.impl.misc.CooldownItemsTracker.OverlayText overlay : CooldownItemsTracker.getOverlayTexts(stack, false)) {
            int ox = x + (overlay.alternativePosition() ? -8 : 0);
            int oy = y + (overlay.alternativePosition() ? -10 : 0);
            int w = textRenderer.getWidth(overlay.text());
            int drawX = ox + 19 - 2 - w;
            int drawY = oy + 9;
            context.drawTextWithShadow(textRenderer, overlay.text(), drawX, drawY, 0xFF000000 | overlay.colorRgb());
        }
    }
}
