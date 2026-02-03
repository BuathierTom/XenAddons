package xenus.xenaddons.mixin.client;

import xenus.xenaddons.XenAddons;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.RenderLayer;
import xenus.xenaddons.utils.RenderUtils;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.client.gui.Click;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen extends Screen {

    private static final Identifier BUTTONS_TEXTURE = Identifier.of("xenaddons", "textures/gui/buttons.png");
    private static final int STANDARD_INV_HEIGHT = 166;
    private static final int STANDARD_INV_WIDTH = 176;

    protected MixinHandledScreen(Text title) { super(title); }

    @Inject(method = "renderMain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawForeground(Lnet/minecraft/client/gui/DrawContext;II)V", shift = At.Shift.BEFORE))
    private void renderInvButtons(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {

        if (!XenAddons.instance.enabled) return;

        if (XenAddons.instance.hideInCreative && this.client != null && this.client.interactionManager != null && this.client.interactionManager.getCurrentGameMode().isCreative()) {
            return;
        }

        HandledScreenAccessor accessor = (HandledScreenAccessor) this;
        int xSize = accessor.getBackgroundWidth();
        int ySize = accessor.getBackgroundHeight();
        int guiLeft = accessor.getXPosition();
        int guiTop = accessor.getYPosition();

        boolean isPlayerInventory = ((Object)this instanceof InventoryScreen);
        int containerOffset = ySize - STANDARD_INV_HEIGHT;

        XenAddons.CustomButtonData hoveredBtn = null;

        for (XenAddons.CustomButtonData btn : XenAddons.instance.buttons) {
            int renderOffsetY = 0;

            if (!isPlayerInventory) {
                if (!btn.anchorBottom) {
                    // --- Logic for Top Buttons (Y < 80) ---
                    if (btn.y < 80) {
                        // Check if it's an "Internal" button (Crafting/Armor/Player Model)

                        boolean isInsideX = (btn.x >= 0 && btn.x <= STANDARD_INV_WIDTH);
                        boolean isInsideY = (btn.y >= 0);

                        if (!btn.anchorRight && isInsideX && isInsideY) {
                            continue; // hide
                        }
                        // If it's above (y < 0) or to the side, it stays visible at renderOffsetY = 0 (pinned to top)
                    }
                    // --- Logic for Inventory Buttons (Y >= 80) ---
                    else {
                        // Shift down to align with the player inventory in this container
                        renderOffsetY = containerOffset;
                    }
                }
            }

            int x = btn.x;
            int y = btn.y + renderOffsetY;

            if (btn.anchorRight) x += xSize;
            if (btn.anchorBottom) y += ySize;

            context.drawTexture(RenderPipelines.GUI_TEXTURED, BUTTONS_TEXTURE, x, y,
                    (float)(btn.backgroundIndex * 18), 18.0f, 18, 18, 90, 36);

            Identifier customTex = null;
            if (btn.itemId != null) {
                for(Map.Entry<String, Identifier> entry : XenAddons.CUSTOM_TEXTURES.entrySet()) {
                    if (entry.getValue().toString().equals(btn.itemId)) {
                        customTex = entry.getValue();
                        break;
                    }
                }
            }

            if (customTex != null) {
                context.drawTexture(RenderPipelines.GUI_TEXTURED, customTex, x + 1, y + 1, 0.0f, 0.0f, 16, 16, 16, 16);
            } else {
                ItemStack stack = btn.getItemStack();
                if (!stack.isEmpty()) context.drawItem(stack, x + 1, y + 1);
                else context.drawTextWithShadow(textRenderer, "?", x + 9 - textRenderer.getWidth("?") / 2, y + 5, 0xFFFFFFFF);
            }

            if (mouseX >= guiLeft + x && mouseX <= guiLeft + x + 18 && mouseY >= guiTop + y && mouseY <= guiTop + y + 18) {
                hoveredBtn = btn;
            }
        }

        if (hoveredBtn != null) {
            int renderOffsetY = 0;
            if (!isPlayerInventory && !hoveredBtn.anchorBottom && hoveredBtn.y >= 80) {
                renderOffsetY = containerOffset;
            }

            int finalX = hoveredBtn.x + (hoveredBtn.anchorRight ? xSize : 0);
            int finalY = hoveredBtn.y + renderOffsetY + (hoveredBtn.anchorBottom ? ySize : 0);

            RenderUtils.drawBorder(context, finalX, finalY, 18, 18, 0xFFFFFFFF);

            if (XenAddons.instance.showTooltips) {
                context.drawTooltip(textRenderer, Text.literal(hoveredBtn.command), mouseX, mouseY);
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (!XenAddons.instance.enabled) return;

        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (XenAddons.instance.hideInCreative && this.client != null && this.client.interactionManager != null && this.client.interactionManager.getCurrentGameMode().isCreative()) {
            return;
        }

        if (button == 0 && (Object)this instanceof HandledScreen) {
            HandledScreenAccessor accessor = (HandledScreenAccessor) this;
            int guiLeft = accessor.getXPosition();
            int guiTop = accessor.getYPosition();
            int xSize = accessor.getBackgroundWidth();
            int ySize = accessor.getBackgroundHeight();

            boolean isPlayerInventory = ((Object)this instanceof InventoryScreen);
            int containerOffset = ySize - STANDARD_INV_HEIGHT;

            for (XenAddons.CustomButtonData btn : XenAddons.instance.buttons) {
                int renderOffsetY = 0;

                if (!isPlayerInventory) {
                    if (!btn.anchorBottom) {
                        if (btn.y < 80) {
                            boolean isInsideX = (btn.x >= 0 && btn.x <= STANDARD_INV_WIDTH);
                            boolean isInsideY = (btn.y >= 0);

                            if (!btn.anchorRight && isInsideX && isInsideY) {
                                continue;
                            }
                        } else {
                            renderOffsetY = containerOffset;
                        }
                    }
                }

                int btnX = guiLeft + btn.x;
                int btnY = guiTop + btn.y + renderOffsetY;

                if (btn.anchorRight) btnX += xSize;
                if (btn.anchorBottom) btnY += ySize;

                if (mouseX >= btnX && mouseX <= btnX + 18 && mouseY >= btnY && mouseY <= btnY + 18) {
                    if (this.client != null && this.client.player != null) {
                        String cmd = btn.command;
                        if (cmd.startsWith("/")) cmd = cmd.substring(1);
                        this.client.player.networkHandler.sendChatCommand(cmd);
                        this.client.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    }

                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }
}