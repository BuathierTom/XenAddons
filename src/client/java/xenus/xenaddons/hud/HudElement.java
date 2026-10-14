package xenus.xenaddons.hud;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class HudElement {
    public static final float CENTER = Float.NaN;
    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 6.0f;

    @FunctionalInterface
    public interface Renderer {
        Size render(GuiGraphicsExtractor context, boolean example);
    }

    public record Size(float width, float height) {
        public static final Size NONE = new Size(0.0f, 0.0f);
    }

    private final String name;
    private final Renderer renderer;
    private BooleanSupplier enabled;
    private BooleanSupplier visible = () -> true;
    private boolean centered;
    private float defaultX = 20.0f;
    private float defaultY = 20.0f;
    private float defaultScale = 1.0f;
    private float x = defaultX;
    private float y = defaultY;
    private float scale = defaultScale;
    private float width;
    private float height;

    public HudElement(String name, BooleanSupplier enabled, Renderer renderer) {
        this.name = name;
        this.enabled = enabled;
        this.renderer = renderer;
    }

    public HudElement enabledWhen(BooleanSupplier enabled) {
        this.enabled = enabled;
        return this;
    }

    public HudElement visibleWhen(BooleanSupplier visible) {
        this.visible = visible;
        return this;
    }

    public HudElement centered() {
        this.centered = true;
        return this;
    }

    public HudElement defaults(float x, float y, float scale) {
        this.defaultX = x;
        this.defaultY = y;
        this.defaultScale = scale;
        reset();
        return this;
    }

    public String name() {
        return name;
    }

    public boolean isEnabled() {
        return enabled.getAsBoolean();
    }

    public boolean shouldDraw() {
        return visible.getAsBoolean();
    }

    public boolean isCentered() {
        return centered;
    }

    public float x() {
        return Float.isNaN(x) ? HudResolution.width() / 2.0f : x;
    }

    public float y() {
        return Float.isNaN(y) ? HudResolution.height() / 2.0f : y;
    }

    public float scale() {
        return scale;
    }

    public float scaledWidth() {
        return width * scale;
    }

    public float scaledHeight() {
        return height * scale;
    }

    public float left() {
        return centered ? x() - scaledWidth() / 2.0f : x();
    }

    public float top() {
        return y();
    }

    public boolean hasStoredX() {
        return !Float.isNaN(x);
    }

    public boolean hasStoredY() {
        return !Float.isNaN(y);
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setScale(float scale) {
        this.scale = Math.clamp(scale, MIN_SCALE, MAX_SCALE);
    }

    public void reset() {
        x = defaultX;
        y = defaultY;
        scale = defaultScale;
    }

    public boolean contains(float mouseX, float mouseY) {
        float left = left();
        float top = top();
        return mouseX >= left && mouseX <= left + Math.max(1.0f, scaledWidth())
            && mouseY >= top && mouseY <= top + Math.max(1.0f, scaledHeight());
    }

    public void render(GuiGraphicsExtractor context, boolean example) {
        context.pose().pushMatrix();
        context.pose().translate(x(), y());
        context.pose().scale(scale, scale);
        Size size = renderer.render(context, example);
        context.pose().popMatrix();
        width = size.width();
        height = size.height();
    }
}
