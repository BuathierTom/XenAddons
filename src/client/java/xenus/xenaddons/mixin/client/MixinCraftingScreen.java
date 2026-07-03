package xenus.xenaddons.mixin.client;

import xenus.xenaddons.XenAddons;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingScreen.class)
public abstract class MixinCraftingScreen extends Screen {

    protected MixinCraftingScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (XenAddons.instance.hideRecipeBook) {
            for (GuiEventListener element : this.children()) {
                if (element instanceof AbstractWidget widget) {
                    if (widget.getWidth() == 20 && widget.getHeight() == 18) {
                        widget.visible = false;
                        widget.active = false;
                    }
                }
            }
        }
    }
}
