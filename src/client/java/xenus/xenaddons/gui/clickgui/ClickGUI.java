package xenus.xenaddons.gui.clickgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;
import xenus.xenaddons.features.Category;
import java.util.ArrayList;
import java.util.List;

public class ClickGUI extends Screen {
    private final List<Panel> panels = new ArrayList<>();

    public ClickGUI() {
        super(Text.literal("ClickGUI"));
    }

    @Override
    protected void init() {
        if (panels.isEmpty()) {
            int x = 10;
            for (Category category : Category.values()) {
                panels.add(new Panel(category, x, 10));
                x += Panel.WIDTH + 10;
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderInGameBackground(context);
        
        for (Panel panel : panels) {
            panel.render(context, mouseX, mouseY, delta, textRenderer);
        }
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
}
