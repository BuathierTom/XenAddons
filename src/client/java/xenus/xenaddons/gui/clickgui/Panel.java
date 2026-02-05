package xenus.xenaddons.gui.clickgui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.utils.RenderUtils;

import java.util.ArrayList;
import java.util.List;

public class Panel {
    public static final int WIDTH = 110;
    public static final int HEADER_HEIGHT = 20;

    private final Category category;
    private int x, y;
    private boolean dragging;
    private double dragX, dragY;
    private boolean open = true;

    private final List<ModuleButton> buttons = new ArrayList<>();

    public Panel(Category category, int x, int y) {
        this.category = category;
        this.x = x;
        this.y = y;

        List<Module> modules = ModuleManager.getModulesByCategory(category);
        for (Module module : modules) {
            buttons.add(new ModuleButton(module, this));
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta, TextRenderer textRenderer) {
        // Draw Header - Solid Black
        context.fill(x, y, x + WIDTH, y + HEADER_HEIGHT, 0xFF000000);
        
        // Draw Header Title - Centered
        RenderUtils.drawCenteredText(context, textRenderer, category.name, x + WIDTH / 2, y + (HEADER_HEIGHT - 9) / 2 + 1, 0xFFFFFFFF, true);

        if (open) {
            // Calculate total content height first
            int contentHeight = 0;
            for (ModuleButton button : buttons) {
                contentHeight += button.getHeight();
            }
            
            // Draw Panel Background (Translucent Black) behind all buttons
            context.fill(x, y + HEADER_HEIGHT, x + WIDTH, y + HEADER_HEIGHT + contentHeight, 0xAA000000);
            
            // Render Buttons
            int currentY = y + HEADER_HEIGHT;
            for (ModuleButton button : buttons) {
                // Pass baseHeight = 16 (smaller, cleaner)
                button.render(context, mouseX, mouseY, x, currentY, WIDTH, 16, textRenderer);
                currentY += button.getHeight();
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= x && mouseX <= x + WIDTH && mouseY >= y && mouseY <= y + HEADER_HEIGHT) {
            if (button == 0) { // Left Click - Drag
                dragging = true;
                dragX = mouseX - x;
                dragY = mouseY - y;
                return true;
            } else if (button == 1) { // Right Click - Toggle Open
                open = !open;
                return true;
            }
        }

        if (open) {
             int currentY = y + HEADER_HEIGHT;
             for (ModuleButton btn : buttons) {
                 int btnHeight = btn.getHeight();
                 // Check if click is within button bounds (and its settings)
                 if (mouseX >= x && mouseX <= x + WIDTH && mouseY >= currentY && mouseY < currentY + btnHeight) {
                     // Check settings click first
                     if (btn.mouseClickedSettings(mouseX, mouseY, button, x, currentY)) return true;
                     // Then check button click (only if in the header part area)
                     if (mouseY < currentY + 18) {
                         if (btn.mouseClicked(mouseX, mouseY, button)) return true;
                     }
                 }
                 currentY += btnHeight;
             }
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            x = (int) (mouseX - dragX);
            y = (int) (mouseY - dragY);
            return true;
        }
        return false;
    }
}
