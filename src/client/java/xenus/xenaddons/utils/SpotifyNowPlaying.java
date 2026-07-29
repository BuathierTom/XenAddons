package xenus.xenaddons.utils;

import xenus.xenaddons.XenAddons;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SpotifyNowPlaying {
    private static final long REFRESH_MS = 1500L;
    private static final int DEFAULT_MAX_TITLE_LENGTH = 20;
    private static final String MAC_FIELD_SEPARATOR = "\u001F";
    private static final String MAC_NOW_PLAYING_SCRIPT = """
        tell application "Spotify"
            set stateValue to player state as text
            set artistValue to ""
            set titleValue to ""
            set artworkValue to ""
            set trackValue to ""
            set positionValue to "-1"
            set durationValue to "-1"

            try
                set artistValue to artist of current track as text
                set titleValue to name of current track as text
                set artworkValue to artwork url of current track as text
                set trackValue to spotify url of current track as text
                set positionValue to player position as integer
                set durationValue to duration of current track as integer
            end try

            return stateValue & (ASCII character 31) & artistValue & (ASCII character 31) & titleValue & (ASCII character 31) & artworkValue & (ASCII character 31) & trackValue & (ASCII character 31) & positionValue & (ASCII character 31) & durationValue
        end tell
        """;
    private static final AtomicBoolean FETCHING = new AtomicBoolean(false);

    private static volatile long lastFetchAt = 0L;
    private static volatile String source = "Music";
    private static volatile String artist = "";
    private static volatile String title = "";
    private static volatile String state = "Not open";
    private static volatile String artworkUrl = "";
    private static volatile byte[] artworkBytes;
    private static volatile String trackUrl = "";
    private static volatile double progressSeconds = -1.0;
    private static volatile int durationSeconds = -1;
    private static volatile long progressSampleNanos = 0L;
    private static volatile boolean progressAdvancing = false;
    private static volatile String progressTrack = "";
    private static volatile double lastRawProgressSeconds = -1.0;
    private static volatile boolean lastFetchIncludedProgress = false;

    private SpotifyNowPlaying() {}

    public static void requestRefreshIfNeeded() {
        requestRefreshIfNeeded(false);
    }

    public static void requestRefreshIfNeeded(boolean includeProgress) {
        long now = System.currentTimeMillis();
        if ((now - lastFetchAt) < REFRESH_MS && (!includeProgress || lastFetchIncludedProgress)) return;
        if (!FETCHING.compareAndSet(false, true)) return;

        Thread.startVirtualThread(() -> {
            try {
                refreshNowPlaying(includeProgress);
            } finally {
                lastFetchAt = System.currentTimeMillis();
                lastFetchIncludedProgress = includeProgress;
                FETCHING.set(false);
            }
        });
    }

    public static String getFormattedLine() {
        return getFormattedLine(DEFAULT_MAX_TITLE_LENGTH);
    }

    public static String getFormattedLine(int maxTitleLength) {
        if ("Playing".equals(state) && !artist.isBlank() && !title.isBlank()) {
            String displayTitle = clampWithEllipsis(title, maxTitleLength);
            return "§6" + displaySource() + " §7> §a" + artist + " §7- §b" + displayTitle;
        }
        if ("Paused".equals(state)) {
            return "§6" + displaySource() + " §7> §cPaused";
        }
        return "§6" + displaySource() + " §7> §cNot open";
    }

    public static Snapshot getSnapshot() {
        double liveProgress = currentProgress(System.nanoTime());

        return new Snapshot(
            displaySource(),
            artist,
            title,
            state,
            artworkUrl,
            artworkBytes,
            trackUrl,
            liveProgress,
            durationSeconds
        );
    }

    public record Snapshot(
            String source,
            String artist,
            String title,
            String state,
            String artworkUrl,
            byte[] artworkBytes,
            String trackUrl,
            double progressSeconds,
            int durationSeconds
    ) {
        public boolean isPlaying() {
            return "Playing".equals(state) && !artist.isBlank() && !title.isBlank();
        }

        public boolean isPaused() {
            return "Paused".equals(state);
        }

        public boolean hasProgress() {
            return progressSeconds >= 0.0 && durationSeconds > 0;
        }
    }

    private static void refreshNowPlaying(boolean includeProgress) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            refreshWindows(includeProgress);
            return;
        }
        if (os.contains("mac")) {
            refreshMac(includeProgress);
            return;
        }
        clearNowPlaying("Music");
    }

    private static void refreshWindows(boolean includeProgress) {
        WindowsSpotifyBridge.Snapshot snapshot = WindowsSpotifyBridge.read(includeProgress);
        if (snapshot == null) {
            clearNowPlaying();
            return;
        }

        source = snapshot.source();
        artist = sanitize(snapshot.artist());
        title = sanitize(snapshot.title());
        state = snapshot.playing() ? "Playing" : "Paused";
        artworkUrl = "";
        artworkBytes = includeProgress ? snapshot.artwork() : null;
        trackUrl = "";
        setProgress(
            includeProgress ? snapshot.progressSeconds() : -1.0,
            includeProgress ? snapshot.durationSeconds() : -1,
            snapshot.playing(),
            artist + '\u0000' + title
        );
    }

    private static void refreshMac(boolean includeProgress) {
        source = "Spotify";
        try {
            Process check = new ProcessBuilder("pgrep", "-x", "Spotify").start();
            boolean done = check.waitFor(800, TimeUnit.MILLISECONDS);
            if (!done || check.exitValue() != 0) {
                if (!done) check.destroyForcibly();
                clearNowPlaying("Spotify");
                return;
            }

            String nowPlaying = runAppleScript(MAC_NOW_PLAYING_SCRIPT);
            if (nowPlaying == null) {
                clearNowPlaying("Spotify");
                return;
            }

            String[] fields = nowPlaying.split(MAC_FIELD_SEPARATOR, -1);
            if (fields.length < 7) {
                clearNowPlaying("Spotify");
                return;
            }

            String normalizedState = fields[0].trim().toLowerCase(Locale.ROOT);
            String artistRaw = fields[1];
            String titleRaw = fields[2];
            if (artistRaw == null || titleRaw == null || artistRaw.isBlank() || titleRaw.isBlank()) {
                artist = "";
                title = "";
                state = normalizedState.contains("playing") ? "Playing" : "Paused";
                artworkUrl = "";
                artworkBytes = null;
                trackUrl = "";
                clearProgress();
                return;
            }

            artist = sanitize(artistRaw);
            title = sanitize(titleRaw);
            state = normalizedState.contains("playing") ? "Playing" : "Paused";
            artworkUrl = includeProgress ? sanitizeUrl(fields[3]) : "";
            artworkBytes = null;
            trackUrl = includeProgress ? sanitizeTrackUrl(fields[4]) : "";
            if (includeProgress) {
                double sampledProgress = parseIntOrDefault(fields[5], -1);
                int durationMs = parseIntOrDefault(fields[6], -1);
                setProgress(
                    sampledProgress,
                    durationMs > 0 ? durationMs / 1000 : -1,
                    "Playing".equals(state),
                    artist + '\u0000' + title
                );
            } else {
                clearProgress();
            }
        } catch (Exception e) {
            clearNowPlaying("Spotify");
            XenAddons.LOGGER.debug("Spotify overlay refresh failed", e);
        }
    }

    public static void control(ControlAction action) {
        Thread.startVirtualThread(() -> {
            try {
                controlNow(action);
            } catch (Exception e) {
                XenAddons.LOGGER.debug("Spotify overlay control failed", e);
            }
        });
    }

    private static void controlNow(ControlAction action) throws Exception {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("mac")) {
            String command = switch (action) {
                case PREVIOUS -> "previous track";
                case PLAY_PAUSE -> "playpause";
                case NEXT -> "next track";
            };
            runAppleScript("tell application \"Spotify\" to " + command);
            lastFetchAt = 0L;
            return;
        }

        if (os.contains("win")) {
            WindowsSpotifyBridge.control(action);
            lastFetchAt = 0L;
            return;
        }

        String command = switch (action) {
            case PREVIOUS -> "previous";
            case PLAY_PAUSE -> "play-pause";
            case NEXT -> "next";
        };
        runCommand(1500, "playerctl", command);
        lastFetchAt = 0L;
    }

    public enum ControlAction {
        PREVIOUS,
        PLAY_PAUSE,
        NEXT
    }

    public static void setVolume(int percent) {
        int safePercent = Math.max(0, Math.min(100, percent));
        Thread.startVirtualThread(() -> {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("mac")) {
                runAppleScript("tell application \"Spotify\" to set sound volume to " + safePercent);
            } else if (os.contains("win")) {
                WindowsSpotifyBridge.setVolume(safePercent);
            } else if (!os.contains("win")) {
                runCommand(1500, "playerctl", "volume", (safePercent / 100.0) + "");
            }
        });
    }

    public static void setRepeat(boolean enabled) {
        Thread.startVirtualThread(() -> {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("mac")) {
                runAppleScript("tell application \"Spotify\" to set repeating to " + enabled);
            } else if (os.contains("win")) {
                WindowsSpotifyBridge.setRepeat(enabled);
            } else if (!os.contains("win")) {
                runCommand(1500, "playerctl", "loop", enabled ? "Playlist" : "None");
            }
        });
    }

    public static void setShuffle(boolean enabled) {
        Thread.startVirtualThread(() -> {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("mac")) {
                runAppleScript("tell application \"Spotify\" to set shuffling to " + enabled);
            } else if (os.contains("win")) {
                WindowsSpotifyBridge.setShuffle(enabled);
            } else if (!os.contains("win")) {
                runCommand(1500, "playerctl", "shuffle", enabled ? "On" : "Off");
            }
        });
    }

    public static void toggleLike() {
        Thread.startVirtualThread(() -> {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("mac")) {
                runAppleScript(
                    "tell application \"Spotify\" to set starred of current track to not (starred of current track)"
                );
            } else if (os.contains("win")) {
                WindowsSpotifyBridge.toggleLike();
            }
        });
    }

    private static void clearNowPlaying() {
        clearNowPlaying("Music");
    }

    private static void clearNowPlaying(String preferredSource) {
        source = preferredSource;
        artist = "";
        title = "";
        state = "Not open";
        artworkUrl = "";
        artworkBytes = null;
        trackUrl = "";
        clearProgress();
    }

    private static void setProgress(
        double sampledProgress,
        int duration,
        boolean playing,
        String trackKey
    ) {
        if (sampledProgress < 0.0 || duration <= 0) {
            clearProgress();
            return;
        }

        long now = System.nanoTime();
        double nextProgress = sampledProgress;
        if (trackKey.equals(progressTrack) && progressSeconds >= 0.0) {
            double estimatedProgress = currentProgress(now);
            boolean rawPositionChanged =
                lastRawProgressSeconds < 0.0
                    || Math.abs(sampledProgress - lastRawProgressSeconds) > 0.5;
            if (!rawPositionChanged || Math.abs(sampledProgress - estimatedProgress) <= 2.0) {
                nextProgress = Math.max(sampledProgress, estimatedProgress);
            }
        }

        progressSeconds = Math.min(nextProgress, duration);
        durationSeconds = duration;
        progressSampleNanos = now;
        progressAdvancing = playing;
        progressTrack = trackKey;
        lastRawProgressSeconds = sampledProgress;
    }

    private static double currentProgress(long now) {
        double current = progressSeconds;
        if (progressAdvancing && current >= 0.0 && progressSampleNanos > 0L) {
            current += Math.max(0.0, (now - progressSampleNanos) / 1_000_000_000.0);
        }
        return durationSeconds > 0 ? Math.min(current, durationSeconds) : current;
    }

    private static void clearProgress() {
        progressSeconds = -1.0;
        durationSeconds = -1;
        progressSampleNanos = 0L;
        progressAdvancing = false;
        progressTrack = "";
        lastRawProgressSeconds = -1.0;
    }

    private static String displaySource() {
        return source == null || source.isBlank() ? "Music" : source;
    }

    private static String runAppleScript(String script) {
        Process process = null;
        try {
            process = new ProcessBuilder("osascript", "-e", script).start();
            boolean done = process.waitFor(1200, TimeUnit.MILLISECONDS);
            if (!done || process.exitValue() != 0) {
                if (!done) process.destroyForcibly();
                return null;
            }
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                return line == null ? null : line.trim();
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
        }
    }

    private static void runCommand(long timeoutMs, String... command) {
        Process process = null;
        try {
            process = new ProcessBuilder(command).start();
            boolean done = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!done) process.destroyForcibly();
        } catch (Exception ignored) {
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
        }
    }

    private static String sanitize(String input) {
        if (input == null) return "";
        String noCodes = input.replaceAll("(?i)[§&][0-9A-FK-OR]", "");
        return noCodes.trim();
    }

    private static String sanitizeUrl(String input) {
        if (input == null) return "";
        String trimmed = input.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed;
        return "";
    }

    private static String sanitizeTrackUrl(String input) {
        if (input == null) return "";
        String trimmed = input.trim();
        if (trimmed.startsWith("https://open.spotify.com/track/")) return trimmed;
        if (trimmed.startsWith("spotify:track:")) {
            String id = trimmed.substring("spotify:track:".length()).trim();
            if (id.matches("[A-Za-z0-9]+")) {
                return "https://open.spotify.com/track/" + id;
            }
        }
        return "";
    }

    private static int parseIntOrDefault(String value, int fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String clampWithEllipsis(String value, int maxLength) {
        if (value == null) return "";
        int cap = Math.max(4, maxLength);
        if (value.length() <= cap) return value;
        return value.substring(0, cap - 3) + "...";
    }

}
