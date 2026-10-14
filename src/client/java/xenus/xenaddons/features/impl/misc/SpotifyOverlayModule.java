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
import org.joml.Vector2f;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.hud.HudElement;
import xenus.xenaddons.hud.HudRender;
import xenus.xenaddons.render.music.MusicCaptureManager;
import xenus.xenaddons.render.music.MusicRenderPipelines;
import xenus.xenaddons.render.music.font.MusicMsdfFont;
import xenus.xenaddons.render.music.font.MusicMsdfFontManager;
import xenus.xenaddons.render.music.font.MusicMsdfTextState;
import xenus.xenaddons.render.music.state.MusicImageState;
import xenus.xenaddons.render.music.state.MusicLiquidGlassState;
import xenus.xenaddons.render.music.state.MusicRoundedRectState;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.utils.SpotifyCoverCache;
import xenus.xenaddons.utils.SpotifyNowPlaying;

public class SpotifyOverlayModule extends Module {
    public static final int BETTER_DISPLAY_WIDTH = 175;
    public static final int BETTER_DISPLAY_HEIGHT = 58;

    private static final String STANDARD_PREVIEW_LINE = "§6Spotify §7> §aAuthor §7- §bSong Title";
    private static final HudElement.Size BETTER_DISPLAY_SIZE = new HudElement.Size(BETTER_DISPLAY_WIDTH, BETTER_DISPLAY_HEIGHT);
    private static final SpotifyNowPlaying.Snapshot PREVIEW_SNAPSHOT = new SpotifyNowPlaying.Snapshot(
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

    private MusicMsdfFont titleFont;
    private MusicMsdfFont detailFont;
    private MusicMsdfFont controlFont;

    private boolean repeatActive;
    private boolean shuffleActive;
    private double animatedProgress;
    private long lastProgressFrame = System.nanoTime();
    private String lastProgressTrack = "";

    private boolean displayBoundsValid;
    private final Matrix3x2f displayInverse = new Matrix3x2f();

    public SpotifyOverlayModule() {
        super("Spotify Overlay", "Displays the current Spotify or compatible media track", Category.MISC);
        hideWhenNotPlaying.setVisibility(betterDisplay::getValue);
        registerSetting(betterDisplay);
        registerSetting(hideWhenNotPlaying);

        hudElement("Spotify Overlay", this::renderHud)
            .visibleWhen(this::shouldRenderInGameHud)
            .defaults(HudElement.CENTER, 84.0f, 1.0f);
    }

    private HudElement.Size renderHud(GuiGraphicsExtractor graphics, boolean example) {
        boolean useBetterDisplay = isBetterDisplayEnabled();
        if (example) {
            if (!useBetterDisplay) return HudRender.text(graphics, STANDARD_PREVIEW_LINE, 0xFFFFFFFF);
            renderBetterDisplay(graphics, PREVIEW_SNAPSHOT, false);
            return BETTER_DISPLAY_SIZE;
        }

        displayBoundsValid = false;
        SpotifyNowPlaying.requestRefreshIfNeeded(useBetterDisplay);

        if (useBetterDisplay) {
            SpotifyNowPlaying.Snapshot snapshot = SpotifyNowPlaying.getSnapshot();
            if (isHiddenWhenNotPlaying(snapshot)) return HudElement.Size.NONE;
            renderBetterDisplay(graphics, snapshot, true);
            return BETTER_DISPLAY_SIZE;
        }

        String line = SpotifyNowPlaying.getFormattedLine();
        if (line == null || line.isBlank()) return HudElement.Size.NONE;
        return HudRender.text(graphics, line, 0xFFFFFFFF);
    }

    private void renderBetterDisplay(GuiGraphicsExtractor graphics, SpotifyNowPlaying.Snapshot snapshot, boolean interactive) {
        loadFonts();

        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        if (interactive) {
            displayInverse.set(pose).invert();
            displayBoundsValid = true;
        }
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
        drawControls(graphics, snapshot, pose, scissor, interactive);
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
        boolean interactive
    ) {
        if (controlFont == null) return;
        Vector2f localMouse = toDisplaySpace(mc.mouseHandler.getScaledXPos(mc.getWindow()), mc.mouseHandler.getScaledYPos(mc.getWindow()));
        float localMouseX = localMouse.x;
        float localMouseY = localMouse.y;
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

        Vector2f local = toDisplaySpace(mouseX, mouseY);
        float localX = local.x;
        float localY = local.y;
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

    private Vector2f toDisplaySpace(double mouseX, double mouseY) {
        return displayInverse.transformPosition(new Vector2f((float) mouseX, (float) mouseY));
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
}
