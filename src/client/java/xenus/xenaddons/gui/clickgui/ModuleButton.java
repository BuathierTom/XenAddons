package xenus.xenaddons.gui.clickgui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import xenus.xenaddons.features.Module;

public class ModuleButton {
    private final Module module;
    private final Panel parent;

    public ModuleButton(Module module, Panel parent) {
        this.module = module;
        this.parent = parent;
    }

    public void render(DrawContext context, int mouseX, int mouseY, int x, int y, int width, int height, TextRenderer textRenderer) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        
        int color = module.isEnabled() ? 0xFF00AAFF : 0xFF2A2A2A; // Blue if enabled, dark gray if not
        if (hovered) {
             color = module.isEnabled() ? 0xFF33BBFF : 0xFF3A3A3A; // Lighter on hover
        }
        
        context.fill(x, y, x + width, y + height, color);
        context.drawTextWithShadow(textRenderer, module.name, x + 5, y + 5, 0xFFFFFFFF);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left Click
            module.toggle();
            return true;
        }
        return false;
    }
}
