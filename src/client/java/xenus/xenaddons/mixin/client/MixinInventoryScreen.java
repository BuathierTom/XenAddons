package xenus.xenaddons.mixin.client;

import xenus.xenaddons.XenAddons;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class MixinInventoryScreen extends Screen {

    protected MixinInventoryScreen(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (XenAddons.instance.hideRecipeBook) {
            for (Element element : this.children()) {
                if (element instanceof ClickableWidget widget) {
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
}
