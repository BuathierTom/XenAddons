package xenus.xenaddons.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import xenus.xenaddons.config.ConfigManager;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.features.impl.dungeons.AutoPetRuleTitleModule;
import xenus.xenaddons.features.impl.dungeons.PosTitlesModule;
import xenus.xenaddons.hud.HudState;
import xenus.xenaddons.utils.RenderUtils;

public class HudEditorScreen extends Screen {
    private static final Text POS_TITLES_PREVIEW = Text.literal("Player (M) At P2!");
    private static final Text AUTOPET_TITLE_PREVIEW = Text.literal("Golden Dragon");
    private static final Text AUTOPET_SUB_PREVIEW = Text.literal("Autopet");

    private final Screen parent;
    private PosTitlesModule posTitlesModule;
    private AutoPetRuleTitleModule autoPetModule;

    private ComponentType dragging = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private static final int RESET_BTN_W = 100;
    private static final int RESET_BTN_H = 16;
    private static final int RESET_BTN_MARGIN = 10;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("Edit HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        posTitlesModule = ModuleManager.getModule(PosTitlesModule.class);
        autoPetModule = ModuleManager.getModule(AutoPetRuleTitleModule.class);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x88000000);

        context.drawCenteredTextWithShadow(textRenderer, "HUD Editor", width / 2, 12, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "Drag components to move them. Press ESC to save and close.", width / 2, 26, 0xFFBFBFBF);

        Bounds posBounds = getPosTitlesBounds();
        Bounds petBounds = getAutoPetBounds();

        renderComponentBox(context, mouseX, mouseY, posBounds, "Pos Titles");
        renderPosTitlesPreview(context, posBounds);

        renderComponentBox(context, mouseX, mouseY, petBounds, "AutoPet Title");
        renderAutoPetPreview(context, petBounds);

        renderResetButton(context, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);

        int mouseX = (int) click.x();
        int mouseY = (int) click.y();

        if (isOverResetButton(mouseX, mouseY)) {
            resetPlacement();
            return true;
        }

        Bounds petBounds = getAutoPetBounds();
        if (petBounds.contains(mouseX, mouseY)) {
            dragging = ComponentType.AUTOPET_TITLE;
            dragOffsetX = mouseX - petBounds.x;
            dragOffsetY = mouseY - petBounds.y;
            return true;
        }

        Bounds posBounds = getPosTitlesBounds();
        if (posBounds.contains(mouseX, mouseY)) {
            dragging = ComponentType.POS_TITLES;
            dragOffsetX = mouseX - posBounds.x;
            dragOffsetY = mouseY - posBounds.y;
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (dragging == null) return super.mouseDragged(click, deltaX, deltaY);

        int mouseX = (int) click.x();
        int mouseY = (int) click.y();
        int newX = mouseX - dragOffsetX;
        int newY = mouseY - dragOffsetY;

        if (dragging == ComponentType.POS_TITLES && posTitlesModule != null) {
            Bounds posBounds = getPosTitlesBounds();
            int centerX = newX + (posBounds.w / 2);
            posTitlesModule.setHudOffsetX(centerX - (width / 2));
            posTitlesModule.setHudY(newY);
            return true;
        }

        if (dragging == ComponentType.AUTOPET_TITLE && autoPetModule != null) {
            Bounds petBounds = getAutoPetBounds();
            int centerX = newX + petBounds.w / 2;
            autoPetModule.setHudOffsetX(centerX - (width / 2));
            autoPetModule.setHudOffsetY(newY - (height / 2));
            return true;
        }

        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount == 0.0) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

        ComponentType target = dragging;
        if (target == null) {
            int mx = (int) mouseX;
            int my = (int) mouseY;
            if (getAutoPetBounds().contains(mx, my)) target = ComponentType.AUTOPET_TITLE;
            else if (getPosTitlesBounds().contains(mx, my)) target = ComponentType.POS_TITLES;
        }

        if (target == ComponentType.AUTOPET_TITLE && autoPetModule != null) {
            double step = verticalAmount > 0 ? 0.05 : -0.05;
            autoPetModule.setHudScale(autoPetModule.getHudScale() + step);
            return true;
        }

        if (target == ComponentType.POS_TITLES && posTitlesModule != null) {
            double step = verticalAmount > 0 ? 0.05 : -0.05;
            posTitlesModule.setHudScale(posTitlesModule.getHudScale() + step);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        ConfigManager.save();
        if (this.client != null) this.client.setScreen(parent);
    }

    private void renderResetButton(DrawContext context, int mouseX, int mouseY) {
        int x = width - RESET_BTN_W - RESET_BTN_MARGIN;
        int y = height - RESET_BTN_H - RESET_BTN_MARGIN;
        boolean hovered = isOverResetButton(mouseX, mouseY);

        int bg = hovered ? 0xCC8A2525 : 0xCC5E1C1C;
        int border = hovered ? 0xFFFF8A8A : 0xFFE07070;

        context.fill(x, y, x + RESET_BTN_W, y + RESET_BTN_H, bg);
        RenderUtils.drawBorder(context, x, y, RESET_BTN_W, RESET_BTN_H, border);
        context.drawCenteredTextWithShadow(textRenderer, "Reset placement", x + (RESET_BTN_W / 2), y + 5, 0xFFFFFFFF);
    }

    private boolean isOverResetButton(int mouseX, int mouseY) {
        int x = width - RESET_BTN_W - RESET_BTN_MARGIN;
        int y = height - RESET_BTN_H - RESET_BTN_MARGIN;
        return mouseX >= x && mouseX <= x + RESET_BTN_W && mouseY >= y && mouseY <= y + RESET_BTN_H;
    }

    private void resetPlacement() {
        if (posTitlesModule != null) {
            posTitlesModule.setHudOffsetX(0);
            posTitlesModule.setHudY(30);
        }
        if (autoPetModule != null) {
            autoPetModule.setHudOffsetX(0);
            autoPetModule.setHudOffsetY(-20);
        }
    }

    private Bounds getPosTitlesBounds() {
        int textWidth = textRenderer.getWidth(POS_TITLES_PREVIEW);
        float scale = posTitlesModule != null ? (float) posTitlesModule.getHudScale() : 1.0f;
        int scaledWidth = (int) Math.ceil(textWidth * scale);
        int x = (width - scaledWidth) / 2;
        int y = 30;
        if (posTitlesModule != null) {
            x += posTitlesModule.getHudOffsetX();
            y = posTitlesModule.getHudY();
        }
        return new Bounds(x, y, scaledWidth, (int) Math.ceil(textRenderer.fontHeight * scale));
    }

    private Bounds getAutoPetBounds() {
        int centerX = width / 2;
        int baseY = height / 2 + HudState.PET_NOTIFICATION.yOffset;
        if (autoPetModule != null) {
            centerX += autoPetModule.getHudOffsetX();
            baseY = (height / 2) + autoPetModule.getHudOffsetY();
        }

        float titleScale = autoPetModule != null ? (float) autoPetModule.getHudScale() : HudState.PET_NOTIFICATION.scale;
        float subScale = titleScale * 0.6f;
        int titleW = textRenderer.getWidth(AUTOPET_TITLE_PREVIEW);
        int subW = textRenderer.getWidth(AUTOPET_SUB_PREVIEW);

        int widthPx = (int) Math.ceil(Math.max(titleW * titleScale, subW * subScale));
        int titleH = (int) Math.ceil(textRenderer.fontHeight * titleScale);
        int subBottom = (int) Math.ceil(25 + (textRenderer.fontHeight * subScale));
        int heightPx = Math.max(titleH, subBottom);

        int left = centerX - (widthPx / 2);
        return new Bounds(left, baseY, widthPx, heightPx);
    }

    private void renderPosTitlesPreview(DrawContext context, Bounds bounds) {
        float scale = posTitlesModule != null ? (float) posTitlesModule.getHudScale() : 1.0f;
        float inv = 1.0f / scale;
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);
        int drawX = (int) (bounds.x * inv);
        int drawY = (int) (bounds.y * inv);
        context.drawTextWithShadow(textRenderer, POS_TITLES_PREVIEW, drawX, drawY, 0xFFFFFFFF);
        context.getMatrices().popMatrix();
    }

    private void renderAutoPetPreview(DrawContext context, Bounds bounds) {
        int centerX = bounds.x + (bounds.w / 2);
        int baseY = bounds.y;

        float titleScale = autoPetModule != null ? (float) autoPetModule.getHudScale() : HudState.PET_NOTIFICATION.scale;
        float subScale = titleScale * 0.6f;

        int titleW = textRenderer.getWidth(AUTOPET_TITLE_PREVIEW);
        int subW = textRenderer.getWidth(AUTOPET_SUB_PREVIEW);

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(titleScale, titleScale);
        float titleInv = 1.0f / titleScale;
        int drawTitleX = (int) ((centerX * titleInv) - (titleW / 2f));
        int drawTitleY = (int) (baseY * titleInv);
        context.drawTextWithShadow(textRenderer, AUTOPET_TITLE_PREVIEW, drawTitleX, drawTitleY, 0xFFFFAA00);
        context.getMatrices().popMatrix();

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(subScale, subScale);
        float subInv = 1.0f / subScale;
        int drawSubX = (int) ((centerX * subInv) - (subW / 2f));
        int drawSubY = (int) ((baseY + 25) * subInv);
        context.drawTextWithShadow(textRenderer, AUTOPET_SUB_PREVIEW, drawSubX, drawSubY, 0xFFFF5555);
        context.getMatrices().popMatrix();
    }

    private void renderComponentBox(DrawContext context, int mouseX, int mouseY, Bounds bounds, String name) {
        boolean hovered = bounds.contains(mouseX, mouseY);
        boolean active = dragging != null && dragging.label.equals(name);
        int fillColor = active ? 0x443E8EFF : hovered ? 0x333E8EFF : 0x22111111;
        int borderColor = active ? 0xFF7AA2FF : 0xFF7F7F7F;

        context.fill(bounds.x - 2, bounds.y - 2, bounds.x + bounds.w + 2, bounds.y + bounds.h + 2, fillColor);
        RenderUtils.drawBorder(context, bounds.x - 2, bounds.y - 2, bounds.w + 4, bounds.h + 4, borderColor);

        context.drawTextWithShadow(textRenderer, name, bounds.x, bounds.y - 12, 0xFFE0E0E0);
    }

    private enum ComponentType {
        POS_TITLES("Pos Titles"),
        AUTOPET_TITLE("AutoPet Title");

        private final String label;

        ComponentType(String label) {
            this.label = label;
        }
    }

    private static final class Bounds {
        private final int x;
        private final int y;
        private final int w;
        private final int h;

        private Bounds(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = Math.max(1, w);
            this.h = Math.max(1, h);
        }

        private boolean contains(int px, int py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }
    }
}
