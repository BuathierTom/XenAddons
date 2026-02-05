package xenus.xenaddons.gui.clickgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import xenus.xenaddons.features.Category;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;

public class ClickGUI extends Screen {
    private final List<Panel> panels = new ArrayList<>();
    
    private static xenus.xenaddons.features.Module hoveredModule = null;
    private static int hoveredX = 0;
    private static int hoveredY = 0;
    private TextFieldWidget searchField;

    public static void setHoveredModule(xenus.xenaddons.features.Module module, int x, int y) {
        hoveredModule = module;
        hoveredX = x;
        hoveredY = y;
    }

    public static String getSearch() {
        return ClickGUI.currentSearch;
    }
    
    private static String currentSearch = "";

    public ClickGUI() {
        super(Text.literal("ClickGUI"));
    }

    @Override
    protected void init() {
        int searchW = 100;
        int searchH = 14;
        // Positioned at the bottom center
        this.searchField = new TextFieldWidget(this.textRenderer, this.width / 2 - searchW / 2, this.height - searchH - 10, searchW, searchH, Text.literal("Search"));
        this.searchField.setText(currentSearch);
        this.searchField.setFocused(true);

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
        currentSearch = searchField.getText();

        this.renderInGameBackground(context);
        
        // Render Search Bar (Scaled 0.8x)
        context.getMatrices().pushMatrix();
        // Calculate centered bottom position for the scaled widget
        float scaledW = 100 * 0.8f;
        float scaledH = 14 * 0.8f;
        context.getMatrices().translate(this.width / 2f - scaledW / 2f, this.height - scaledH - 10f);
        context.getMatrices().scale(0.8f, 0.8f);
        // Render at 0,0 relative to the translated matrix
        searchField.setX(0);
        searchField.setY(0);
        searchField.render(context, (int)((mouseX - (this.width / 2f - scaledW / 2f)) / 0.8f), (int)((mouseY - (this.height - scaledH - 10f)) / 0.8f), delta);
        context.getMatrices().popMatrix();

        // Render Panels
        for (Panel panel : panels) {
            panel.render(context, mouseX, mouseY, delta, textRenderer);
        }

        // Render Description Tooltip (Extreme Micro)
        if (hoveredModule != null && hoveredModule.description != null && !hoveredModule.description.isEmpty()) {
            String text = hoveredModule.description;
            int textWidth = textRenderer.getWidth(text);
            int padding = 2; 
            
            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float)hoveredX, (float)hoveredY);
            context.getMatrices().scale(0.5f, 0.5f); // 0.5x Scale
            
            int boxW = textWidth + padding * 2;
            int boxH = 10 + padding * 2;
            
            context.fill(0, 0, boxW, boxH, 0xEE1A1A1A);
            context.fill(0, 0, boxW, 1, 0xFF00AAFF);
            context.fill(0, boxH - 1, boxW, boxH, 0xFF00AAFF);
            context.fill(0, 0, 1, boxH, 0xFF00AAFF);
            context.fill(boxW - 1, 0, boxW, boxH, 0xFF00AAFF);
            
            context.drawTextWithShadow(textRenderer, text, padding, padding, 0xFFFFFFFF);
            
            context.getMatrices().popMatrix();
        }
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (searchField.charTyped(input)) return true;
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (searchField.keyPressed(input)) return true;
        
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
        double mouseY = click.y();
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
        double mouseY = click.y();
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
        double mouseY = click.y();
        int button = click.button();
        
         for (Panel panel : panels) {
            if (panel.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }
    @Override
    public void close() {
        xenus.xenaddons.config.ConfigManager.save();
        super.close();
    }
}
