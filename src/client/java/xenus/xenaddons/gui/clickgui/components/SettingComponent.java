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
            BooleanSetting boolSetting = (BooleanSetting) setting;
            boolean enabled = boolSetting.getValue();
            boolean isSubSetting = setting.isSubSetting();
            
            // Check if this setting is a parent of other settings (needs expansion logic)
            boolean isParent = false;
            // Ideally we'd scan if any other setting has this as parent, 
            // but for now let's check if it's "Commands" specifically or rely on manual flag if we had one.
            // A simple heuristic: if it has no parent, and others have it as parent (which we can't easily check O(1) here without list)
            // But let's look at the expanded flag on BooleanSetting. If it's used as a parent, logic elsewhere handles visibility.
            // We just need to know if we should draw the expansion arrow.
            
            // Actually, ZguegAddonsModule sets "Commands" as parent.
            if ("Commands".equals(setting.name)) {
                isParent = true;
            }
            
            int renderX = x;
            int renderWidth = width;
            
            // Removed indentation for sub-settings as requested
            /*
            if (isSubSetting) {
                renderX += 5;
                renderWidth -= 5;
            }
            */
            
            context.fill(renderX, y, renderX + renderWidth, y + height, enabled ? 0x8800AAFF : 0x882A2A2A);
            context.drawTextWithShadow(textRenderer, setting.name, renderX + 5, y + 5, 0xFFFFFFFF);
            
            if (isParent) {
                 String arrow = boolSetting.expanded ? "v" : ">";
                 context.drawTextWithShadow(textRenderer, arrow, renderX + renderWidth - 15, y + 5, 0xFFFFFFFF);
            }
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
        }
        return false;
    }
}
