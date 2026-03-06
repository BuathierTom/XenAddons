package xenus.xenaddons.mixin.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xenus.xenaddons.config.GoatedPlayersManager;

import java.util.ArrayList;
import java.util.List;

@Mixin(DrawContext.class)
public class MixinDrawContextVisualWords {
    @ModifyVariable(
        method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;II)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 1,
        require = 0
    )
    private Text xenaddons$drawTooltipSingle(Text text) {
        return GoatedPlayersManager.applyVisualWordsForTooltip(text);
    }

    @ModifyVariable(
        method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IILnet/minecraft/util/Identifier;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 1,
        require = 0
    )
    private Text xenaddons$drawTooltipSingleWithId(Text text) {
        return GoatedPlayersManager.applyVisualWordsForTooltip(text);
    }

    @ModifyVariable(
        method = "drawTooltip(Lnet/minecraft/text/Text;II)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0,
        require = 0
    )
    private Text xenaddons$drawTooltipNoRenderer(Text text) {
        return GoatedPlayersManager.applyVisualWordsForTooltip(text);
    }

    @ModifyVariable(
        method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;II)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 1,
        require = 0
    )
    private List<Text> xenaddons$drawTooltipList(List<Text> lines) {
        if (lines == null || lines.isEmpty()) return lines;
        List<Text> replaced = new ArrayList<>(lines.size());
        for (Text line : lines) {
            replaced.add(GoatedPlayersManager.applyVisualWordsForTooltip(line));
        }
        return replaced;
    }

    @ModifyVariable(
        method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/util/Identifier;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 1,
        require = 0
    )
    private List<Text> xenaddons$drawTooltipListWithId(List<Text> lines) {
        if (lines == null || lines.isEmpty()) return lines;
        List<Text> replaced = new ArrayList<>(lines.size());
        for (Text line : lines) {
            replaced.add(GoatedPlayersManager.applyVisualWordsForTooltip(line));
        }
        return replaced;
    }

}
