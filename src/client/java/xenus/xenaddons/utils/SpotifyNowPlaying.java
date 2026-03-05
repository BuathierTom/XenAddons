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
    private static volatile int spotifyProcessId = -1;

    private SpotifyNowPlaying() {}

    public static void requestRefreshIfNeeded() {
        long now = System.currentTimeMillis();
        if ((now - lastFetchAt) < REFRESH_MS) return;
        if (!FETCHING.compareAndSet(false, true)) return;

        Thread.startVirtualThread(() -> {
            try {
                refreshNowPlaying();
            } finally {
                lastFetchAt = System.currentTimeMillis();
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

    private static void refreshNowPlaying() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            refreshWindowsLikeSoopy();
            return;
        }
        if (os.contains("mac")) {
            refreshMac();
            return;
        }
        artist = "";
        title = "";
        state = "Not open";
    }

    private static void refreshWindowsLikeSoopy() {
        String currentSong = getSongFromPid(spotifyProcessId);
        if (currentSong == null) {
            spotifyProcessId = -1;
            for (Integer pid : getSpotifyPids()) {
                String song = getSongFromPid(pid);
                if (song != null) {
                    spotifyProcessId = pid;
                    currentSong = song;
                    break;
                }
            }
        }

        if (currentSong == null) {
            artist = "";
            title = "";
            state = "Not open";
            return;
        }

        if ("__PAUSED__".equals(currentSong)) {
            artist = "";
            title = "";
            state = "Paused";
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
    }

    private static void refreshMac() {
        try {
            Process check = new ProcessBuilder("pgrep", "-x", "Spotify").start();
            boolean done = check.waitFor(800, TimeUnit.MILLISECONDS);
            if (!done || check.exitValue() != 0) {
                if (!done) check.destroyForcibly();
                artist = "";
                title = "";
                state = "Not open";
                return;
            }

            String playerState = runAppleScript("tell application \"Spotify\" to player state as text");
            if (playerState == null) {
                artist = "";
                title = "";
                state = "Not open";
                return;
            }

            String normalizedState = playerState.trim().toLowerCase(Locale.ROOT);
            if (!normalizedState.contains("playing")) {
                artist = "";
                title = "";
                state = "Paused";
                return;
            }

            String artistRaw = runAppleScript("tell application \"Spotify\" to artist of current track as text");
            String titleRaw = runAppleScript("tell application \"Spotify\" to name of current track as text");
            if (artistRaw == null || titleRaw == null || artistRaw.isBlank() || titleRaw.isBlank()) {
                artist = "";
                title = "";
                state = "Paused";
                return;
            }

            artist = sanitize(artistRaw);
            title = sanitize(titleRaw);
            state = "Playing";
        } catch (Exception e) {
            artist = "";
            title = "";
            state = "Not open";
            XenAddons.LOGGER.debug("Spotify overlay refresh failed", e);
        }
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
                    if ("Spotify Free".equalsIgnoreCase(status)
                        || "Spotify Premium".equalsIgnoreCase(status)
                        || "AngleHiddenWindow".equalsIgnoreCase(status)) {
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

    private static String sanitize(String input) {
        if (input == null) return "";
        String noCodes = input.replaceAll("(?i)[§&][0-9A-FK-OR]", "");
        return noCodes.trim();
    }

    private static String clampWithEllipsis(String value, int maxLength) {
        if (value == null) return "";
        int cap = Math.max(4, maxLength);
        if (value.length() <= cap) return value;
        return value.substring(0, cap - 3) + "...";
    }

}
