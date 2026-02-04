package xenus.xenaddons.gui.clickgui.components;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.settings.Setting;

public class SettingComponent {
    public final Setting<?> setting;
    
    public SettingComponent(Setting<?> setting) {
        this.setting = setting;
    }
    
    public void render(DrawContext context, int mouseX, int mouseY, int x, int y, int width, int height, TextRenderer textRenderer) {
        if (setting instanceof BooleanSetting) {
            boolean enabled = ((BooleanSetting) setting).getValue();
            context.fill(x, y, x + width, y + height, enabled ? 0x8800AAFF : 0x882A2A2A);
            context.drawTextWithShadow(textRenderer, setting.name, x + 5, y + 5, 0xFFFFFFFF);
        }
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (setting instanceof BooleanSetting) {
             ((BooleanSetting) setting).toggle();
             return true;
        }
        return false;
    }
}
