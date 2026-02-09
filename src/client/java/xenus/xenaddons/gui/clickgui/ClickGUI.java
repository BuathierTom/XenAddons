package xenus.xenaddons.gui.clickgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import xenus.xenaddons.features.Category;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;

public class ClickGUI extends Screen {
    private final List<Panel> panels = new ArrayList<>();
    private double scrollY = 0;
    
    private static xenus.xenaddons.features.Module hoveredModule = null;
    private static int hoveredX = 0;
    private static int hoveredY = 0;

    public static void setHoveredModule(xenus.xenaddons.features.Module module, int x, int y) {
        hoveredModule = module;
        hoveredX = x;
        hoveredY = y;
    }

    public static String getSearch() {
        return ""; // Search disabled
    }

    public ClickGUI() {
        super(Text.literal("ClickGUI"));
    }

    @Override
    protected void init() {
        if (panels.isEmpty()) {
            int x = 20;
            for (Category category : Category.values()) {
                panels.add(new Panel(category, x, 40));
                x += Panel.WIDTH + 15;
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        hoveredModule = null; 

        context.fill(0, 0, this.width, this.height, 0x99000000); // Dark semi-transparent overlay
        
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0, (float)scrollY);

        // Render Panels
        for (Panel panel : panels) {
            panel.render(context, mouseX, (int)(mouseY - scrollY), delta, textRenderer);
        }

        // Render Description Tooltip (Extreme Micro)
        if (hoveredModule != null && hoveredModule.description != null && !hoveredModule.description.isEmpty()) {
            String text = hoveredModule.description;
            int textWidth = textRenderer.getWidth(text);
            int padding = 2; 
            int accentColor = xenus.xenaddons.features.ModuleManager.colors.getColor();
            
            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float)hoveredX, (float)hoveredY);
            context.getMatrices().scale(0.5f, 0.5f); // 0.5x Scale
            
            int boxW = textWidth + padding * 2;
            int boxH = 10 + padding * 2;
            
            context.fill(0, 0, boxW, boxH, 0xEE1A1A1A);
            context.fill(0, 0, boxW, 1, accentColor);
            context.fill(0, boxH - 1, boxW, boxH, accentColor);
            context.fill(0, 0, 1, boxH, accentColor);
            context.fill(boxW - 1, 0, boxW, boxH, accentColor);
            
            context.drawTextWithShadow(textRenderer, text, padding, padding, 0xFFFFFFFF);
            
            context.getMatrices().popMatrix();
        }

        context.getMatrices().popMatrix();
    }

    @Override
    public boolean charTyped(CharInput input) {
        for (Panel panel : panels) {
            if (panel.charTyped(input)) return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        for (Panel panel : panels) {
            if (panel.keyPressed(input)) return true;
        }
        
        int keyCode = input.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
             this.close();
             return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y() - scrollY;
        int button = click.button();
        
        for (Panel panel : panels) {
            if (panel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }
    
    @Override
    public boolean mouseReleased(Click click) {
        double mouseX = click.x();
        double mouseY = click.y() - scrollY;
        int button = click.button();
        
        for (Panel panel : panels) {
            if (panel.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseReleased(click);
    }
    
    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        double mouseX = click.x();
        double mouseY = click.y() - scrollY;
        int button = click.button();
        
         for (Panel panel : panels) {
            if (panel.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollY += verticalAmount * 20;
        if (scrollY > 0) scrollY = 0;
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    @Override
    public void close() {
        xenus.xenaddons.config.ConfigManager.save();
        super.close();
    }
}
