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

    public ModuleButton(Module module, Panel parent) {
        this.module = module;
        this.parent = parent;
        for (xenus.xenaddons.settings.Setting<?> setting : module.settings) {
            settings.add(new xenus.xenaddons.gui.clickgui.components.SettingComponent(setting));
        }
    }

    public Module getModule() {
        return module;
    }

    public void render(DrawContext context, int mouseX, int mouseY, int x, int y, int width, int baseHeight, TextRenderer textRenderer) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + baseHeight;
        
        int themeColor = xenus.xenaddons.features.ModuleManager.colors.getColor();
        if (module.isEnabled()) {
             context.fill(x, y, x + width, y + baseHeight, themeColor);
             RenderUtils.drawBorder(context, x, y, width, baseHeight, 0x66FFFFFF); 
        } else {
            if (hovered) {
                context.fill(x, y, x + width, y + baseHeight, 0x22FFFFFF);
            }
        }
        
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x + 5, y + (baseHeight - 9 * 0.8f) / 2f + 0.5f);
        context.getMatrices().scale(0.8f, 0.8f);
        RenderUtils.drawText(context, textRenderer, module.name, 0, 0, 0xFFFFFFFF, false);
        context.getMatrices().popMatrix();
        
        if (hovered) {
            ClickGUI.setHoveredModule(module, x + width + 5, y);
        }

        if (extended && !settings.isEmpty()) {
            int currentY = y + baseHeight;
            for (xenus.xenaddons.gui.clickgui.components.SettingComponent comp : settings) {
                if (comp.setting.isVisible()) {
                    int compHeight = comp.getHeight();
                    comp.render(context, mouseX, mouseY, x, currentY, width, compHeight, textRenderer);
                    currentY += compHeight;
                }
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
             module.onClick();
             return true;
        } else if (button == 1) {
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
                    settingsHeight += comp.getHeight();
                }
            }
        }
        return 12 + settingsHeight;
    }

    public boolean mouseClickedSettings(double mouseX, double mouseY, int button, int x, int y) {
        if (extended && !settings.isEmpty()) {
            int currentY = y + 12;
            for (xenus.xenaddons.gui.clickgui.components.SettingComponent comp : settings) {
                if (comp.setting.isVisible()) {
                    int compHeight = comp.getHeight();
                    if (mouseX >= x && mouseX < x + Panel.WIDTH && mouseY >= currentY && mouseY < currentY + compHeight) {
                        if (comp.mouseClicked(mouseX, mouseY, button, x, currentY, Panel.WIDTH)) return true;
                    }
                    currentY += compHeight;
                }
            }
        }
        return false;
    }

    public void mouseReleasedSettings() {
        for (xenus.xenaddons.gui.clickgui.components.SettingComponent comp : settings) {
             comp.mouseReleased();
        }
    }

    public boolean charTyped(net.minecraft.client.input.CharInput input) {
        if (extended) {
            for (xenus.xenaddons.gui.clickgui.components.SettingComponent comp : settings) {
                if (comp.setting.isVisible() && comp.charTyped(input)) return true;
            }
        }
        return false;
    }

    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (extended) {
            for (xenus.xenaddons.gui.clickgui.components.SettingComponent comp : settings) {
                if (comp.setting.isVisible() && comp.keyPressed(input)) return true;
            }
        }
        return false;
    }
}
