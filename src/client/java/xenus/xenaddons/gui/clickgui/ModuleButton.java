package xenus.xenaddons.gui.clickgui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.utils.RenderUtils;

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
        
        // Colors
        // Blue requested: 0xFF3D5AFE (approx material blue) or stick to current 0xFF00AAFF
        // Translucent background provided by Panel. only fill active or hovered.
        
        if (module.isEnabled()) {
             context.fill(x, y, x + width, y + baseHeight, 0xFF00AAFF); // Active Blue
        } else {
            // Disabled: Do nothing (transparent), show panel bg
            // Hover effect slightly lighter
            if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + baseHeight) {
                context.fill(x, y, x + width, y + baseHeight, 0x20FFFFFF);
            }
        }
        
        // Center Text
        RenderUtils.drawCenteredText(context, textRenderer, module.name, x + width / 2, y + (baseHeight - 9) / 2 + 1, 0xFFFFFFFF, true);
        
        if (extended && !settings.isEmpty()) {
            int currentY = y + baseHeight;
            // Draw background for settings to distinguish them? Or just line?
            // Usually simpler is better.
            
            for (xenus.xenaddons.gui.clickgui.components.SettingComponent comp : settings) {
                if (comp.setting.isVisible()) {
                    comp.render(context, mouseX, mouseY, x, currentY, width, 15, textRenderer);
                    currentY += 15;
                    this.height += 15;
                }
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
        int settingsHeight = 0;
        if (extended) {
            for (xenus.xenaddons.gui.clickgui.components.SettingComponent comp : settings) {
                if (comp.setting.isVisible()) {
                    settingsHeight += 15;
                }
            }
        }
        return 16 + settingsHeight;
    }

    public boolean mouseClickedSettings(double mouseX, double mouseY, int button, int x, int y) {
        if (extended && !settings.isEmpty()) {
            int currentY = y + 16;
            for (xenus.xenaddons.gui.clickgui.components.SettingComponent comp : settings) {
                if (comp.setting.isVisible()) {
                    if (mouseX >= x && mouseX < x + parent.WIDTH && mouseY >= currentY && mouseY < currentY + 15) {
                        if (comp.mouseClicked(mouseX, mouseY, button, x, currentY, parent.WIDTH)) return true;
                    }
                    currentY += 15;
                }
            }
        }
        return false;
    }
}
