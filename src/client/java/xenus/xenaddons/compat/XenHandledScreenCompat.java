package xenus.xenaddons.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xenus.xenaddons.XenAddons;
import xenus.xenaddons.features.impl.misc.ExperimentationTableSolverTracker;
import xenus.xenaddons.features.impl.misc.VisitorGardenModule;
import xenus.xenaddons.mixin.client.HandledScreenAccessor;

import java.util.ArrayList;
import java.util.List;

public final class XenHandledScreenCompat {
    private static final Identifier BUTTONS_TEXTURE = XenIdentifierCompat.xen("textures/gui/buttons.png");
    private static final int STANDARD_INV_HEIGHT = 166;
    private static final int STANDARD_INV_WIDTH = 176;

    private XenHandledScreenCompat() {}

    public static XenAddons.CustomButtonData renderInvButtons(Screen screen, GuiGraphicsExtractor context, Font textRenderer, int mouseX, int mouseY) {
        return renderInvButtons(screen, context, textRenderer, mouseX, mouseY, false);
    }

    public static XenAddons.CustomButtonData renderInvButtonsInContainerSpace(Screen screen, GuiGraphicsExtractor context, Font textRenderer, int mouseX, int mouseY) {
        return renderInvButtons(screen, context, textRenderer, mouseX, mouseY, true);
    }

    private static XenAddons.CustomButtonData renderInvButtons(Screen screen, GuiGraphicsExtractor context, Font textRenderer, int mouseX, int mouseY, boolean containerSpace) {
        if (!XenAddons.instance.enabled) return null;

        Minecraft client = Minecraft.getInstance();
        if (XenAddons.instance.hideInCreative && client.gameMode != null && client.gameMode.getPlayerMode().isCreative()) {
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
        List<ButtonRenderEntry> entries = new ArrayList<>();

        context.nextStratum();

        for (XenAddons.CustomButtonData button : XenAddons.instance.buttons) {
            int renderOffsetY = getRenderOffsetY(button, isPlayerInventory, containerOffset);
            if (renderOffsetY == Integer.MIN_VALUE) continue;

            int localX = button.x + (button.anchorRight ? xSize : 0);
            int localY = button.y + renderOffsetY + (button.anchorBottom ? ySize : 0);
            int x = containerSpace ? localX : guiLeft + localX;
            int y = containerSpace ? localY : guiTop + localY;
            int hoverX = guiLeft + localX;
            int hoverY = guiTop + localY;

            XenDrawContextCompat.drawTexture(context, BUTTONS_TEXTURE, x, y, button.backgroundIndex * 18.0f, 18.0f, 18, 18, 90, 36);
            entries.add(new ButtonRenderEntry(button, x, y));

            if (mouseX >= hoverX && mouseX <= hoverX + 18 && mouseY >= hoverY && mouseY <= hoverY + 18) {
                hovered = button;
            }
        }

        if (!entries.isEmpty()) context.guiRenderState.up();

        for (ButtonRenderEntry entry : entries) {
            XenAddons.CustomButtonData button = entry.button;
            int x = entry.x;
            int y = entry.y;
            Identifier customTexture = XenAddons.findCustomTexture(button.itemId);
            if (customTexture != null) {
                XenDrawContextCompat.drawTexture(context, customTexture, x + 1, y + 1, 0.0f, 0.0f, 16, 16, 16, 16);
            } else {
                ItemStack stack = button.getItemStack();
                if (!stack.isEmpty()) XenDrawContextCompat.item(context, stack, x + 1, y + 1);
                else XenDrawContextCompat.text(context, textRenderer, "?", x + 9 - textRenderer.width("?") / 2, y + 5, 0xFFFFFFFF);
            }
        }

        return hovered;
    }

    public static void drawInvTooltip(GuiGraphicsExtractor context, Font textRenderer, XenAddons.CustomButtonData hoveredButton, int mouseX, int mouseY) {
        if (hoveredButton != null && XenAddons.instance.showTooltips) {
            context.setTooltipForNextFrame(textRenderer, Component.literal(hoveredButton.command), mouseX, mouseY);
        }
    }

    public static boolean handleMouseClicked(Screen screen, MouseButtonEvent click) {
        if (screen instanceof AbstractContainerScreen<?> handled && ExperimentationTableSolverTracker.onHandledScreenMouseClicked(handled, click.button())) {
            return true;
        }

        if (screen instanceof AbstractContainerScreen<?> visitor && VisitorGardenModule.onMouseClicked(visitor, click.button())) {
            return true;
        }

        if (!XenAddons.instance.enabled) return false;

        Minecraft client = Minecraft.getInstance();
        if (XenAddons.instance.hideInCreative && client.gameMode != null && client.gameMode.getPlayerMode().isCreative()) {
            return false;
        }

        if (click.button() != 0 || !(screen instanceof AbstractContainerScreen<?>)) return false;

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

    public static Slot getFocusedSlot(AbstractContainerScreen<?> screen) {
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

    private static void sendButtonCommand(Minecraft client, String command) {
        if (client.player == null) return;
        String cleanCommand = command;
        if (cleanCommand.startsWith("/")) cleanCommand = cleanCommand.substring(1);
        client.player.connection.sendCommand(cleanCommand);
        XenSoundCompat.playButtonClick(client);
    }

    private static final class ButtonRenderEntry {
        private final XenAddons.CustomButtonData button;
        private final int x;
        private final int y;

        private ButtonRenderEntry(XenAddons.CustomButtonData button, int x, int y) {
            this.button = button;
            this.x = x;
            this.y = y;
        }
    }
}
