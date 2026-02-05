package xenus.xenaddons.gui.clickgui.components;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.settings.NumberSetting;
import xenus.xenaddons.settings.Setting;

public class SettingComponent {
    public final Setting<?> setting;
    
    public SettingComponent(Setting<?> setting) {
        this.setting = setting;
    }
    
    public void render(DrawContext context, int mouseX, int mouseY, int x, int y, int width, int height, TextRenderer textRenderer) {
        if (!setting.isVisible()) return;

        // Hover effect for setting
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            context.fill(x, y, x + width, y + height, 0x22FFFFFF);
        }

        if (setting instanceof BooleanSetting) {
            BooleanSetting boolSetting = (BooleanSetting) setting;
            boolean enabled = boolSetting.getValue();
            
            // Scaled Icon and Name
            String arrow = null;
            if (setting instanceof BooleanSetting && "Commands".equals(setting.name)) {
                arrow = ((BooleanSetting)setting).expanded ? "-" : "+";
            }

            context.getMatrices().pushMatrix();
            if (arrow != null) {
                // Render Arrow at the beginning
                context.getMatrices().pushMatrix();
                context.getMatrices().translate(x + 6, y + (height - 9 * 0.7f) / 2f + 0.5f);
                context.getMatrices().scale(0.7f, 0.7f);
                context.drawTextWithShadow(textRenderer, arrow, 0, 0, 0xFFBBBBBB);
                context.getMatrices().popMatrix();
                
                // Shift Name to the right
                context.getMatrices().translate(x + 15, y + (height - 9 * 0.7f) / 2f + 0.5f);
            } else {
                // Normal position
                context.getMatrices().translate(x + 6, y + (height - 9 * 0.7f) / 2f + 0.5f);
            }
            context.getMatrices().scale(0.7f, 0.7f);
            context.drawTextWithShadow(textRenderer, setting.name, 0, 0, 0xFFE0E0E0);
            context.getMatrices().popMatrix();
            
            // Toggle Switch on the Right
            int switchW = 16;
            int switchH = 8;
            int switchX = x + width - switchW - 6;
            int switchY = y + (height - switchH) / 2;
            
            // Switch Background (Dynamic Theme)
            int themeColor = xenus.xenaddons.features.ModuleManager.colors.getColor();
            context.fill(switchX, switchY, switchX + switchW, switchY + switchH, enabled ? themeColor : 0xFF353535);
            
            // Switch Knob
            int knobSize = 6;
            int knobX = enabled ? (switchX + switchW - knobSize - 1) : (switchX + 1);
            context.fill(knobX, switchY + 1, knobX + knobSize, switchY + switchH - 1, 0xFFFFFFFF);
            // ... (rest of search arrow logic if needed)
        } else if (setting instanceof NumberSetting) {
            NumberSetting numSetting = (NumberSetting) setting;
            
            // Name label (Scaled)
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(x + 6, y + (height - 9 * 0.7f) / 2f + 0.5f);
            context.getMatrices().scale(0.7f, 0.7f);
            context.drawTextWithShadow(textRenderer, setting.name + ": " + numSetting.getValue().intValue(), 0, 0, 0xFFE0E0E0);
            context.getMatrices().popMatrix();

            // Slider Bar
            int barW = 40;
            int barH = 2;
            int barX = x + width - barW - 6;
            int barY = y + (height - barH) / 2;
            context.fill(barX, barY, barX + barW, barY + barH, 0xFF353535);

            // Slider Knob (Theme Color)
            double percent = (numSetting.getValue() - numSetting.getMin()) / (numSetting.getMax() - numSetting.getMin());
            int knobX = barX + (int)(percent * barW);
            int themeColor = xenus.xenaddons.features.ModuleManager.colors.getColor();
            context.fill(knobX - 2, barY - 3, knobX + 2, barY + 5, themeColor);
        }
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width) {
        if (setting instanceof BooleanSetting) {
             BooleanSetting boolSetting = (BooleanSetting) setting;
             
             // Check if this setting is a parent (e.g. "Commands")
             boolean isParent = "Commands".equals(setting.name);
             
             if (isParent) {
                 // Check if click is on the arrow area (Right 20 pixels)
                 boolean onArrow = mouseX >= x + width - 20;
                 
                 // Left Click on Arrow -> Expand
                 if (button == 0 && onArrow) {
                     boolSetting.expanded = !boolSetting.expanded;
                     return true;
                 }
                 
                 // Right Click anywhere -> Expand
                 if (button == 1) {
                     boolSetting.expanded = !boolSetting.expanded;
                     return true;
                 }
                 
                 // Left Click elsewhere -> Toggle Value
                 if (button == 0 && !onArrow) {
                     boolSetting.toggle();
                     return true;
                 }
             } else {
                 // Normal Setting
                 if (button == 0) {
                     boolSetting.toggle();
                     return true;
                  }
             }
        } else if (setting instanceof NumberSetting) {
            NumberSetting numSetting = (NumberSetting) setting;
            int barW = 40;
            int barX = x + width - barW - 6;
            
            if (mouseX >= barX - 2 && mouseX <= barX + barW + 2) {
                double percent = (mouseX - barX) / (double)barW;
                double newValue = numSetting.getMin() + (percent * (numSetting.getMax() - numSetting.getMin()));
                numSetting.setValue(newValue);
                return true;
            }
        }
        return false;
    }
}
