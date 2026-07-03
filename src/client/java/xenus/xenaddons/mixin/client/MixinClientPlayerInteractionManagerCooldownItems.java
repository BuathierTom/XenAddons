package xenus.xenaddons.mixin.client;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xenus.xenaddons.compat.XenInteractionCompat;

@Mixin(MultiPlayerGameMode.class)
public class MixinClientPlayerInteractionManagerCooldownItems {

    @Inject(method = "useItem", at = @At("HEAD"))
    private void xenaddons$trackItemClick(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (player == null || hand == null) return;
        ItemStack stack = player.getItemInHand(hand);
        XenInteractionCompat.onItemRightClick(stack);
    }

    @Inject(method = "attack", at = @At("HEAD"))
    private void xenaddons$trackLeftClickEntity(Player player, Entity target, CallbackInfo ci) {
        if (player == null) return;
        XenInteractionCompat.onItemLeftClick(player.getMainHandItem());
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void xenaddons$trackLeftClickBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return;
        XenInteractionCompat.onItemLeftClick(player.getMainHandItem());
    }
}
