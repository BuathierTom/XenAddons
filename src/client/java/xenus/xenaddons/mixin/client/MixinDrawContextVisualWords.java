package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xenus.xenaddons.compat.XenTextCompat;

import java.util.List;

@Mixin(GuiGraphicsExtractor.class)
public class MixinDrawContextVisualWords {
    @ModifyVariable(
        method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;II)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 1,
        require = 0
    )
    private Component xenaddons$drawTooltipSingle(Component text) {
        return XenTextCompat.replaceTooltip(text);
    }

    @ModifyVariable(
        method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IILnet/minecraft/resources/Identifier;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 1,
        require = 0
    )
    private Component xenaddons$drawTooltipSingleWithId(Component text) {
        return XenTextCompat.replaceTooltip(text);
    }

    @ModifyVariable(
        method = "setTooltipForNextFrame(Lnet/minecraft/network/chat/Component;II)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0,
        require = 0
    )
    private Component xenaddons$drawTooltipNoRenderer(Component text) {
        return XenTextCompat.replaceTooltip(text);
    }

    @ModifyVariable(
        method = "setComponentTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 1,
        require = 0
    )
    private List<Component> xenaddons$drawTooltipList(List<Component> lines) {
        return XenTextCompat.replaceTooltipList(lines);
    }

    @ModifyVariable(
        method = "setComponentTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/resources/Identifier;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 1,
        require = 0
    )
    private List<Component> xenaddons$drawTooltipListWithId(List<Component> lines) {
        return XenTextCompat.replaceTooltipList(lines);
    }

}
