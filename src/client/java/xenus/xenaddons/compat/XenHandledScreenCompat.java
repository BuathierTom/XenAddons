package xenus.xenaddons.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import xenus.xenaddons.XenAddons;
import xenus.xenaddons.features.impl.misc.ExperimentationTableSolverTracker;
import xenus.xenaddons.mixin.client.HandledScreenAccessor;

import java.util.Map;

public final class XenHandledScreenCompat {
    private static final Identifier BUTTONS_TEXTURE = XenIdentifierCompat.xen("textures/gui/buttons.png");
    private static final int STANDARD_INV_HEIGHT = 166;
    private static final int STANDARD_INV_WIDTH = 176;

    private XenHandledScreenCompat() {}

    public static XenAddons.CustomButtonData renderInvButtons(Screen screen, DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY) {
        if (!XenAddons.instance.enabled) return null;

        MinecraftClient client = MinecraftClient.getInstance();
        if (XenAddons.instance.hideInCreative && client.interactionManager != null && client.interactionManager.getCurrentGameMode().isCreative()) {
            return null;
        }

        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        int xSize = accessor.getBackgroundWidth();
        int ySize = accessor.getBackgroundHeight();
        int guiLeft = accessor.getXPosition();
        int guiTop = accessor.getYPosition();
        boolean isPlayerInventory = screen instanceof InventoryScreen;
        int containerOffset = ySize - STANDARD_INV_HEIGHT;
        XenAddons.CustomButtonData hovered = null;

        for (XenAddons.CustomButtonData button : XenAddons.instance.buttons) {
            int renderOffsetY = getRenderOffsetY(button, isPlayerInventory, containerOffset);
            if (renderOffsetY == Integer.MIN_VALUE) continue;

            int x = button.x + (button.anchorRight ? xSize : 0);
            int y = button.y + renderOffsetY + (button.anchorBottom ? ySize : 0);

            XenDrawContextCompat.drawTexture(context, BUTTONS_TEXTURE, x, y, button.backgroundIndex * 18.0f, 18.0f, 18, 18, 90, 36);

            Identifier customTexture = findCustomTexture(button);
            if (customTexture != null) {
                XenDrawContextCompat.drawTexture(context, customTexture, x + 1, y + 1, 0.0f, 0.0f, 16, 16, 16, 16);
            } else {
                ItemStack stack = button.getItemStack();
                if (!stack.isEmpty()) context.drawItem(stack, x + 1, y + 1);
                else XenDrawContextCompat.drawTextWithShadow(context, textRenderer, "?", x + 9 - textRenderer.getWidth("?") / 2, y + 5, 0xFFFFFFFF);
            }

            if (mouseX >= guiLeft + x && mouseX <= guiLeft + x + 18 && mouseY >= guiTop + y && mouseY <= guiTop + y + 18) {
                hovered = button;
            }
        }

        return hovered;
    }

    public static void drawInvTooltip(DrawContext context, TextRenderer textRenderer, XenAddons.CustomButtonData hoveredButton, int mouseX, int mouseY) {
        if (hoveredButton != null && XenAddons.instance.showTooltips) {
            context.drawTooltip(textRenderer, Text.literal(hoveredButton.command), mouseX, mouseY);
        }
    }

    public static boolean handleMouseClicked(Screen screen, Click click) {
        if (screen instanceof HandledScreen<?> handled && ExperimentationTableSolverTracker.onHandledScreenMouseClicked(handled, click.button())) {
            return true;
        }

        if (!XenAddons.instance.enabled) return false;

        MinecraftClient client = MinecraftClient.getInstance();
        if (XenAddons.instance.hideInCreative && client.interactionManager != null && client.interactionManager.getCurrentGameMode().isCreative()) {
            return false;
        }

        if (click.button() != 0 || !(screen instanceof HandledScreen<?>)) return false;

        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        int guiLeft = accessor.getXPosition();
        int guiTop = accessor.getYPosition();
        int xSize = accessor.getBackgroundWidth();
        int ySize = accessor.getBackgroundHeight();
        boolean isPlayerInventory = screen instanceof InventoryScreen;
        int containerOffset = ySize - STANDARD_INV_HEIGHT;

        for (XenAddons.CustomButtonData button : XenAddons.instance.buttons) {
            int renderOffsetY = getRenderOffsetY(button, isPlayerInventory, containerOffset);
            if (renderOffsetY == Integer.MIN_VALUE) continue;

            int buttonX = guiLeft + button.x + (button.anchorRight ? xSize : 0);
            int buttonY = guiTop + button.y + renderOffsetY + (button.anchorBottom ? ySize : 0);

            if (click.x() >= buttonX && click.x() <= buttonX + 18 && click.y() >= buttonY && click.y() <= buttonY + 18) {
                sendButtonCommand(client, button.command);
                return true;
            }
        }

        return false;
    }

    public static Slot getFocusedSlot(HandledScreen<?> screen) {
        if (screen == null) return null;
        return ((HandledScreenAccessor) screen).getFocusedSlot();
    }

    private static int getRenderOffsetY(XenAddons.CustomButtonData button, boolean isPlayerInventory, int containerOffset) {
        if (isPlayerInventory || button.anchorBottom) return 0;
        if (button.y < 80) {
            boolean isInsideX = button.x >= 0 && button.x <= STANDARD_INV_WIDTH;
            boolean isInsideY = button.y >= 0;
            if (!button.anchorRight && isInsideX && isInsideY) return Integer.MIN_VALUE;
            return 0;
        }
        return containerOffset;
    }

    private static Identifier findCustomTexture(XenAddons.CustomButtonData button) {
        if (button.itemId == null) return null;
        for (Map.Entry<String, Identifier> entry : XenAddons.CUSTOM_TEXTURES.entrySet()) {
            if (entry.getValue().toString().equals(button.itemId)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static void sendButtonCommand(MinecraftClient client, String command) {
        if (client.player == null) return;
        String cleanCommand = command;
        if (cleanCommand.startsWith("/")) cleanCommand = cleanCommand.substring(1);
        client.player.networkHandler.sendChatCommand(cleanCommand);
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
