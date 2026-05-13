package xenus.xenaddons.mixin.client;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xenus.xenaddons.compat.XenInteractionCompat;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManagerCooldownItems {

    @Inject(method = "interactItem", at = @At("HEAD"))
    private void xenaddons$trackItemClick(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (player == null || hand == null) return;
        ItemStack stack = player.getStackInHand(hand);
        XenInteractionCompat.onItemRightClick(stack);
    }

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void xenaddons$trackLeftClickEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (player == null) return;
        XenInteractionCompat.onItemLeftClick(player.getMainHandStack());
    }

    @Inject(method = "attackBlock", at = @At("HEAD"))
    private void xenaddons$trackLeftClickBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = net.minecraft.client.MinecraftClient.getInstance().player;
        if (player == null) return;
        XenInteractionCompat.onItemLeftClick(player.getMainHandStack());
    }
}
