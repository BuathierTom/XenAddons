package xenus.xenaddons.gui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import xenus.xenaddons.compat.XenSoundCompat;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GuiInvButtonMenu extends Screen {

    // List to hold our custom button objects
    private final List<MenuButton> menuButtons = new ArrayList<>();

    public GuiInvButtonMenu() {
        super(Component.literal("Inventory Buttons"));
    }

    private static final int PANEL_W = 226;
    private static final int PANEL_H = 142;
    private static final int PAD = 14;
    private static final int HEADER_H = 24;
    private static final int BTN_H = 22;
    private static final int BTN_GAP = 8;

    private int panelX;
    private int panelY;

    protected void init() {
        menuButtons.clear();
        panelX = (this.width - PANEL_W) / 2;
        panelY = (this.height - PANEL_H) / 2;

        int btnWidth = PANEL_W - (PAD * 2);
        int startY = panelY + HEADER_H + 14;
        int centerX = panelX + PAD;

        menuButtons.add(new MenuButton(centerX, startY, btnWidth, BTN_H, "Configuration", () -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new GuiInvButtonConfig(this));
            }
        }));

        menuButtons.add(new MenuButton(centerX, startY + BTN_H + BTN_GAP, btnWidth, BTN_H, "Edit Buttons", () -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new GuiInvButtonEditor(this));
            }
        }));

        menuButtons.add(new MenuButton(centerX, startY + (BTN_H + BTN_GAP) * 2, btnWidth, BTN_H, "Open Config Folder", () -> {
            Path configDir = FabricLoader.getInstance().getConfigDir().resolve("xenaddons");
            File file = configDir.toFile();
            if (!file.exists()) {
                file.mkdirs();
            }
            Util.getPlatform().openFile(file);
        }));
    }

    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        context.fill(0, 0, this.width, this.height, InvButtonsUi.SCRIM);

        InvButtonsUi.panel(context, panelX, panelY, PANEL_W, PANEL_H);
        InvButtonsUi.header(context, font, panelX, panelY, PANEL_W, HEADER_H, "Inventory Buttons");

        for (int i = 0; i < menuButtons.size(); i++) {
            MenuButton btn = menuButtons.get(i);
            boolean hover = InvButtonsUi.hovered(mouseX, mouseY, btn.x, btn.y, btn.width, btn.height);
            InvButtonsUi.button(context, font, btn.x, btn.y, btn.width, btn.height, btn.label, hover,
                    i == 0 ? InvButtonsUi.Tone.ACCENT : InvButtonsUi.Tone.NEUTRAL);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button == 0) { // Left MouseButtonEvent
            for (MenuButton btn : menuButtons) {
                if (mouseX >= btn.x && mouseX < btn.x + btn.width &&
                        mouseY >= btn.y && mouseY < btn.y + btn.height) {

                    // play click sound
                    if (this.minecraft != null) {
                        XenSoundCompat.playButtonClick(this.minecraft);
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
