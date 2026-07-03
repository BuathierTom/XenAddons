package xenus.xenaddons.mixin.client;

import xenus.xenaddons.XenAddons;
import xenus.xenaddons.features.impl.misc.Cursor189Module;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class MixinInventoryScreen extends Screen {

    protected MixinInventoryScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        Cursor189Module.applyRememberedOnInventoryInit(this.minecraft);

        if (XenAddons.instance.hideRecipeBook) {
            for (GuiEventListener element : this.children()) {
                if (element instanceof AbstractWidget widget) {
                    // Standard recipe button is 20x18. 
                    // We check dimensions to find it among other potential buttons.
                    if (widget.getWidth() == 20 && widget.getHeight() == 18) {
                        widget.visible = false;
                        widget.active = false;
                    }
                }
            }
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onExtractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Cursor189Module.captureCurrentCursorInInventory(this.minecraft);
    }
}
