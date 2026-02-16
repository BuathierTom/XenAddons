package xenus.xenaddons.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xenus.xenaddons.config.GoatedPlayersManager;

import java.util.ArrayList;
import java.util.List;

@Mixin(Screen.class)
public class MixinScreenTooltipVisualWords {

    @Inject(method = "getTooltipFromItem", at = @At("RETURN"), cancellable = true)
    private static void xenaddons$replaceItemTooltipText(MinecraftClient client, ItemStack stack, CallbackInfoReturnable<List<Text>> cir) {
        List<Text> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) return;

        List<Text> replaced = new ArrayList<>(original.size());
        for (Text line : original) {
            replaced.add(GoatedPlayersManager.applyVisualWords(line));
        }
        cir.setReturnValue(replaced);
    }
}
