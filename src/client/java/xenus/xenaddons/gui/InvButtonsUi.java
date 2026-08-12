package xenus.xenaddons.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import xenus.xenaddons.features.ModuleManager;

/**
 * Shared look and feel for the Inventory Buttons screens: rounded surfaces, section labels,
 * switches and buttons, so the config menu and the per-button editor read as one interface.
 */
public final class InvButtonsUi {
    public static final int PANEL = 0xF41A1A20;
    public static final int HEADER = 0xFF22222A;
    public static final int RAISED = 0xFF2B2B34;
    public static final int RAISED_HOVER = 0xFF383843;
    public static final int SUNKEN = 0xFF121217;
    public static final int BORDER = 0xFF3C3C47;
    public static final int BORDER_SOFT = 0xFF292931;
    public static final int TEXT = 0xFFEDEDF3;
    public static final int TEXT_MUTED = 0xFF9A9AA8;
    public static final int TEXT_DIM = 0xFF62626F;
    public static final int DANGER = 0xFFD9525C;
    public static final int SUCCESS = 0xFF5FCB7A;
    public static final int SCRIM = 0xBB0A0A0D;

    public static final int RADIUS = 4;
    public static final int TOGGLE_W = 24;
    public static final int TOGGLE_H = 12;

    public enum Tone { NEUTRAL, ACCENT, DANGER, GHOST }

    private InvButtonsUi() {}

    public static int accent() {
        return 0xFF000000 | (ModuleManager.colors.getColor() & 0x00FFFFFF);
    }

    public static int alpha(int color, int a) {
        return ((a & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    public static int mix(int from, int to, float t) {
        float f = Math.max(0.0f, Math.min(1.0f, t));
        int a = channel(from, 24) + Math.round((channel(to, 24) - channel(from, 24)) * f);
        int r = channel(from, 16) + Math.round((channel(to, 16) - channel(from, 16)) * f);
        int g = channel(from, 8) + Math.round((channel(to, 8) - channel(from, 8)) * f);
        int b = channel(from, 0) + Math.round((channel(to, 0) - channel(from, 0)) * f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int channel(int color, int shift) {
        return (color >>> shift) & 0xFF;
    }

    public static boolean hovered(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    /** Filled rectangle with rounded corners, approximated per scanline. */
    public static void rect(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;

        int r = Math.min(radius, Math.min(w, h) / 2);
        if (r <= 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }

        ctx.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(Math.max(0.0, (r * r) - (dy * dy))));
            ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            ctx.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
    }

    /** One pixel rounded outline drawn on top of {@code inner}, which fills the shape. */
    public static void outline(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int radius, int color, int inner) {
        rect(ctx, x, y, w, h, radius, color);
        rect(ctx, x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1), inner);
    }

    public static void panel(GuiGraphicsExtractor ctx, int x, int y, int w, int h) {
        rect(ctx, x + 1, y + 2, w, h, RADIUS + 1, 0x40000000);
        outline(ctx, x, y, w, h, RADIUS, BORDER, PANEL);
    }

    /** Panel title bar: darker band, title on the left, accent underline. */
    public static void header(GuiGraphicsExtractor ctx, Font font, int x, int y, int w, int h, String title) {
        rect(ctx, x + 1, y + 1, w - 2, h, RADIUS - 1, HEADER);
        ctx.fill(x + 1, y + h, x + w - 1, y + h + 1, BORDER_SOFT);
        ctx.fill(x + 1, y + h, x + 1 + Math.min(w - 2, 34), y + h + 1, accent());
        ctx.text(font, title, x + 10, y + (h - font.lineHeight) / 2 + 1, TEXT, false);
    }

    /** Small muted caption introducing a group of controls. */
    public static void label(GuiGraphicsExtractor ctx, Font font, String text, int x, int y) {
        ctx.text(font, text, x, y, TEXT_MUTED, false);
    }

    public static void divider(GuiGraphicsExtractor ctx, int x, int y, int w) {
        ctx.fill(x, y, x + w, y + 1, BORDER_SOFT);
    }

    public static void button(GuiGraphicsExtractor ctx, Font font, int x, int y, int w, int h, String text, boolean hover, Tone tone) {
        int fill = switch (tone) {
            case ACCENT -> hover ? mix(accent(), 0xFFFFFFFF, 0.2f) : accent();
            case DANGER -> hover ? mix(DANGER, 0xFFFFFFFF, 0.18f) : alpha(DANGER, 0xCC);
            case GHOST -> hover ? RAISED : 0x00000000;
            case NEUTRAL -> hover ? RAISED_HOVER : RAISED;
        };
        int border = switch (tone) {
            case ACCENT -> mix(accent(), 0xFFFFFFFF, 0.25f);
            case DANGER -> DANGER;
            case GHOST -> hover ? BORDER : BORDER_SOFT;
            case NEUTRAL -> hover ? BORDER : BORDER_SOFT;
        };

        outline(ctx, x, y, w, h, RADIUS - 1, border, fill);
        ctx.text(font, text, x + (w - font.width(text)) / 2, y + (h - font.lineHeight) / 2 + 1, TEXT, false);
    }

    /** Pill switch. Reads as on/off at a glance without needing an ON/OFF caption. */
    public static void toggle(GuiGraphicsExtractor ctx, int x, int y, boolean on, boolean hover) {
        int track = on ? accent() : (hover ? RAISED_HOVER : RAISED);
        int border = on ? mix(accent(), 0xFFFFFFFF, 0.3f) : (hover ? BORDER : BORDER_SOFT);
        outline(ctx, x, y, TOGGLE_W, TOGGLE_H, TOGGLE_H / 2, border, track);

        int knobSize = TOGGLE_H - 4;
        int knobX = on ? (x + TOGGLE_W - knobSize - 2) : (x + 2);
        rect(ctx, knobX, y + 2, knobSize, knobSize, knobSize / 2, on ? 0xFFFFFFFF : 0xFFB4B4C0);
    }

    /** Sunken frame drawn behind an unbordered EditBox. */
    public static void field(GuiGraphicsExtractor ctx, int x, int y, int w, int h, boolean focused) {
        outline(ctx, x, y, w, h, RADIUS - 1, focused ? accent() : BORDER_SOFT, SUNKEN);
    }

    public static void scrollbar(GuiGraphicsExtractor ctx, int x, int y, int trackH, int contentH, int viewH, int scroll) {
        if (contentH <= viewH || trackH <= 0) return;

        int thumbH = Math.max(12, Math.round(trackH * (viewH / (float) contentH)));
        float progress = Math.min(1.0f, Math.max(0.0f, scroll / (float) (contentH - viewH)));
        int thumbY = y + Math.round((trackH - thumbH) * progress);

        rect(ctx, x, y, 3, trackH, 1, alpha(0xFFFFFFFF, 0x14));
        rect(ctx, x, thumbY, 3, thumbH, 1, alpha(accent(), 0xCC));
    }

    /** Selection ring drawn around an icon or swatch. */
    public static void selection(GuiGraphicsExtractor ctx, int x, int y, int w, int h) {
        int color = accent();
        ctx.fill(x - 1, y - 1, x + w + 1, y, color);
        ctx.fill(x - 1, y + h, x + w + 1, y + h + 1, color);
        ctx.fill(x - 1, y, x, y + h, color);
        ctx.fill(x + w, y, x + w + 1, y + h, color);
    }
}
