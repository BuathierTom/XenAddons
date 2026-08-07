package xenus.xenaddons.features.impl.misc;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.render.music.MusicCaptureManager;
import xenus.xenaddons.render.music.MusicRenderPipelines;
import xenus.xenaddons.render.music.font.MusicMsdfFont;
import xenus.xenaddons.render.music.font.MusicMsdfFontManager;
import xenus.xenaddons.render.music.font.MusicMsdfTextState;
import xenus.xenaddons.render.music.state.MusicImageState;
import xenus.xenaddons.render.music.state.MusicLiquidGlassState;
import xenus.xenaddons.render.music.state.MusicRoundedRectState;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.settings.NumberSetting;
import xenus.xenaddons.utils.SpotifyCoverCache;
import xenus.xenaddons.utils.SpotifyNowPlaying;

public class SpotifyOverlayModule extends Module {
    public static final int BETTER_DISPLAY_WIDTH = 175;
    public static final int BETTER_DISPLAY_HEIGHT = 58;

    private static final String STANDARD_PREVIEW_LINE = "§6Spotify §7> §aAuthor §7- §bSong Title";
    private static final int BACKGROUND_COLOR = ARGB.color(100, 12, 12, 12);
    private static final int TITLE_COLOR = ARGB.color(255, 255, 255, 255);
    private static final int DETAIL_COLOR = ARGB.color(145, 255, 255, 255);
    private static final int PROGRESS_COLOR = ARGB.color(220, 255, 255, 255);
    private static final int INACTIVE_CONTROL_COLOR = ARGB.color(178, 255, 255, 255);
    private static final int ACTIVE_CONTROL_COLOR = ARGB.color(255, 29, 185, 84);
    private static final float CARD_RADIUS = 9.0f;
    private static final float BLUR_STRENGTH = 15.0f;
    private static final float BLOOM_STRENGTH = 3.0f;
    private static final float PADDING = 6.0f;
    private static final float COVER_SIZE = 32.0f;
    private static final float TEXT_X = PADDING + COVER_SIZE + 7.0f;
    private static final float TEXT_WIDTH = BETTER_DISPLAY_WIDTH - TEXT_X - PADDING;
    private static final float TITLE_SIZE = 9.0f;
    private static final float DETAIL_SIZE = 7.0f;
    private static final float PROGRESS_HEIGHT = 3.0f;

    private final BooleanSetting betterDisplay = new BooleanSetting("Better Display", false);
    private final BooleanSetting hideWhenNotPlaying = new BooleanSetting("Hide When Not Playing", false);
    private final NumberSetting hudOffsetX = new NumberSetting("HUD Offset X", 0, -1000, 1000, 1);
    private final NumberSetting hudY = new NumberSetting("HUD Y", 84, -1000, 1000, 1);
    private final NumberSetting hudScale = new NumberSetting("HUD Scale", 1.0, 0.5, 6.0, 0.05);

    private MusicMsdfFont titleFont;
    private MusicMsdfFont detailFont;
    private MusicMsdfFont controlFont;

    private boolean repeatActive;
    private boolean shuffleActive;
    private double animatedProgress;
    private long lastProgressFrame = System.nanoTime();
    private String lastProgressTrack = "";

    private boolean displayBoundsValid;
    private int displayLeft;
    private int displayTop;
    private float displayScale = 1.0f;

    public SpotifyOverlayModule() {
        super("Spotify Overlay", "Displays the current Spotify or compatible media track", Category.MISC);
        hideWhenNotPlaying.setVisibility(betterDisplay::getValue);
        hudOffsetX.setVisibility(() -> false);
        hudY.setVisibility(() -> false);
        hudScale.setVisibility(() -> false);
        registerSetting(betterDisplay);
        registerSetting(hideWhenNotPlaying);
        registerSetting(hudOffsetX);
        registerSetting(hudY);
        registerSetting(hudScale);
    }

    public void renderInGameHud(GuiGraphicsExtractor graphics) {
        displayBoundsValid = false;
        if (!shouldRenderInGameHud()) return;
        boolean useBetterDisplay = isBetterDisplayEnabled();
        SpotifyNowPlaying.requestRefreshIfNeeded(useBetterDisplay);

        if (useBetterDisplay) {
            SpotifyNowPlaying.Snapshot snapshot = SpotifyNowPlaying.getSnapshot();
            if (isHiddenWhenNotPlaying(snapshot)) return;
            renderBetterDisplay(graphics, snapshot);
            return;
        }

        String line = SpotifyNowPlaying.getFormattedLine();
        if (line == null || line.isBlank()) return;
        renderLine(graphics, line);
    }

    public void renderPreview(GuiGraphicsExtractor graphics) {
        int left = (mc.getWindow().getGuiScaledWidth() / 2) + hudOffsetX.getValue().intValue();
        renderPreviewAt(graphics, left, hudY.getValue().intValue());
    }

    public void renderPreviewAt(GuiGraphicsExtractor graphics, int left, int top) {
        float scale = hudScale.getValue().floatValue();
        if (!isBetterDisplayEnabled()) {
            renderLineAt(graphics, STANDARD_PREVIEW_LINE, left, top, scale);
            return;
        }

        SpotifyNowPlaying.Snapshot preview = new SpotifyNowPlaying.Snapshot(
            "Music",
            "kotlola, Haranczykov",
            "MEWA",
            "Playing",
            "",
            null,
            "",
            74,
            151
        );
        renderBetterDisplayAt(graphics, preview, left, top, scale, false);
    }

    private void renderLine(GuiGraphicsExtractor graphics, String line) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        float scale = hudScale.getValue().floatValue();
        int maxLeft = screenWidth - (int) Math.ceil(mc.font.width(line) * scale) - 2;
        int left = Mth.clamp(
            (screenWidth / 2) + hudOffsetX.getValue().intValue(),
            2,
            Math.max(2, maxLeft)
        );
        int maxTop = screenHeight - (int) Math.ceil(mc.font.lineHeight * scale) - 2;
        int top = Mth.clamp(hudY.getValue().intValue(), 2, Math.max(2, maxTop));
        renderLineAt(graphics, line, left, top, scale);
    }

    private void renderLineAt(GuiGraphicsExtractor graphics, String line, int left, int top, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(left, top);
        graphics.pose().scale(scale, scale);
        graphics.text(mc.font, line, 0, 0, 0xFFFFFFFF);
        graphics.pose().popMatrix();
    }

    private void renderBetterDisplay(GuiGraphicsExtractor graphics, SpotifyNowPlaying.Snapshot snapshot) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        float scale = hudScale.getValue().floatValue();
        int maxLeft = screenWidth - (int) Math.ceil(BETTER_DISPLAY_WIDTH * scale) - 2;
        int left = Mth.clamp(
            (screenWidth / 2) + hudOffsetX.getValue().intValue(),
            2,
            Math.max(2, maxLeft)
        );
        int maxTop = screenHeight - (int) Math.ceil(BETTER_DISPLAY_HEIGHT * scale) - 2;
        int top = Mth.clamp(hudY.getValue().intValue(), 2, Math.max(2, maxTop));

        displayBoundsValid = true;
        displayLeft = left;
        displayTop = top;
        displayScale = scale;
        renderBetterDisplayAt(graphics, snapshot, left, top, scale, true);
    }

    private void renderBetterDisplayAt(
        GuiGraphicsExtractor graphics,
        SpotifyNowPlaying.Snapshot snapshot,
        int left,
        int top,
        float scale,
        boolean interactive
    ) {
        loadFonts();

        Matrix3x2f pose = new Matrix3x2f(graphics.pose())
            .translate(left, top)
            .scale(scale, scale);
        ScreenRectangle parentScissor = graphics.scissorStack.peek();
        ScreenRectangle cardBounds = new ScreenRectangle(
            0,
            0,
            BETTER_DISPLAY_WIDTH,
            BETTER_DISPLAY_HEIGHT
        ).transformMaxBounds(pose);
        ScreenRectangle scissor = parentScissor != null ? parentScissor.intersection(cardBounds) : cardBounds;

        graphics.guiRenderState.addGuiElement(
            new MusicLiquidGlassState(
                MusicRenderPipelines.LIQUID_GLASS,
                MusicCaptureManager.textureSetup(),
                pose,
                0.0f,
                0.0f,
                BETTER_DISPLAY_WIDTH,
                BETTER_DISPLAY_HEIGHT,
                BACKGROUND_COLOR,
                CARD_RADIUS,
                BLUR_STRENGTH,
                BLOOM_STRENGTH,
                parentScissor
            )
        );

        drawCover(graphics, snapshot, pose, scissor);
        drawTrackInformation(graphics, snapshot, pose, scissor);
        drawProgress(graphics, snapshot, pose, scissor);
        drawControls(graphics, snapshot, pose, scissor, left, top, scale, interactive);
    }

    private void drawCover(
        GuiGraphicsExtractor graphics,
        SpotifyNowPlaying.Snapshot snapshot,
        Matrix3x2f pose,
        ScreenRectangle scissor
    ) {
        graphics.guiRenderState.addGuiElement(
            new MusicRoundedRectState(
                MusicRenderPipelines.ROUNDED_RECT,
                pose,
                PADDING,
                PADDING,
                COVER_SIZE,
                COVER_SIZE,
                ARGB.color(55, 255, 255, 255),
                6.0f,
                scissor
            )
        );

        Identifier coverId = SpotifyCoverCache.getOrLoad(snapshot);
        TextureSetup coverTexture = textureSetup(coverId);
        if (coverTexture == null) return;

        graphics.guiRenderState.addGuiElement(
            new MusicImageState(
                MusicRenderPipelines.IMAGE,
                coverTexture,
                pose,
                PADDING,
                PADDING,
                COVER_SIZE,
                COVER_SIZE,
                0xFFFFFFFF,
                6.0f,
                scissor
            )
        );
    }

    private void drawTrackInformation(
        GuiGraphicsExtractor graphics,
        SpotifyNowPlaying.Snapshot snapshot,
        Matrix3x2f pose,
        ScreenRectangle scissor
    ) {
        if (titleFont == null || detailFont == null) return;

        String title = trimToWidth(titleFont, betterTitle(snapshot), TEXT_WIDTH, TITLE_SIZE);
        graphics.guiRenderState.addGuiElement(
            new MusicMsdfTextState(titleFont, title, pose, TEXT_X, 7.0f, TITLE_SIZE, TITLE_COLOR, scissor)
        );

        String time = snapshot.hasProgress()
            ? formatTime(snapshot.progressSeconds()) + " / " + formatTime(snapshot.durationSeconds())
            : "";
        float timeWidth = time.isBlank() ? 0.0f : detailFont.width(time, DETAIL_SIZE);
        float artistWidth = time.isBlank() ? TEXT_WIDTH : Math.max(0.0f, TEXT_WIDTH - timeWidth - 8.0f);
        String artist = trimToWidth(detailFont, betterSubtitle(snapshot), artistWidth, DETAIL_SIZE);
        graphics.guiRenderState.addGuiElement(
            new MusicMsdfTextState(detailFont, artist, pose, TEXT_X, 18.0f, DETAIL_SIZE, DETAIL_COLOR, scissor)
        );

        if (!time.isBlank()) {
            graphics.guiRenderState.addGuiElement(
                new MusicMsdfTextState(
                    detailFont,
                    time,
                    pose,
                    TEXT_X + TEXT_WIDTH - timeWidth,
                    18.0f,
                    DETAIL_SIZE,
                    DETAIL_COLOR,
                    scissor
                )
            );
        }
    }

    private void drawProgress(
        GuiGraphicsExtractor graphics,
        SpotifyNowPlaying.Snapshot snapshot,
        Matrix3x2f pose,
        ScreenRectangle scissor
    ) {
        float progressY = 44.0f - PADDING - PROGRESS_HEIGHT - 5.0f;
        graphics.guiRenderState.addGuiElement(
            new MusicRoundedRectState(
                MusicRenderPipelines.ROUNDED_RECT,
                pose,
                TEXT_X,
                progressY,
                TEXT_WIDTH,
                PROGRESS_HEIGHT,
                ARGB.color(65, 255, 255, 255),
                1.5f,
                scissor
            )
        );

        double targetProgress = snapshot.hasProgress()
            ? Mth.clamp(snapshot.progressSeconds() / (double) snapshot.durationSeconds(), 0.0, 1.0)
            : 0.0;
        updateAnimatedProgress(snapshot, targetProgress);
        float filledWidth = Math.max(PROGRESS_HEIGHT, (float) (TEXT_WIDTH * animatedProgress));
        graphics.guiRenderState.addGuiElement(
            new MusicRoundedRectState(
                MusicRenderPipelines.ROUNDED_RECT,
                pose,
                TEXT_X,
                progressY,
                filledWidth,
                PROGRESS_HEIGHT,
                PROGRESS_COLOR,
                1.5f,
                scissor
            )
        );
    }

    private void drawControls(
        GuiGraphicsExtractor graphics,
        SpotifyNowPlaying.Snapshot snapshot,
        Matrix3x2f pose,
        ScreenRectangle scissor,
        int left,
        int top,
        float scale,
        boolean interactive
    ) {
        if (controlFont == null) return;
        double mouseX = mc.mouseHandler.getScaledXPos(mc.getWindow());
        double mouseY = mc.mouseHandler.getScaledYPos(mc.getWindow());
        float localMouseX = (float) ((mouseX - left) / scale);
        float localMouseY = (float) ((mouseY - top) / scale);
        boolean canHover = interactive && isInteractiveScreen(mc.screen);

        float middleX = (BETTER_DISPLAY_WIDTH / 2.0f) + 2.0f;
        addControlIcon(
            graphics,
            pose,
            scissor,
            "H",
            middleX - 17.5f,
            41.5f,
            16.0f,
            controlColor(canHover && hit(localMouseX, localMouseY, 68.0f, 82.0f))
        );
        addControlIcon(
            graphics,
            pose,
            scissor,
            snapshot.isPlaying() ? "A" : "B",
            middleX - 4.5f,
            41.5f,
            16.0f,
            controlColor(canHover && hit(localMouseX, localMouseY, 83.0f, 96.0f))
        );
        addControlIcon(
            graphics,
            pose,
            scissor,
            "G",
            middleX + 7.5f,
            41.5f,
            16.0f,
            controlColor(canHover && hit(localMouseX, localMouseY, 97.0f, 111.0f))
        );

        addControlIcon(
            graphics,
            pose,
            scissor,
            "I",
            BETTER_DISPLAY_WIDTH - PADDING - 29.0f,
            42.0f,
            14.0f,
            statefulControlColor(repeatActive, canHover && hit(localMouseX, localMouseY, 134.0f, 146.0f))
        );
        addControlIcon(
            graphics,
            pose,
            scissor,
            "C",
            BETTER_DISPLAY_WIDTH - PADDING - 18.0f,
            41.5f,
            16.0f,
            statefulControlColor(shuffleActive, canHover && hit(localMouseX, localMouseY, 147.0f, 159.0f))
        );
    }

    private void addControlIcon(
        GuiGraphicsExtractor graphics,
        Matrix3x2f pose,
        ScreenRectangle scissor,
        String glyph,
        float x,
        float y,
        float size,
        int color
    ) {
        graphics.guiRenderState.addGuiElement(
            new MusicMsdfTextState(controlFont, glyph, pose, x, y, size, color, scissor)
        );
    }

    private TextureSetup textureSetup(Identifier id) {
        if (id == null) return null;
        try {
            AbstractTexture texture = mc.getTextureManager().getTexture(id);
            if (texture == null || texture.getTextureView() == null || texture.getSampler() == null) return null;
            return TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler());
        } catch (Exception ignored) {
            return null;
        }
    }

    private void loadFonts() {
        if (titleFont == null) titleFont = MusicMsdfFontManager.get("inter-bold", TITLE_SIZE);
        if (detailFont == null) detailFont = MusicMsdfFontManager.get("inter-semibold", 9.0f);
        if (controlFont == null) controlFont = MusicMsdfFontManager.get("fluid-regular", 9.0f);
    }

    private void updateAnimatedProgress(SpotifyNowPlaying.Snapshot snapshot, double target) {
        String track = snapshot.artist() + '\u0000' + snapshot.title();
        long now = System.nanoTime();
        if (!track.equals(lastProgressTrack)) {
            lastProgressTrack = track;
            animatedProgress = target;
            lastProgressFrame = now;
            return;
        }

        double deltaSeconds = Math.min(0.1, Math.max(0.0, (now - lastProgressFrame) / 1_000_000_000.0));
        lastProgressFrame = now;
        double interpolation = 1.0 - Math.exp(-deltaSeconds / 0.08);
        animatedProgress += (target - animatedProgress) * interpolation;
    }

    private String trimToWidth(MusicMsdfFont font, String text, float width, float size) {
        if (text == null || text.isBlank()) return "";
        if (font.width(text, size) <= width) return text;

        String suffix = "...";
        float available = Math.max(0.0f, width - font.width(suffix, size));
        StringBuilder result = new StringBuilder();
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            if (font.width(result + character, size) > available) break;
            result.append(character);
            offset += Character.charCount(codePoint);
        }
        return result + suffix;
    }

    private String betterTitle(SpotifyNowPlaying.Snapshot snapshot) {
        if (snapshot.isPlaying()) return snapshot.title();
        if (snapshot.isPaused() && !snapshot.title().isBlank()) return snapshot.title();
        if (snapshot.isPaused()) return "Paused";
        return snapshot.source() + " not connected";
    }

    private String betterSubtitle(SpotifyNowPlaying.Snapshot snapshot) {
        if (snapshot.isPlaying()) return snapshot.artist();
        if (snapshot.isPaused() && !snapshot.artist().isBlank()) return snapshot.artist();
        if (snapshot.isPaused()) return snapshot.source();
        return snapshot.source() + " is not running";
    }

    private String formatTime(double seconds) {
        int safeSeconds = Math.max(0, (int) Math.floor(seconds));
        return (safeSeconds / 60) + ":" + String.format("%02d", safeSeconds % 60);
    }

    private int controlColor(boolean hovered) {
        return hovered ? TITLE_COLOR : INACTIVE_CONTROL_COLOR;
    }

    private int statefulControlColor(boolean active, boolean hovered) {
        if (active) return ACTIVE_CONTROL_COLOR;
        return controlColor(hovered);
    }

    private boolean hit(float mouseX, float mouseY, float minX, float maxX) {
        return mouseX >= minX && mouseX <= maxX && mouseY >= 40.0f && mouseY <= 58.0f;
    }

    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        if (!shouldRenderInGameHud()
            || !isInteractiveScreen(mc.screen)
            || !isBetterDisplayEnabled()
            || !displayBoundsValid
            || button != 0) {
            return false;
        }

        float localX = (float) ((mouseX - displayLeft) / displayScale);
        float localY = (float) ((mouseY - displayTop) / displayScale);
        if (localX < 0.0f || localX > BETTER_DISPLAY_WIDTH || localY < 40.0f || localY > 58.0f) {
            return false;
        }

        if (localX >= 68.0f && localX <= 82.0f) {
            SpotifyNowPlaying.control(SpotifyNowPlaying.ControlAction.PREVIOUS);
            return true;
        }
        if (localX >= 83.0f && localX <= 96.0f) {
            SpotifyNowPlaying.control(SpotifyNowPlaying.ControlAction.PLAY_PAUSE);
            return true;
        }
        if (localX >= 97.0f && localX <= 111.0f) {
            SpotifyNowPlaying.control(SpotifyNowPlaying.ControlAction.NEXT);
            return true;
        }
        if (localX >= 134.0f && localX <= 146.0f) {
            repeatActive = !repeatActive;
            SpotifyNowPlaying.setRepeat(repeatActive);
            return true;
        }
        if (localX >= 147.0f && localX <= 159.0f) {
            shuffleActive = !shuffleActive;
            SpotifyNowPlaying.setShuffle(shuffleActive);
            return true;
        }
        return true;
    }

    public boolean shouldRenderInGameHud() {
        if (!isEnabled()
            || mc.level == null
            || mc.player == null
            || mc.options.hideGui
            || mc.getOverlay() != null) {
            return false;
        }

        return !(mc.screen instanceof LevelLoadingScreen)
            && !(mc.screen instanceof ProgressScreen);
    }

    public static boolean isInteractiveScreen(Screen screen) {
        return screen instanceof ChatScreen || screen instanceof AbstractContainerScreen<?>;
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
        return betterDisplay.getValue() && MusicRenderPipelines.isAvailable();
    }

    public boolean shouldRenderBetterDisplay() {
        if (!shouldRenderInGameHud() || !isBetterDisplayEnabled()) return false;
        return !isHiddenWhenNotPlaying(SpotifyNowPlaying.getSnapshot());
    }

    private boolean isHiddenWhenNotPlaying(SpotifyNowPlaying.Snapshot snapshot) {
        return hideWhenNotPlaying.getValue() && !snapshot.isPlaying();
    }

    public int getPreviewWidth() {
        return isBetterDisplayEnabled() ? BETTER_DISPLAY_WIDTH : mc.font.width(STANDARD_PREVIEW_LINE);
    }

    public int getPreviewHeight() {
        return isBetterDisplayEnabled() ? BETTER_DISPLAY_HEIGHT : mc.font.lineHeight;
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
