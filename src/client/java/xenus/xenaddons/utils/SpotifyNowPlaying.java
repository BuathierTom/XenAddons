package xenus.xenaddons.utils;

import xenus.xenaddons.XenAddons;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SpotifyNowPlaying {
    private static final long REFRESH_MS = 1500L;
    private static final int DEFAULT_MAX_TITLE_LENGTH = 20;
    private static final AtomicBoolean FETCHING = new AtomicBoolean(false);

    private static volatile long lastFetchAt = 0L;
    private static volatile String artist = "";
    private static volatile String title = "";
    private static volatile String state = "Not open";
    private static volatile String artworkUrl = "";
    private static volatile String trackUrl = "";
    private static volatile int progressSeconds = -1;
    private static volatile int durationSeconds = -1;
    private static volatile int spotifyProcessId = -1;
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
            return "§6Spotify §7> §a" + artist + " §7- §b" + displayTitle;
        }
        if ("Paused".equals(state)) {
            return "§6Spotify §7> §cPaused";
        }
        return "§6Spotify §7> §cNot open";
    }

    public static Snapshot getSnapshot() {
        return new Snapshot(artist, title, state, artworkUrl, trackUrl, progressSeconds, durationSeconds);
    }

    public record Snapshot(String artist, String title, String state, String artworkUrl, String trackUrl, int progressSeconds, int durationSeconds) {
        public boolean isPlaying() {
            return "Playing".equals(state) && !artist.isBlank() && !title.isBlank();
        }

        public boolean isPaused() {
            return "Paused".equals(state);
        }

        public boolean hasProgress() {
            return progressSeconds >= 0 && durationSeconds > 0;
        }
    }

    private static void refreshNowPlaying(boolean includeProgress) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            refreshWindowsLikeSoopy();
            return;
        }
        if (os.contains("mac")) {
            refreshMac(includeProgress);
            return;
        }
        artist = "";
        title = "";
        state = "Not open";
        artworkUrl = "";
        trackUrl = "";
        progressSeconds = -1;
        durationSeconds = -1;
    }

    private static void refreshWindowsLikeSoopy() {
        String currentSong = getWindowsSpotifyWindowTitle();
        if (currentSong == null) currentSong = getWindowsSpotifyTasklistTitle();

        if (currentSong == null) {
            artist = "";
            title = "";
            state = "Not open";
            artworkUrl = "";
            trackUrl = "";
            progressSeconds = -1;
            durationSeconds = -1;
            return;
        }

        if ("__PAUSED__".equals(currentSong)) {
            artist = "";
            title = "";
            state = "Paused";
            artworkUrl = "";
            trackUrl = "";
            progressSeconds = -1;
            durationSeconds = -1;
            return;
        }

        if ("Spotify".equalsIgnoreCase(currentSong)) {
            currentSong = "Advertisement";
        }
        String[] parts = currentSong.split(" - ", 2);
        if (parts.length == 2) {
            artist = sanitize(parts[0]);
            title = sanitize(parts[1]);
        } else {
            artist = "Unknown";
            title = sanitize(currentSong);
        }
        state = "Playing";
        artworkUrl = "";
        trackUrl = "";
        progressSeconds = -1;
        durationSeconds = -1;
    }

    private static void refreshMac(boolean includeProgress) {
        try {
            Process check = new ProcessBuilder("pgrep", "-x", "Spotify").start();
            boolean done = check.waitFor(800, TimeUnit.MILLISECONDS);
            if (!done || check.exitValue() != 0) {
                if (!done) check.destroyForcibly();
                artist = "";
                title = "";
                state = "Not open";
                artworkUrl = "";
                trackUrl = "";
                progressSeconds = -1;
                durationSeconds = -1;
                return;
            }

            String playerState = runAppleScript("tell application \"Spotify\" to player state as text");
            if (playerState == null) {
                artist = "";
                title = "";
                state = "Not open";
                artworkUrl = "";
                trackUrl = "";
                progressSeconds = -1;
                durationSeconds = -1;
                return;
            }

            String normalizedState = playerState.trim().toLowerCase(Locale.ROOT);
            String artistRaw = runAppleScript("tell application \"Spotify\" to artist of current track as text");
            String titleRaw = runAppleScript("tell application \"Spotify\" to name of current track as text");
            if (artistRaw == null || titleRaw == null || artistRaw.isBlank() || titleRaw.isBlank()) {
                artist = "";
                title = "";
                state = normalizedState.contains("playing") ? "Playing" : "Paused";
                artworkUrl = "";
                trackUrl = "";
                progressSeconds = -1;
                durationSeconds = -1;
                return;
            }

            artist = sanitize(artistRaw);
            title = sanitize(titleRaw);
            state = normalizedState.contains("playing") ? "Playing" : "Paused";
            artworkUrl = includeProgress ? sanitizeUrl(runAppleScript("tell application \"Spotify\" to artwork url of current track as text")) : "";
            trackUrl = includeProgress ? sanitizeTrackUrl(runAppleScript("tell application \"Spotify\" to spotify url of current track as text")) : "";
            if (includeProgress) {
                progressSeconds = parseIntOrDefault(runAppleScript("tell application \"Spotify\" to player position as integer"), -1);
                int durationMs = parseIntOrDefault(runAppleScript("tell application \"Spotify\" to duration of current track as integer"), -1);
                durationSeconds = durationMs > 0 ? durationMs / 1000 : -1;
            } else {
                progressSeconds = -1;
                durationSeconds = -1;
            }
        } catch (Exception e) {
            artist = "";
            title = "";
            state = "Not open";
            artworkUrl = "";
            trackUrl = "";
            progressSeconds = -1;
            durationSeconds = -1;
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
            String key = switch (action) {
                case PREVIOUS -> "0xB1";
                case PLAY_PAUSE -> "0xB3";
                case NEXT -> "0xB0";
            };
            String script = "$sig='[DllImport(\"user32.dll\")]public static extern void keybd_event(byte bVk, byte bScan, int dwFlags, int dwExtraInfo);';"
                + "Add-Type -MemberDefinition $sig -Name MediaKeys -Namespace XenAddons;"
                + "[XenAddons.MediaKeys]::keybd_event(" + key + ",0,0,0);"
                + "[XenAddons.MediaKeys]::keybd_event(" + key + ",0,2,0);";
            runCommand(1500, "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", script);
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
            }
        });
    }

    private static String getWindowsSpotifyWindowTitle() {
        Process process = null;
        try {
            String script = "$enc=New-Object System.Component.UTF8Encoding $false;"
                + "[Console]::OutputEncoding=$enc;"
                + "$OutputEncoding=$enc;"
                + "Get-Process -Name Spotify -ErrorAction SilentlyContinue | "
                + "Where-Object { $_.MainWindowTitle -and $_.MainWindowTitle.Trim().Length -gt 0 } | "
                + "ForEach-Object { $_.MainWindowTitle }";
            process = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-Command",
                script
            ).start();

            String pausedCandidate = null;
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String windowTitle = line.trim();
                    if (windowTitle.isBlank()) continue;
                    if (isPausedWindowTitle(windowTitle)) {
                        pausedCandidate = "__PAUSED__";
                        continue;
                    }
                    return windowTitle;
                }
            }

            process.waitFor(1500, TimeUnit.MILLISECONDS);
            return pausedCandidate;
        } catch (Exception ignored) {
            return null;
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
        }
    }

    private static String getWindowsSpotifyTasklistTitle() {
        String currentSong = getSongFromPid(spotifyProcessId);
        if (currentSong != null) return currentSong;

        spotifyProcessId = -1;
        for (Integer pid : getSpotifyPids()) {
            String song = getSongFromPid(pid);
            if (song != null) {
                spotifyProcessId = pid;
                return song;
            }
        }
        return null;
    }

    private static List<Integer> getSpotifyPids() {
        List<Integer> out = new ArrayList<>();
        Process process = null;
        try {
            process = new ProcessBuilder("tasklist.exe", "/fo", "csv", "/nh").start();
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = parseCsvLine(line);
                    if (parts.length < 2) continue;
                    if (!"Spotify.exe".equalsIgnoreCase(parts[0])) continue;
                    try {
                        out.add(Integer.parseInt(parts[1]));
                    } catch (Exception ignored) {}
                }
            }
            process.waitFor(1500, TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
        }
        return out;
    }

    private static String getSongFromPid(int pid) {
        if (pid <= 0) return null;
        Process process = null;
        try {
            process = new ProcessBuilder("tasklist.exe", "/FO", "csv", "/V", "/FI", "PID eq " + pid).start();
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                boolean headerSkipped = false;
                while ((line = reader.readLine()) != null) {
                    if (!headerSkipped) {
                        headerSkipped = true;
                        continue;
                    }
                    String[] parts = parseCsvLine(line);
                    if (parts.length < 9) continue;
                    String status = parts[parts.length - 1];
                    if ("N/A".equalsIgnoreCase(status)) continue;
                    if (isPausedWindowTitle(status)) {
                        return "__PAUSED__";
                    }
                    return status;
                }
            }
            process.waitFor(1500, TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
        }
        return null;
    }

    private static boolean isPausedWindowTitle(String title) {
        return "Spotify Free".equalsIgnoreCase(title)
            || "Spotify Premium".equalsIgnoreCase(title)
            || "AngleHiddenWindow".equalsIgnoreCase(title);
    }

    private static String[] parseCsvLine(String line) {
        if (line == null) return new String[0];
        String trimmed = line.trim();
        if (trimmed.startsWith("\"")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("\"")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        return trimmed.split("\",\"");
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
