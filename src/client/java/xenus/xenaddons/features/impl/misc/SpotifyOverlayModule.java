package xenus.xenaddons.features.impl.misc;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import xenus.xenaddons.compat.XenDrawContextCompat;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.settings.NumberSetting;
import xenus.xenaddons.utils.SpotifyCoverCache;
import xenus.xenaddons.utils.SpotifyNowPlaying;
import xenus.xenaddons.utils.SvgIconTextures;

public class SpotifyOverlayModule extends Module {
    public static final int BETTER_DISPLAY_WIDTH = 150;
    public static final int BETTER_DISPLAY_HEIGHT = 52;

    private static final String STANDARD_PREVIEW_LINE = "§6Spotify §7> §aAuthor §7- §bSong Title";
    private static final int BETTER_BACKGROUND = 0x96000000;
    private static final int SPOTIFY_GREEN = 0xFF1DB954;
    private static final int PROGRESS_BACKGROUND = 0x665F6368;
    private static final int CONTROL_BORDER = 0x55FFFFFF;

    private static final int COVER_SIZE = BETTER_DISPLAY_HEIGHT;
    private static final int BOX_PADDING = 3;
    private static final int CONTENT_X = COVER_SIZE + BOX_PADDING;
    private static final int CONTENT_WIDTH = BETTER_DISPLAY_WIDTH - CONTENT_X - 4;
    private static final int TITLE_Y = 3;
    private static final int ARTIST_Y = 13;
    private static final int TIMELINE_Y = 26;
    private static final int TIME_Y = 29;
    private static final int CONTROL_SIZE = 11;
    private static final int CONTROL_ICON_SIZE = 7;
    private static final int CONTROL_GAP = 4;
    private static final int CONTROLS_Y = 39;
    private static final int CONTROLS_WIDTH = (CONTROL_SIZE * 3) + (CONTROL_GAP * 2);
    private static final int CONTROLS_X = CONTENT_X + ((CONTENT_WIDTH - CONTROLS_WIDTH) / 2);
    private static final int PREVIOUS_X = CONTROLS_X;
    private static final int PLAY_X = PREVIOUS_X + CONTROL_SIZE + CONTROL_GAP;
    private static final int NEXT_X = PLAY_X + CONTROL_SIZE + CONTROL_GAP;
    private static final float TITLE_SCALE = 0.75f;
    private static final float ARTIST_SCALE = 0.55f;
    private static final float TIME_SCALE = 0.5f;
    private static final String PREVIOUS_ICON = "/assets/xenaddons/textures/spotify/previous.svg";
    private static final String PLAY_ICON = "/assets/xenaddons/textures/spotify/play.svg";
    private static final String PAUSE_ICON = "/assets/xenaddons/textures/spotify/pause.svg";
    private static final String NEXT_ICON = "/assets/xenaddons/textures/spotify/next.svg";

    private final BooleanSetting betterDisplay = new BooleanSetting("Better Display", false);
    private final NumberSetting hudOffsetX = new NumberSetting("HUD Offset X", 0, -1000, 1000, 1);
    private final NumberSetting hudY = new NumberSetting("HUD Y", 84, -1000, 1000, 1);
    private final NumberSetting hudScale = new NumberSetting("HUD Scale", 1.0, 0.5, 6.0, 0.05);

    private final ButtonBounds previousButtonBounds = new ButtonBounds();
    private final ButtonBounds playPauseButtonBounds = new ButtonBounds();
    private final ButtonBounds nextButtonBounds = new ButtonBounds();

    public SpotifyOverlayModule() {
        super("Spotify Overlay", "Displays currently playing Spotify track", Category.MISC);
        hudOffsetX.setVisibility(() -> false);
        hudY.setVisibility(() -> false);
        hudScale.setVisibility(() -> false);
        this.registerSetting(betterDisplay);
        this.registerSetting(hudOffsetX);
        this.registerSetting(hudY);
        this.registerSetting(hudScale);
    }

    public void renderInGameHud(DrawContext drawContext) {
        if (!this.isEnabled()) return;
        SpotifyNowPlaying.requestRefreshIfNeeded(betterDisplay.getValue());

        if (betterDisplay.getValue()) {
            renderBetterDisplay(drawContext, SpotifyNowPlaying.getSnapshot());
            return;
        }

        String line = SpotifyNowPlaying.getFormattedLine();
        if (line == null || line.isBlank()) return;
        renderLine(drawContext, line);
    }

    public void renderPreview(DrawContext drawContext) {
        int leftX = (mc.getWindow().getScaledWidth() / 2) + hudOffsetX.getValue().intValue();
        int y = hudY.getValue().intValue();
        renderPreviewAt(drawContext, leftX, y);
    }

    public void renderPreviewAt(DrawContext drawContext, int leftX, int y) {
        float scale = (float) hudScale.getValue().doubleValue();
        if (betterDisplay.getValue()) {
            SpotifyNowPlaying.Snapshot preview = new SpotifyNowPlaying.Snapshot(
                "Author",
                "Song Title",
                "Playing",
                "",
                "",
                72,
                205
            );
            renderBetterDisplayAt(drawContext, preview, leftX, y, scale);
        } else {
            renderLineAt(drawContext, STANDARD_PREVIEW_LINE, leftX, y, scale);
        }
    }

    private void renderLine(DrawContext drawContext, String line) {
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();
        int textWidth = mc.textRenderer.getWidth(line);
        int textHeight = mc.textRenderer.fontHeight;
        float scale = (float) hudScale.getValue().doubleValue();
        int leftXRaw = (width / 2) + hudOffsetX.getValue().intValue();
        int yRaw = hudY.getValue().intValue();
        int maxLeftX = width - (int) Math.ceil(textWidth * scale) - 2;
        int leftX = MathHelper.clamp(leftXRaw, 2, Math.max(2, maxLeftX));
        int maxY = height - (int) Math.ceil(textHeight * scale) - 2;
        int y = MathHelper.clamp(yRaw, 2, Math.max(2, maxY));

        renderLineAt(drawContext, line, leftX, y, scale);
    }

    private void renderLineAt(DrawContext drawContext, String line, int leftX, int y, float scale) {
        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().scale(scale, scale);
        float inv = 1.0f / scale;
        int x = (int) (leftX * inv);
        int drawY = (int) (y * inv);
        drawContext.drawTextWithShadow(mc.textRenderer, line, x, drawY, 0xFFFFFFFF);
        drawContext.getMatrices().popMatrix();
    }

    private void renderBetterDisplay(DrawContext drawContext, SpotifyNowPlaying.Snapshot snapshot) {
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();
        float scale = (float) hudScale.getValue().doubleValue();

        int leftXRaw = (width / 2) + hudOffsetX.getValue().intValue();
        int yRaw = hudY.getValue().intValue();
        int maxLeftX = width - (int) Math.ceil(BETTER_DISPLAY_WIDTH * scale) - 2;
        int leftX = MathHelper.clamp(leftXRaw, 2, Math.max(2, maxLeftX));
        int maxY = height - (int) Math.ceil(BETTER_DISPLAY_HEIGHT * scale) - 2;
        int y = MathHelper.clamp(yRaw, 2, Math.max(2, maxY));

        updateControlBounds(leftX, y, scale);
        renderBetterDisplayAt(drawContext, snapshot, leftX, y, scale);
    }

    private void renderBetterDisplayAt(
        DrawContext drawContext,
        SpotifyNowPlaying.Snapshot snapshot,
        int leftX,
        int y,
        float scale
    ) {
        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().scale(scale, scale);
        float inv = 1.0f / scale;
        int x = (int) (leftX * inv);
        int drawY = (int) (y * inv);

        drawContext.fill(x, drawY, x + BETTER_DISPLAY_WIDTH, drawY + BETTER_DISPLAY_HEIGHT, BETTER_BACKGROUND);
        drawCover(drawContext, snapshot, x, drawY, COVER_SIZE);

        String title = betterTitle(snapshot);
        String subtitle = betterSubtitle(snapshot);
        int titleColor = snapshot.isPlaying() ? 0xFFFFFFFF : 0xFFD4D7D8;
        int subtitleColor = snapshot.isPlaying() ? 0xFFE0E0E0 : 0xFFB8BFC4;

        int contentX = x + CONTENT_X;
        drawScaledText(drawContext, fitText(title, CONTENT_WIDTH, TITLE_SCALE), contentX, drawY + TITLE_Y, TITLE_SCALE, titleColor);
        drawScaledText(drawContext, fitText(subtitle, CONTENT_WIDTH, ARTIST_SCALE), contentX, drawY + ARTIST_Y, ARTIST_SCALE, subtitleColor);

        if (snapshot.isPlaying() || snapshot.isPaused()) {
            drawProgress(drawContext, snapshot, contentX, drawY + TIMELINE_Y, CONTENT_WIDTH, 1);
            drawTimeLabels(drawContext, snapshot, contentX, drawY + TIME_Y, CONTENT_WIDTH);
        }
        drawControls(drawContext, snapshot, x, drawY);

        drawContext.getMatrices().popMatrix();
    }

    private void drawCover(DrawContext drawContext, SpotifyNowPlaying.Snapshot snapshot, int x, int y, int size) {
        drawContext.fill(x, y, x + size, y + size, 0xFF151A1F);
        Identifier cover = SpotifyCoverCache.getOrLoad(snapshot);
        if (cover != null) {
            XenDrawContextCompat.drawTexture(
                drawContext,
                cover,
                x,
                y,
                0.0f,
                0.0f,
                size,
                size,
                SpotifyCoverCache.COVER_TEXTURE_SIZE,
                SpotifyCoverCache.COVER_TEXTURE_SIZE,
                SpotifyCoverCache.COVER_TEXTURE_SIZE,
                SpotifyCoverCache.COVER_TEXTURE_SIZE
            );
        } else {
            drawAlbumPlaceholder(drawContext, snapshot, x, y, size);
        }
    }

    private void drawAlbumPlaceholder(DrawContext drawContext, SpotifyNowPlaying.Snapshot snapshot, int x, int y, int size) {
        int baseColor = snapshot.isPlaying() ? 0xFF132A20 : snapshot.isPaused() ? 0xFF252A30 : 0xFF151A1F;
        drawContext.fill(x, y, x + size, y + size, baseColor);

        int barWidth = Math.max(4, size / 8);
        int gap = Math.max(3, size / 12);
        int bottom = y + size - Math.max(7, size / 7);
        int startX = x + Math.max(8, size / 6);
        int[] heights = {size / 5, size / 3, size / 2, size / 4};
        for (int i = 0; i < heights.length; i++) {
            int barX = startX + (i * (barWidth + gap));
            drawRoundedRect(drawContext, barX, bottom - heights[i], barWidth, heights[i], 1, 0xCCFFFFFF);
        }
    }

    private void drawTimeLabels(DrawContext drawContext, SpotifyNowPlaying.Snapshot snapshot, int x, int y, int width) {
        if (!snapshot.hasProgress()) return;

        String start = formatTime(snapshot.progressSeconds());
        String end = formatTime(snapshot.durationSeconds());
        drawScaledText(drawContext, start, x, y, TIME_SCALE, 0xFFEAEAEA);
        drawScaledText(drawContext, end, x + width - scaledTextWidth(end, TIME_SCALE), y, TIME_SCALE, 0xFFEAEAEA);
    }

    private void drawProgress(DrawContext drawContext, SpotifyNowPlaying.Snapshot snapshot, int x, int y, int width, int height) {
        drawRoundedRect(drawContext, x, y, width, height, 0, PROGRESS_BACKGROUND);
        if (snapshot.isPaused()) {
            int filled = snapshot.hasProgress()
                ? MathHelper.clamp((int) Math.round(width * (snapshot.progressSeconds() / (double) snapshot.durationSeconds())), 1, width)
                : width / 3;
            drawRoundedRect(drawContext, x, y, filled, height, 0, SPOTIFY_GREEN);
            return;
        }
        if (!snapshot.isPlaying()) return;

        if (snapshot.hasProgress()) {
            double raw = snapshot.progressSeconds() / (double) snapshot.durationSeconds();
            int filled = MathHelper.clamp((int) Math.round(width * raw), 1, width);
            drawRoundedRect(drawContext, x, y, filled, height, 0, SPOTIFY_GREEN);
            return;
        }

        long phase = (System.currentTimeMillis() / 45L) % width;
        int segmentWidth = Math.max(18, width / 4);
        int start = x + (int) phase - segmentWidth;
        int end = start + segmentWidth;
        int clippedStart = Math.max(x, start);
        int clippedEnd = Math.min(x + width, end);
        if (clippedEnd > clippedStart) {
            drawRoundedRect(drawContext, clippedStart, y, clippedEnd - clippedStart, height, 0, SPOTIFY_GREEN);
        }
    }

    private void drawControls(DrawContext drawContext, SpotifyNowPlaying.Snapshot snapshot, int x, int y) {
        drawControlButton(drawContext, x + PREVIOUS_X, y + CONTROLS_Y, ControlButton.PREVIOUS, snapshot);
        drawControlButton(drawContext, x + PLAY_X, y + CONTROLS_Y, ControlButton.PLAY_PAUSE, snapshot);
        drawControlButton(drawContext, x + NEXT_X, y + CONTROLS_Y, ControlButton.NEXT, snapshot);
    }

    private void drawControlButton(
        DrawContext drawContext,
        int x,
        int y,
        ControlButton button,
        SpotifyNowPlaying.Snapshot snapshot
    ) {
        boolean hovered = isHovered(button);
        drawRoundedRect(drawContext, x, y, CONTROL_SIZE, CONTROL_SIZE, 0, hovered ? 0x661DB954 : 0x16FFFFFF);
        drawRoundedBorder(drawContext, x, y, CONTROL_SIZE, CONTROL_SIZE, 0, CONTROL_BORDER);

        int iconColor = hovered ? SPOTIFY_GREEN : 0xFFFFFFFF;
        String icon = switch (button) {
            case PREVIOUS -> PREVIOUS_ICON;
            case PLAY_PAUSE -> snapshot.isPlaying() ? PAUSE_ICON : PLAY_ICON;
            case NEXT -> NEXT_ICON;
        };
        Identifier iconTexture = SvgIconTextures.get(icon, CONTROL_ICON_SIZE, iconColor);
        if (iconTexture != null) {
            int iconX = x + ((CONTROL_SIZE - CONTROL_ICON_SIZE) / 2);
            int iconY = y + ((CONTROL_SIZE - CONTROL_ICON_SIZE) / 2);
            XenDrawContextCompat.drawTexture(
                drawContext,
                iconTexture,
                iconX,
                iconY,
                0.0f,
                0.0f,
                CONTROL_ICON_SIZE,
                CONTROL_ICON_SIZE,
                CONTROL_ICON_SIZE,
                CONTROL_ICON_SIZE
            );
            return;
        }

        switch (button) {
            case PREVIOUS -> drawPreviousIcon(drawContext, x, y, iconColor);
            case PLAY_PAUSE -> {
                if (snapshot.isPlaying()) drawPauseIcon(drawContext, x, y, iconColor);
                else drawPlayIcon(drawContext, x, y, iconColor);
            }
            case NEXT -> drawNextIcon(drawContext, x, y, iconColor);
        }
    }

    private void drawPreviousIcon(DrawContext context, int x, int y, int color) {
        context.fill(x + 3, y + 3, x + 5, y + 9, color);
        for (int row = 0; row < 6; row++) {
            int width = row < 3 ? row + 1 : 6 - row;
            context.fill(x + 9 - width, y + 3 + row, x + 9, y + 4 + row, color);
        }
    }

    private void drawNextIcon(DrawContext context, int x, int y, int color) {
        context.fill(x + 8, y + 3, x + 10, y + 9, color);
        for (int row = 0; row < 6; row++) {
            int width = row < 3 ? row + 1 : 6 - row;
            context.fill(x + 3, y + 3 + row, x + 3 + width, y + 4 + row, color);
        }
    }

    private void drawPlayIcon(DrawContext context, int x, int y, int color) {
        for (int row = 0; row < 7; row++) {
            int width = row < 4 ? row + 1 : 7 - row;
            context.fill(x + 4, y + 3 + row, x + 4 + width, y + 4 + row, color);
        }
    }

    private void drawPauseIcon(DrawContext context, int x, int y, int color) {
        context.fill(x + 4, y + 3, x + 6, y + 10, color);
        context.fill(x + 8, y + 3, x + 10, y + 10, color);
    }

    private String betterTitle(SpotifyNowPlaying.Snapshot snapshot) {
        if (snapshot.isPlaying()) return snapshot.title();
        if (snapshot.isPaused() && !snapshot.title().isBlank()) return snapshot.title();
        if (snapshot.isPaused()) return "Paused";
        return "Not open";
    }

    private String betterSubtitle(SpotifyNowPlaying.Snapshot snapshot) {
        if (snapshot.isPlaying()) return snapshot.artist();
        if (snapshot.isPaused() && !snapshot.artist().isBlank()) return snapshot.artist();
        if (snapshot.isPaused()) return "Spotify";
        return "Spotify is not running";
    }

    private void drawScaledText(DrawContext context, String text, float x, float y, float scale, int color) {
        if (text == null || text.isBlank()) return;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scale, scale);
        context.drawText(mc.textRenderer, text, 0, 0, color, false);
        context.getMatrices().popMatrix();
    }

    private int scaledTextWidth(String text, float scale) {
        return (int) Math.ceil(mc.textRenderer.getWidth(text) * scale);
    }

    private String fitText(String value, int maxWidth, float scale) {
        if (value == null || value.isBlank()) return "";
        String clean = value.trim();
        float safeScale = Math.max(0.1f, scale);
        if (scaledTextWidth(clean, safeScale) <= maxWidth) return clean;

        String ellipsis = "...";
        int maxUnscaledWidth = Math.max(0, (int) Math.floor(maxWidth / safeScale));
        int ellipsisWidth = mc.textRenderer.getWidth(ellipsis);
        int targetWidth = Math.max(0, maxUnscaledWidth - ellipsisWidth);
        StringBuilder out = new StringBuilder();
        for (int offset = 0; offset < clean.length();) {
            int codePoint = clean.codePointAt(offset);
            String next = new String(Character.toChars(codePoint));
            if (mc.textRenderer.getWidth(out.toString() + next) > targetWidth) break;
            out.append(next);
            offset += Character.charCount(codePoint);
        }
        return out + ellipsis;
    }

    private String formatTime(int seconds) {
        int safeSeconds = Math.max(0, seconds);
        return (safeSeconds / 60) + ":" + String.format("%02d", safeSeconds % 60);
    }

    private void drawRoundedRect(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        if (width <= 0 || height <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        if (r <= 1) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }

        context.fill(x + r, y, x + width - r, y + height, color);
        context.fill(x, y + r, x + width, y + height - r, color);
        for (int i = 0; i < r; i++) {
            int inset = r - i;
            context.fill(x + inset, y + i, x + width - inset, y + i + 1, color);
            context.fill(x + inset, y + height - i - 1, x + width - inset, y + height - i, color);
        }
    }

    private void drawRoundedBorder(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        drawRoundedRect(context, x, y, width, 1, 0, color);
        drawRoundedRect(context, x, y + height - 1, width, 1, 0, color);
        drawRoundedRect(context, x, y, 1, height, 0, color);
        drawRoundedRect(context, x + width - 1, y, 1, height, 0, color);

        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        for (int i = 1; i < r; i++) {
            int inset = r - i;
            context.fill(x + inset, y + i, x + inset + 1, y + i + 1, color);
            context.fill(x + width - inset - 1, y + i, x + width - inset, y + i + 1, color);
            context.fill(x + inset, y + height - i - 1, x + inset + 1, y + height - i, color);
            context.fill(x + width - inset - 1, y + height - i - 1, x + width - inset, y + height - i, color);
        }
    }

    public int getHudOffsetX() {
        return hudOffsetX.getValue().intValue();
    }

    public int getHudY() {
        return hudY.getValue().intValue();
    }

    public double getHudScale() {
        return hudScale.getValue();
    }

    public boolean isBetterDisplayEnabled() {
        return betterDisplay.getValue();
    }

    public int getPreviewWidth() {
        return betterDisplay.getValue() ? BETTER_DISPLAY_WIDTH : mc.textRenderer.getWidth(STANDARD_PREVIEW_LINE);
    }

    public int getPreviewHeight() {
        return betterDisplay.getValue() ? BETTER_DISPLAY_HEIGHT : mc.textRenderer.fontHeight;
    }

    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isEnabled() || !betterDisplay.getValue() || button != 0) return false;
        if (previousButtonBounds.contains(mouseX, mouseY)) {
            SpotifyNowPlaying.control(SpotifyNowPlaying.ControlAction.PREVIOUS);
            return true;
        }
        if (playPauseButtonBounds.contains(mouseX, mouseY)) {
            SpotifyNowPlaying.control(SpotifyNowPlaying.ControlAction.PLAY_PAUSE);
            return true;
        }
        if (nextButtonBounds.contains(mouseX, mouseY)) {
            SpotifyNowPlaying.control(SpotifyNowPlaying.ControlAction.NEXT);
            return true;
        }
        return false;
    }

    private void updateControlBounds(int leftX, int y, float scale) {
        previousButtonBounds.set(leftX + (PREVIOUS_X * scale), y + (CONTROLS_Y * scale), CONTROL_SIZE * scale, CONTROL_SIZE * scale);
        playPauseButtonBounds.set(leftX + (PLAY_X * scale), y + (CONTROLS_Y * scale), CONTROL_SIZE * scale, CONTROL_SIZE * scale);
        nextButtonBounds.set(leftX + (NEXT_X * scale), y + (CONTROLS_Y * scale), CONTROL_SIZE * scale, CONTROL_SIZE * scale);
    }

    private boolean isHovered(ControlButton button) {
        if (mc.currentScreen == null) return false;
        double mouseX = mc.mouse.getScaledX(mc.getWindow());
        double mouseY = mc.mouse.getScaledY(mc.getWindow());
        return switch (button) {
            case PREVIOUS -> previousButtonBounds.contains(mouseX, mouseY);
            case PLAY_PAUSE -> playPauseButtonBounds.contains(mouseX, mouseY);
            case NEXT -> nextButtonBounds.contains(mouseX, mouseY);
        };
    }

    private enum ControlButton {
        PREVIOUS,
        PLAY_PAUSE,
        NEXT
    }

    private static final class ButtonBounds {
        private double x;
        private double y;
        private double width;
        private double height;

        private void set(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private boolean contains(double px, double py) {
            return width > 0 && height > 0 && px >= x && px <= x + width && py >= y && py <= y + height;
        }
    }

    public void setHudOffsetX(int value) {
        hudOffsetX.setValue((double) value);
    }

    public void setHudY(int value) {
        hudY.setValue((double) value);
    }

    public void setHudScale(double value) {
        hudScale.setValue(value);
    }
}
