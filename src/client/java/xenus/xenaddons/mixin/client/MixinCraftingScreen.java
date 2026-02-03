package xenus.xenaddons.mixin.client;

import xenus.xenaddons.XenAddons;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingScreen.class)
public abstract class MixinCraftingScreen extends Screen {

    protected MixinCraftingScreen(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (XenAddons.instance.hideRecipeBook) {
            for (Element element : this.children()) {
                if (element instanceof ClickableWidget widget) {
                    if (widget.getWidth() == 20 && widget.getHeight() == 18) {
                        widget.visible = false;
                        widget.active = false;
                    }
                }
            }
        }
    }
}
