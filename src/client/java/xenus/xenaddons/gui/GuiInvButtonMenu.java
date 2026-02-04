package xenus.xenaddons.gui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GuiInvButtonMenu extends Screen {

    // List to hold our custom button objects
    private final List<MenuButton> menuButtons = new ArrayList<>();

    public GuiInvButtonMenu() {
        super(Text.literal("Inventory Buttons"));
    }

    protected void init() {
        menuButtons.clear();
        int btnWidth = 200;
        int btnHeight = 20;
        int spacing = 24;

        int startY = this.height / 4 + 40;
        int centerX = this.width / 2 - btnWidth / 2;

        // Config Button
        menuButtons.add(new MenuButton(centerX, startY, btnWidth, btnHeight, "Config", () -> {
            if (this.client != null) {
                this.client.setScreen(new GuiInvButtonConfig(this));
            }
        }));

        // Edit Buttons Button
        menuButtons.add(new MenuButton(centerX, startY + spacing, btnWidth, btnHeight, "Edit Buttons", () -> {
            if (this.client != null) {
                this.client.setScreen(new GuiInvButtonEditor(this));
            }
        }));

        // Open Config Folder Button
        menuButtons.add(new MenuButton(centerX, startY + spacing * 2, btnWidth, btnHeight, "Open Config Folder", () -> {
            Path configDir = FabricLoader.getInstance().getConfigDir().resolve("xenaddons");
            File file = configDir.toFile();
            if (!file.exists()) {
                file.mkdirs();
            }
            Util.getOperatingSystem().open(file);
        }));
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderInGameBackground(context);

        // --- Render Title ---
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(this.width / 2.0f, 60.0f);
        float scale = 2.0f;
        context.getMatrices().scale(scale, scale);
        context.drawText(this.textRenderer, title, -this.textRenderer.getWidth(title) / 2, 0, 0xFFFFFFFF, true);
        context.getMatrices().popMatrix();

        // --- Render Custom Buttons ---
        // draw each button in list
        for (MenuButton btn : menuButtons) {
            boolean isHovered = mouseX >= btn.x && mouseX < btn.x + btn.width &&
                    mouseY >= btn.y && mouseY < btn.y + btn.height;

            // Style: Darker transparent black
            // Normal: 0x80 (50% opacity) | Hover: 0xA0 (62% opacity)
            int color = isHovered ? 0xA0000000 : 0x80000000;

            context.fill(btn.x, btn.y, btn.x + btn.width, btn.y + btn.height, color);

            // button
            int textWidth = this.textRenderer.getWidth(btn.label);
            int textX = btn.x + (btn.width - textWidth) / 2;
            int textY = btn.y + (btn.height - this.textRenderer.fontHeight) / 2;

            // label
            context.drawText(this.textRenderer, btn.label, textX, textY, 0xFFFFFFFF, false);
        }

        // Do NOT call super.render() to avoid drawing default widget layers if any exist
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button == 0) { // Left Click
            for (MenuButton btn : menuButtons) {
                if (mouseX >= btn.x && mouseX < btn.x + btn.width &&
                        mouseY >= btn.y && mouseY < btn.y + btn.height) {

                    // play click sound
                    if (this.client != null) {
                        this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    }
                    // Run action
                    btn.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    // Helper Record for Custom Buttons
    private record MenuButton(int x, int y, int width, int height, String label, Runnable action) {}
}