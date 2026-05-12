package xenus.xenaddons.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import xenus.xenaddons.XenAddons;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpotifyCoverCache {
    public static final int COVER_TEXTURE_SIZE = 128;

    private static final int MAX_DISK_CACHE_FILES = 10;
    private static final String CACHE_DIRECTORY = "xenaddons/spotify-cache";
    private static final ConcurrentHashMap<String, Identifier> COVERS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> RESOLVED_URLS = new ConcurrentHashMap<>();
    private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();
    private static final Set<String> RESOLVING = ConcurrentHashMap.newKeySet();
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();
    private static final Set<String> FAILED_RESOLUTIONS = ConcurrentHashMap.newKeySet();

    private SpotifyCoverCache() {}

    public static Identifier getOrLoad(SpotifyNowPlaying.Snapshot snapshot) {
        if (snapshot == null) return null;

        String directUrl = sanitizeUrl(snapshot.artworkUrl());
        if (!directUrl.isBlank()) {
            Identifier cover = getOrLoadUrl(directUrl);
            if (cover != null || !FAILED.contains(directUrl)) return cover;
        }

        String trackUrl = sanitizeUrl(snapshot.trackUrl());
        if (!trackUrl.isBlank() || (!snapshot.artist().isBlank() && !snapshot.title().isBlank())) {
            String key = "best:" + trackUrl + "|" + snapshot.artist().toLowerCase(Locale.ROOT) + "|" + snapshot.title().toLowerCase(Locale.ROOT);
            return getOrResolve(key, () -> resolveBestCover(trackUrl, snapshot.artist(), snapshot.title()));
        }

        return null;
    }

    public static Identifier getOrLoad(String url) {
        String cleanUrl = sanitizeUrl(url);
        if (cleanUrl.isBlank()) return null;
        return getOrLoadUrl(cleanUrl);
    }

    private static Identifier getOrLoadUrl(String cleanUrl) {
        Identifier cached = COVERS.get(cleanUrl);
        if (cached != null || FAILED.contains(cleanUrl)) return cached;
        if (!LOADING.add(cleanUrl)) return null;

        Thread.startVirtualThread(() -> load(cleanUrl));
        return null;
    }

    private static Identifier getOrResolve(String key, Resolver resolver) {
        String resolved = RESOLVED_URLS.get(key);
        if (resolved != null) return getOrLoad(resolved);
        if (FAILED_RESOLUTIONS.contains(key)) return null;
        if (!RESOLVING.add(key)) return null;

        Thread.startVirtualThread(() -> {
            try {
                String url = sanitizeUrl(resolver.resolve());
                if (url.isBlank()) {
                    FAILED_RESOLUTIONS.add(key);
                    return;
                }
                RESOLVED_URLS.put(key, url);
                getOrLoad(url);
            } catch (Exception e) {
                FAILED_RESOLUTIONS.add(key);
                XenAddons.LOGGER.debug("Failed to resolve Spotify cover", e);
            } finally {
                RESOLVING.remove(key);
            }
        });
        return null;
    }

    private static void load(String url) {
        NativeImage image = null;
        boolean scheduledRegister = false;
        try {
            image = loadFromDiskOrNetwork(url);

            NativeImage loadedImage = image;
            MinecraftClient.getInstance().execute(() -> register(url, loadedImage));
            scheduledRegister = true;
            image = null;
        } catch (Exception e) {
            FAILED.add(url);
            XenAddons.LOGGER.debug("Failed to load Spotify cover", e);
        } finally {
            if (!scheduledRegister) {
                LOADING.remove(url);
            }
            if (image != null) {
                image.close();
            }
        }
    }

    private static NativeImage loadFromDiskOrNetwork(String url) throws Exception {
        File cachedFile = cacheFile(url);
        if (cachedFile.isFile()) {
            try {
                NativeImage image = readCachedImage(cachedFile);
                cachedFile.setLastModified(System.currentTimeMillis());
                return image;
            } catch (Exception e) {
                if (!cachedFile.delete()) {
                    XenAddons.LOGGER.debug("Failed to delete invalid Spotify cover cache file {}", cachedFile);
                }
                XenAddons.LOGGER.debug("Failed to read Spotify cover from disk cache {}", cachedFile, e);
            }
        }

        byte[] imageBytes = downloadImageBytes(url);
        return readLikeSpotifyOverlay(imageBytes, cachedFile);
    }

    private static byte[] downloadImageBytes(String url) throws Exception {
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(2500);
        connection.setReadTimeout(2500);
        connection.setRequestProperty("Accept", "image/png,image/jpeg,image/*");
        connection.setRequestProperty("User-Agent", "XenAddons Spotify Overlay");
        try (InputStream stream = connection.getInputStream()) {
            return stream.readAllBytes();
        }
    }

    private static void register(String url, NativeImage image) {
        try {
            Identifier id = Identifier.of("xenaddons", "spotify/cover_" + UUID.randomUUID().toString().replace("-", ""));
            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "XenAddons Spotify cover", image);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
            COVERS.put(url, id);
        } catch (Exception e) {
            FAILED.add(url);
            image.close();
            XenAddons.LOGGER.debug("Failed to register Spotify cover texture", e);
        } finally {
            LOADING.remove(url);
        }
    }

    private static NativeImage readLikeSpotifyOverlay(byte[] imageBytes, File cachedFile) throws Exception {
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (bufferedImage != null) {
            BufferedImage resized = squareResize(bufferedImage);
            writeCachedPng(resized, cachedFile);
            return bufferedImageToNativeImage(resized);
        }

        NativeImage image = NativeImage.read(NativeImage.Format.RGBA, new ByteArrayInputStream(imageBytes));
        NativeImage resized = squareResize(image);
        writeCachedNativeImage(resized, cachedFile);
        return resized;
    }

    private static NativeImage readCachedImage(File cachedFile) throws Exception {
        BufferedImage bufferedImage = ImageIO.read(cachedFile);
        if (bufferedImage != null) {
            return bufferedImageToNativeImage(squareResize(bufferedImage));
        }

        try (InputStream stream = cachedFile.toURI().toURL().openStream()) {
            NativeImage image = NativeImage.read(NativeImage.Format.RGBA, stream);
            return squareResize(image);
        }
    }

    private static BufferedImage squareResize(BufferedImage image) {
        int cropSize = Math.min(image.getWidth(), image.getHeight());
        int cropX = (image.getWidth() - cropSize) / 2;
        int cropY = (image.getHeight() - cropSize) / 2;
        BufferedImage resized = new BufferedImage(COVER_TEXTURE_SIZE, COVER_TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(
                image,
                0,
                0,
                COVER_TEXTURE_SIZE,
                COVER_TEXTURE_SIZE,
                cropX,
                cropY,
                cropX + cropSize,
                cropY + cropSize,
                null
            );
        } finally {
            graphics.dispose();
        }
        return resized;
    }

    private static NativeImage bufferedImageToNativeImage(BufferedImage bufferedImage) {
        NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, bufferedImage.getWidth(), bufferedImage.getHeight(), false);
        for (int y = 0; y < bufferedImage.getHeight(); y++) {
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                nativeImage.setColorArgb(x, y, bufferedImage.getRGB(x, y));
            }
        }
        return nativeImage;
    }

    private static void writeCachedPng(BufferedImage image, File cachedFile) {
        try {
            ensureCacheDirectory();
            ImageIO.write(image, "png", cachedFile);
            trimDiskCache();
        } catch (Exception e) {
            XenAddons.LOGGER.debug("Failed to write Spotify cover cache file {}", cachedFile, e);
        }
    }

    private static void writeCachedNativeImage(NativeImage image, File cachedFile) {
        try {
            ensureCacheDirectory();
            image.writeTo(cachedFile.toPath());
            trimDiskCache();
        } catch (Exception e) {
            XenAddons.LOGGER.debug("Failed to write Spotify cover cache file {}", cachedFile, e);
        }
    }

    private static File cacheFile(String url) {
        return new File(cacheDirectory(), UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8)) + ".png");
    }

    private static File cacheDirectory() {
        return new File(MinecraftClient.getInstance().runDirectory, CACHE_DIRECTORY);
    }

    private static void ensureCacheDirectory() {
        File directory = cacheDirectory();
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Unable to create Spotify cover cache directory " + directory);
        }
    }

    private static void trimDiskCache() {
        File[] files = cacheDirectory().listFiles(File::isFile);
        if (files == null || files.length <= MAX_DISK_CACHE_FILES) return;

        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        int deleteCount = files.length - MAX_DISK_CACHE_FILES;
        for (int i = 0; i < deleteCount; i++) {
            if (!files[i].delete()) {
                XenAddons.LOGGER.debug("Failed to delete old Spotify cover cache file {}", files[i]);
            }
        }
    }

    private static NativeImage squareResize(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width == COVER_TEXTURE_SIZE && height == COVER_TEXTURE_SIZE) return image;

        int cropSize = Math.min(width, height);
        int cropX = (width - cropSize) / 2;
        int cropY = (height - cropSize) / 2;
        NativeImage resized = new NativeImage(NativeImage.Format.RGBA, COVER_TEXTURE_SIZE, COVER_TEXTURE_SIZE, false);
        image.resizeSubRectTo(cropX, cropY, cropSize, cropSize, resized);
        image.close();
        return resized;
    }

    private static String resolveBestCover(String trackUrl, String artist, String title) {
        if (!trackUrl.isBlank()) {
            try {
                String url = sanitizeUrl(resolveSpotifyOEmbed(trackUrl));
                if (!url.isBlank()) return url;
            } catch (Exception e) {
                XenAddons.LOGGER.debug("Failed to resolve Spotify oEmbed cover", e);
            }
        }
        if (!artist.isBlank() && !title.isBlank()) {
            try {
                return resolveITunesSearch(artist, title);
            } catch (Exception e) {
                XenAddons.LOGGER.debug("Failed to resolve iTunes cover", e);
            }
        }
        return "";
    }

    private static String resolveSpotifyOEmbed(String trackUrl) throws Exception {
        String endpoint = "https://open.spotify.com/oembed?url=" + URLEncoder.encode(trackUrl, StandardCharsets.UTF_8);
        JsonObject root = readJson(endpoint);
        return root.has("thumbnail_url") ? root.get("thumbnail_url").getAsString() : "";
    }

    private static String resolveITunesSearch(String artist, String title) throws Exception {
        String term = artist + " " + title;
        String endpoint = "https://itunes.apple.com/search?media=music&entity=song&limit=1&term="
            + URLEncoder.encode(term, StandardCharsets.UTF_8);
        JsonObject root = readJson(endpoint);
        JsonArray results = root.has("results") ? root.getAsJsonArray("results") : null;
        if (results == null || results.size() == 0 || !results.get(0).isJsonObject()) return "";
        JsonObject result = results.get(0).getAsJsonObject();
        if (!result.has("artworkUrl100")) return "";
        return result.get("artworkUrl100").getAsString().replace("100x100bb", "600x600bb");
    }

    private static JsonObject readJson(String endpoint) throws Exception {
        URLConnection connection = new URL(endpoint).openConnection();
        connection.setConnectTimeout(2500);
        connection.setReadTimeout(2500);
        connection.setRequestProperty("User-Agent", "XenAddons Spotify Overlay");
        try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static String sanitizeUrl(String url) {
        if (url == null) return "";
        String trimmed = url.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed;
        return "";
    }

    private interface Resolver {
        String resolve() throws Exception;
    }
}
