package xenus.xenaddons.gui.clickgui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import xenus.xenaddons.features.Module;

public class ModuleButton {
    private final Module module;
    private final Panel parent;
    private final java.util.List<xenus.xenaddons.gui.clickgui.components.SettingComponent> settings = new java.util.ArrayList<>();
    public boolean extended = false;
    private int height;

    public ModuleButton(Module module, Panel parent) {
        this.module = module;
        this.parent = parent;
        for (xenus.xenaddons.settings.Setting<?> setting : module.settings) {
            settings.add(new xenus.xenaddons.gui.clickgui.components.SettingComponent(setting));
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, int x, int y, int width, int baseHeight, TextRenderer textRenderer) {
        this.height = baseHeight;
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + baseHeight;
        
        int color = module.isEnabled() ? 0xFF00AAFF : 0xFF2A2A2A; 
        if (hovered) color = module.isEnabled() ? 0xFF33BBFF : 0xFF3A3A3A;
        
        context.fill(x, y, x + width, y + baseHeight, color);
        context.drawTextWithShadow(textRenderer, module.name, x + 5, y + 5, 0xFFFFFFFF);
        
        if (extended && !settings.isEmpty()) {
            int currentY = y + baseHeight;
            for (xenus.xenaddons.gui.clickgui.components.SettingComponent comp : settings) {
                comp.render(context, mouseX, mouseY, x, currentY, width, 15, textRenderer);
                currentY += 15;
                this.height += 15;
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left Click - Toggle Module
             module.toggle();
             return true;
        } else if (button == 1) { // Right Click - Expand
             extended = !extended;
             return true;
        }
        return false;
    }
    
    public int getHeight() {
        return extended && !settings.isEmpty() ? (18 + settings.size() * 15) : 18;
    }

    public boolean mouseClickedSettings(double mouseX, double mouseY, int button, int x, int y) {
        if (extended && !settings.isEmpty()) {
            int currentY = y + 18;
            for (xenus.xenaddons.gui.clickgui.components.SettingComponent comp : settings) {
                if (mouseX >= x && mouseX < x + parent.WIDTH && mouseY >= currentY && mouseY < currentY + 15) {
                    if (comp.mouseClicked(mouseX, mouseY, button)) return true;
                }
                currentY += 15;
            }
        }
        return false;
    }
}
