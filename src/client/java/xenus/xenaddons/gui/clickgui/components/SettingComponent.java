package xenus.xenaddons.gui.clickgui.components;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import xenus.xenaddons.gui.clickgui.Panel;
import xenus.xenaddons.settings.ActionSetting;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.settings.ColorSetting;
import xenus.xenaddons.settings.NumberSetting;
import xenus.xenaddons.settings.Setting;

public class SettingComponent {
    public final Setting<?> setting;
    private ColorPickerComponent colorPicker = null;
    
    public boolean listening = false; // For KeybindSetting

    public SettingComponent(Setting<?> setting) {
        this.setting = setting;
        if (setting instanceof ColorSetting) {
            this.colorPicker = new ColorPickerComponent((ColorSetting) setting);
        }
    }
    
    public void render(DrawContext context, int mouseX, int mouseY, int x, int y, int width, int height, TextRenderer textRenderer) {
        if (!setting.isVisible()) return;

        // Hover effect
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            context.fill(x, y, x + width, y + height, 0x22FFFFFF);
        }

        if (setting instanceof BooleanSetting) {
            BooleanSetting boolSetting = (BooleanSetting) setting;
            boolean enabled = boolSetting.getValue();
            
            String arrow = null;
            if ("Commands".equals(setting.name) || "Chat Hider".equals(setting.name)) {
                arrow = boolSetting.expanded ? "-" : "+";
            }

            context.getMatrices().pushMatrix();
            if (arrow != null) {
                context.getMatrices().pushMatrix();
                context.getMatrices().translate(x + 6, y + (height - 9 * 0.7f) / 2f + 0.5f);
                context.getMatrices().scale(0.7f, 0.7f);
                context.drawTextWithShadow(textRenderer, arrow, 0, 0, 0xFFBBBBBB);
                context.getMatrices().popMatrix();
                context.getMatrices().translate(x + 15, y + (height - 9 * 0.7f) / 2f + 0.5f);
            } else {
                context.getMatrices().translate(x + 6, y + (height - 9 * 0.7f) / 2f + 0.5f);
            }
            context.getMatrices().scale(0.7f, 0.7f);
            context.drawTextWithShadow(textRenderer, setting.name, 0, 0, 0xFFE0E0E0);
            context.getMatrices().popMatrix();
            
            int switchW = 16, switchH = 8;
            int switchX = x + width - switchW - 6;
            int switchY = y + (height - switchH) / 2;
            int themeColor = xenus.xenaddons.features.ModuleManager.colors.getColor();
            context.fill(switchX, switchY, switchX + switchW, switchY + switchH, enabled ? themeColor : 0xFF353535);
            int knobSize = 6;
            int knobX = enabled ? (switchX + switchW - knobSize - 1) : (switchX + 1);
            context.fill(knobX, switchY + 1, knobX + knobSize, switchY + switchH - 1, 0xFFFFFFFF);

        } else if (setting instanceof NumberSetting) {
            NumberSetting numSetting = (NumberSetting) setting;
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(x + 6, y + (height - 9 * 0.7f) / 2f + 0.5f);
            context.getMatrices().scale(0.7f, 0.7f);
            context.drawTextWithShadow(textRenderer, setting.name + ": " + numSetting.getValue().intValue(), 0, 0, 0xFFE0E0E0);
            context.getMatrices().popMatrix();

            int barW = 40, barH = 2;
            int barX = x + width - barW - 6;
            int barY = y + (height - barH) / 2;
            context.fill(barX, barY, barX + barW, barY + barH, 0xFF353535);
            double percent = (numSetting.getValue() - numSetting.getMin()) / (numSetting.getMax() - numSetting.getMin());
            int knobX = barX + (int)(percent * barW);
            int themeColor = xenus.xenaddons.features.ModuleManager.colors.getColor();
            context.fill(knobX - 2, barY - 3, knobX + 2, barY + 5, themeColor);

        } else if (setting instanceof ColorSetting) {
            ColorSetting colorSetting = (ColorSetting) setting;
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(x + 6, y + (10 - 9 * 0.7f) / 2f + 0.5f);
            context.getMatrices().scale(0.7f, 0.7f);
            context.drawTextWithShadow(textRenderer, setting.name, 0, 0, 0xFFE0E0E0);
            context.getMatrices().popMatrix();

            int prevW = 20, prevH = 8;
            int prevX = x + width - prevW - 6;
            int prevY = y + (10 - prevH) / 2;
            context.fill(prevX, prevY, prevX + prevW, prevY + prevH, colorSetting.getValue());
            xenus.xenaddons.utils.RenderUtils.drawBorder(context, prevX, prevY, prevW, prevH, 0xFFFFFFFF);

            if (colorSetting.expanded) {
                colorPicker.render(context, mouseX, mouseY, x + 5, y + 12, width - 10, textRenderer);
            }
        } else if (setting instanceof ActionSetting) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(x + 6, y + (height - 9 * 0.7f) / 2f + 0.5f);
            context.getMatrices().scale(0.7f, 0.7f);
            context.drawTextWithShadow(textRenderer, setting.name + " >", 0, 0, 0xFFBBBBBB);
            context.getMatrices().popMatrix();
            
        } else if (setting instanceof xenus.xenaddons.settings.KeybindSetting) {
             xenus.xenaddons.settings.KeybindSetting kb = (xenus.xenaddons.settings.KeybindSetting) setting;
             
             context.getMatrices().pushMatrix();
             context.getMatrices().translate(x + 6, y + (height - 9 * 0.7f) / 2f + 0.5f);
             context.getMatrices().scale(0.7f, 0.7f);
             context.drawTextWithShadow(textRenderer, setting.name, 0, 0, 0xFFE0E0E0);
             context.getMatrices().popMatrix();
             
             String keyName;
             if (listening) {
                 keyName = "...";
             } else {
                 int key = kb.getKey();
                 if (key == -1) keyName = "NONE";
                 else if (key < 0) {
                     // Mouse button handling
                     int btn = -key - 100;
                     if (btn == 0) keyName = "LMOUSE";
                     else if (btn == 1) keyName = "RMOUSE";
                     else if (btn == 2) keyName = "MMOUSE";
                     else keyName = "MOUSE " + (btn + 1);
                 } else {
                     String n = org.lwjgl.glfw.GLFW.glfwGetKeyName(key, 0);
                     if (n == null) {
                         // Fallback for special keys
                         if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) keyName = "LSHIFT";
                         else if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) keyName = "RSHIFT";
                         else if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) keyName = "LCTRL";
                         else if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL) keyName = "RCTRL";
                         else if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT) keyName = "LALT";
                         else if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT) keyName = "RALT";
                         else if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) keyName = "TAB";
                         else if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) keyName = "ENTER";
                         else keyName = "KEY " + key;
                     } else {
                         keyName = n.toUpperCase();
                     }
                 }
             }
             
             keyName = "[" + keyName + "]";
             
             context.getMatrices().pushMatrix();
             float scale = 0.7f;
             int knW = textRenderer.getWidth(keyName);
             context.getMatrices().translate(x + width - (knW * scale) - 6, y + (height - 9 * scale) / 2f + 0.5f);
             context.getMatrices().scale(scale, scale);
             context.drawTextWithShadow(textRenderer, keyName, 0, 0, listening ? 0xFFFFFF00 : 0xFFAAAAAA);
             context.getMatrices().popMatrix();
        }
    }

    public int getHeight() {
        if (!setting.isVisible()) return 0;
        if (setting instanceof ColorSetting && ((ColorSetting)setting).expanded) {
            return 12 + (Panel.WIDTH - 20) + 30; // Approx height for picker
        }
        return 10;
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width) {
        if (setting instanceof BooleanSetting) {
             BooleanSetting boolSetting = (BooleanSetting) setting;
             boolean isParent = "Commands".equals(setting.name) || "Chat Hider".equals(setting.name);
             if (isParent) {
                 boolean onArrow = mouseX >= x + width - 20;
                 if ((button == 0 && onArrow) || button == 1) {
                     boolSetting.expanded = !boolSetting.expanded;
                     return true;
                 }
                 if (button == 0 && !onArrow) {
                     boolSetting.toggle();
                     return true;
                 }
             } else if (button == 0) {
                 boolSetting.toggle();
                 return true;
             }
        } else if (setting instanceof NumberSetting) {
            NumberSetting numSetting = (NumberSetting) setting;
            int barW = 40, barX = x + width - barW - 6;
            if (mouseX >= barX - 2 && mouseX <= barX + barW + 2) {
                double percent = (mouseX - barX) / (double)barW;
                double newValue = numSetting.getMin() + (percent * (numSetting.getMax() - numSetting.getMin()));
                numSetting.setValue(newValue);
                return true;
            }
        } else if (setting instanceof ColorSetting) {
            // ... (ColorSetting logic kept externally or assumed unchanged)
            ColorSetting colorSetting = (ColorSetting) setting;
            if (mouseY < y + 10) { 
                if (button == 0 || button == 1) {
                    colorSetting.expanded = !colorSetting.expanded;
                    return true;
                }
            } else if (colorSetting.expanded) {
                return colorPicker.mouseClicked(mouseX, mouseY, button, x + 5, y + 12, width - 10);
            }
        } else if (setting instanceof ActionSetting) {
            if (button == 0) {
                ((ActionSetting) setting).run();
                return true;
            }
        } else if (setting instanceof xenus.xenaddons.settings.KeybindSetting) {
            xenus.xenaddons.settings.KeybindSetting kb = (xenus.xenaddons.settings.KeybindSetting) setting;
            if (listening) {
                kb.setKey(-100 - button);
                listening = false;
                return true;
            } else {
                if (button == 0) {
                    listening = true;
                    return true;
                } else if (button == 1) {
                    // Right click to clear? or just listen?
                    // Standard is Left Click to listen. 
                    // Optional: Right click to reset/clear.
                    kb.setKey(-1);
                    return true;
                }
            }
        }
        return false;
    }

    public void mouseReleased() {
        if (colorPicker != null) colorPicker.mouseReleased();
    }

    public boolean charTyped(net.minecraft.client.input.CharInput input) {
        if (setting instanceof ColorSetting && ((ColorSetting)setting).expanded) {
            return colorPicker.charTyped(input);
        }
        return false;
    }

    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (setting instanceof ColorSetting && ((ColorSetting)setting).expanded) {
            return colorPicker.keyPressed(input);
        }
        
        if (setting instanceof xenus.xenaddons.settings.KeybindSetting && listening) {
             xenus.xenaddons.settings.KeybindSetting kb = (xenus.xenaddons.settings.KeybindSetting) setting;
             int key = input.key();
             
             if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                 kb.setKey(-1); // Unbind
             } else if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
                 kb.setKey(-1);
             } else {
                 kb.setKey(key);
             }
             listening = false;
             return true;
        }
        
        return false;
    }
}
