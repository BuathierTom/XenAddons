package xenus.xenaddons.gui.clickgui.components;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import xenus.xenaddons.settings.ColorSetting;
import xenus.xenaddons.utils.RenderUtils;

public class ColorPickerComponent {
    private final ColorSetting setting;
    private boolean pickingPalette = false;
    private boolean pickingHue = false;
    private boolean pickingAlpha = false;
    private TextFieldWidget hexField = null;

    public ColorPickerComponent(ColorSetting setting) {
        this.setting = setting;
    }

    private void initHexField(TextRenderer textRenderer) {
        if (hexField == null) {
            this.hexField = new TextFieldWidget(textRenderer, 0, 0, 60, 12, Text.literal("Hex"));
            this.hexField.setMaxLength(9);
            this.hexField.setDrawsBackground(false);
            this.hexField.setText(String.format("%08X", setting.getValue()));
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, int x, int y, int width, TextRenderer textRenderer) {
        initHexField(textRenderer);
        
        int pickerSize = width - 25;
        int paletteX = x + 5;
        int paletteY = y + 5;
        int totalH = pickerSize + 45;
        
        // Background
        context.fill(x, y, x + width, y + totalH, 0xDD000000);
        RenderUtils.drawBorder(context, x, y, width, totalH, 0xFFA020F0); 

        // 1. Saturation/Brightness Square
        for (int i = 0; i < pickerSize; i++) {
            for (int j = 0; j < pickerSize; j++) {
                float s = i / (float) pickerSize;
                float v = 1.0f - (j / (float) pickerSize);
                int color = java.awt.Color.HSBtoRGB(setting.hsv[0], s, v);
                context.fill(paletteX + i, paletteY + j, paletteX + i + 1, paletteY + j + 1, color | 0xFF000000);
            }
        }
        
        // 2. Hue Slider
        int hueX = paletteX + pickerSize + 2;
        int hueW = 8;
        for (int i = 0; i < pickerSize; i++) {
            float h = i / (float) pickerSize;
            int color = java.awt.Color.HSBtoRGB(h, 1.0f, 1.0f);
            context.fill(hueX, paletteY + i, hueX + hueW, paletteY + i + 1, color | 0xFF000000);
        }

        // 3. Indicators
        int cursorX = paletteX + (int) (setting.hsv[1] * pickerSize);
        int cursorY = paletteY + (int) ((1.0f - setting.hsv[2]) * pickerSize);
        RenderUtils.drawBorder(context, cursorX - 2, cursorY - 2, 4, 4, 0xFFFFFFFF);
        int hueY = paletteY + (int) (setting.hsv[0] * pickerSize);
        context.fill(hueX - 1, hueY - 1, hueX + hueW + 1, hueY + 1, 0xFFFFFFFF);

        // 4. Alpha Slider
        int alphaY = paletteY + pickerSize + 5;
        int alphaW = pickerSize + hueW + 2;
        int alphaH = 8;
        int baseColor = java.awt.Color.HSBtoRGB(setting.hsv[0], setting.hsv[1], setting.hsv[2]) & 0x00FFFFFF;
        for (int i = 0; i < alphaW; i++) {
            int a = (int)((i / (float) alphaW) * 255);
            context.fill(paletteX + i, alphaY, paletteX + i + 1, alphaY + alphaH, (a << 24) | baseColor);
        }
        RenderUtils.drawBorder(context, paletteX, alphaY, alphaW, alphaH, 0xFFFFFFFF);
        int alphaKnobX = paletteX + (int)((setting.alpha / 255f) * alphaW);
        context.fill(alphaKnobX - 1, alphaY - 1, alphaKnobX + 1, alphaY + alphaH + 1, 0xFFFFFFFF);

        // 5. Preview & Hex Code
        int previewY = alphaY + alphaH + 5;
        int previewW = 15;
        int previewH = 15;
        context.fill(paletteX, previewY, paletteX + previewW, previewY + previewH, setting.getValue());
        RenderUtils.drawBorder(context, paletteX, previewY, previewW, previewH, 0xFFFFFFFF);

        // Hex Field
        int hexX = paletteX + previewW + 4;
        if (hexField != null && !hexField.isFocused()) {
            hexField.setText(String.format("%08X", setting.getValue()));
        }
        
        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float)hexX, (float)(previewY + 3));
        context.getMatrices().scale(0.8f, 0.8f);
        hexField.setX(0);
        hexField.setY(0);
        hexField.render(context, (int)((mouseX - hexX) / 0.8f), (int)((mouseY - (previewY + 3)) / 0.8f), 0);
        context.getMatrices().popMatrix();

        // Interaction
        if (pickingPalette) {
            float s = Math.max(0, Math.min(1, (mouseX - paletteX) / (float) pickerSize));
            float v = 1.0f - Math.max(0, Math.min(1, (mouseY - paletteY) / (float) pickerSize));
            setting.setHSV(setting.hsv[0], s, v);
        }
        if (pickingHue) {
            float h = Math.max(0, Math.min(1, (mouseY - paletteY) / (float) pickerSize));
            setting.setHSV(h, setting.hsv[1], setting.hsv[2]);
        }
        if (pickingAlpha) {
            int a = (int)(Math.max(0, Math.min(1, (mouseX - paletteX) / (float) alphaW)) * 255);
            setting.setAlpha(a);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width) {
        int pickerSize = width - 25;
        int paletteX = x + 5;
        int paletteY = y + 5;
        int hueX = paletteX + pickerSize + 2;
        int alphaY = paletteY + pickerSize + 5;
        int alphaW = pickerSize + 8 + 2;

        if (mouseX >= paletteX && mouseX <= paletteX + pickerSize && mouseY >= paletteY && mouseY <= paletteY + pickerSize) {
            pickingPalette = true;
            return true;
        }
        if (mouseX >= hueX && mouseX <= hueX + 8 && mouseY >= paletteY && mouseY <= paletteY + pickerSize) {
            pickingHue = true;
            return true;
        }
        if (mouseX >= paletteX && mouseX <= paletteX + alphaW && mouseY >= alphaY && mouseY <= alphaY + 8) {
            pickingAlpha = true;
            return true;
        }
        
        if (hexField != null) {
            int previewY = alphaY + 8 + 5;
            int hexMinX = paletteX + 15 + 4;
            int hexMinY = previewY + 3;
            if (mouseX >= hexMinX && mouseX <= hexMinX + (60*0.8f) && mouseY >= hexMinY && mouseY <= hexMinY + (12*0.8f)) {
                hexField.setFocused(true);
                return true;
            } else {
                hexField.setFocused(false);
            }
        }
        return false;
    }

    public void mouseReleased() {
        pickingPalette = false;
        pickingHue = false;
        pickingAlpha = false;
    }

    public boolean charTyped(net.minecraft.client.input.CharInput input) {
        if (hexField != null && hexField.isFocused()) {
            if (hexField.charTyped(input)) {
                updateColorFromHex();
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (hexField != null && hexField.isFocused()) {
            if (hexField.keyPressed(input)) {
                updateColorFromHex();
                return true;
            }
        }
        return false;
    }

    private void updateColorFromHex() {
        try {
            String text = hexField.getText();
            if (text.startsWith("#")) text = text.substring(1);
            long val = Long.parseLong(text, 16);
            if (text.length() <= 6) val |= 0xFF000000L;
            setting.setValue((int)val);
        } catch (Exception ignored) {}
    }
}
