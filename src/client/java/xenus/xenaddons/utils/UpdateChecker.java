package xenus.xenaddons.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import xenus.xenaddons.XenAddons;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

public final class UpdateChecker {
    private static final String MODRINTH_PROJECT_SLUG = "xenaddons";
    private static final String MODRINTH_PROJECT_PAGE = "https://modrinth.com/mod/xenaddons";
    private static final String DISCORD_INVITE = "https://discord.com/invite/yRbJJwGTPG";
    private static final String MODRINTH_VERSIONS_API =
        "https://api.modrinth.com/v2/project/" + MODRINTH_PROJECT_SLUG + "/version";

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    private static volatile boolean checkedThisLaunch = false;

    private UpdateChecker() {}

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (checkedThisLaunch) return;
            checkedThisLaunch = true;

            ChatUtils.info("Checking for update...");
            Thread.startVirtualThread(UpdateChecker::checkNow);
        });
    }

    private static void checkNow() {
        try {
            String currentVersion = FabricLoader.getInstance()
                .getModContainer("xenaddons")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("0.0.0");

            String latestVersion = fetchLatestCompatibleVersion();
            if (latestVersion == null || latestVersion.isBlank()) {
                warning("Unable to check updates right now.");
                return;
            }

            int cmp = compareVersions(currentVersion, latestVersion);
            if (cmp < 0) {
                updateReminder(currentVersion, latestVersion);
            } else {
                success("No update available. You are on the latest version (v" + currentVersion + ").");
            }
        } catch (Exception e) {
            XenAddons.LOGGER.warn("Update check failed", e);
            warning("Unable to check updates right now.");
        } finally {
            discordInvite();
        }
    }

    private static String fetchLatestCompatibleVersion() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MODRINTH_VERSIONS_API))
                .header("User-Agent", "XenAddons/" + getFriendlyVersion())
                .GET()
                .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) return null;

            JsonElement root = JsonParser.parseString(response.body());
            if (!root.isJsonArray()) return null;

            String gameVersion = FabricLoader.getInstance().getModContainer("minecraft")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("");

            for (JsonElement el : root.getAsJsonArray()) {
                if (!el.isJsonObject()) continue;
                var obj = el.getAsJsonObject();

                if (!obj.has("version_number")) continue;
                if (obj.has("version_type")
                    && !"release".equalsIgnoreCase(obj.get("version_type").getAsString())) continue;

                boolean loaderOk = false;
                if (obj.has("loaders") && obj.get("loaders").isJsonArray()) {
                    for (JsonElement loader : obj.getAsJsonArray("loaders")) {
                        if (loader.isJsonPrimitive() && "fabric".equalsIgnoreCase(loader.getAsString())) {
                            loaderOk = true;
                            break;
                        }
                    }
                }
                if (!loaderOk) continue;

                if (!gameVersion.isBlank() && obj.has("game_versions") && obj.get("game_versions").isJsonArray()) {
                    boolean gameOk = false;
                    for (JsonElement gv : obj.getAsJsonArray("game_versions")) {
                        if (!gv.isJsonPrimitive()) continue;
                        if (gameVersion.equals(gv.getAsString())) {
                            gameOk = true;
                            break;
                        }
                    }
                    if (!gameOk) continue;
                }

                return obj.get("version_number").getAsString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String getFriendlyVersion() {
        return FabricLoader.getInstance()
            .getModContainer("xenaddons")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
    }

    // Simple numeric-aware compare for versions like 1.2.1
    private static int compareVersions(String a, String b) {
        String[] ap = normalizeVersion(a).split("\\.");
        String[] bp = normalizeVersion(b).split("\\.");
        int len = Math.max(ap.length, bp.length);
        for (int i = 0; i < len; i++) {
            int ai = i < ap.length ? parseIntSafe(ap[i]) : 0;
            int bi = i < bp.length ? parseIntSafe(bp[i]) : 0;
            if (ai != bi) return Integer.compare(ai, bi);
        }
        return 0;
    }

    private static int parseIntSafe(String part) {
        try {
            return Integer.parseInt(part);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String normalizeVersion(String v) {
        if (v == null) return "0";
        String cleaned = v.toLowerCase(Locale.ROOT).replaceAll("[^0-9.]", "");
        return cleaned.isBlank() ? "0" : cleaned;
    }

    private static void success(String message) {
        runOnClientThread(() -> ChatUtils.success(message));
    }

    private static void warning(String message) {
        runOnClientThread(() -> ChatUtils.warning(message));
    }

    private static void updateReminder(String currentVersion, String latestVersion) {
        runOnClientThread(() -> {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.player == null) return;

            MutableComponent message = Component.literal(
                ChatUtils.PREFIX + "§eUpdate available: v" + latestVersion + " (current: v" + currentVersion + ") "
            );
            MutableComponent link = Component.literal("CLICK TO DOWNLOAD");
            link.setStyle(link.getStyle()
                .withColor(ChatFormatting.YELLOW)
                .withBold(true)
                .withUnderlined(false)
                .withClickEvent(new ClickEvent.OpenUrl(URI.create(MODRINTH_PROJECT_PAGE)))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("§e" + MODRINTH_PROJECT_PAGE))));
            message.append(link);
            client.player.sendSystemMessage(message);
        });
    }

    private static void discordInvite() {
        runOnClientThread(() -> {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.player == null) return;

            MutableComponent message = Component.literal(ChatUtils.PREFIX + "§eJoin the Discord: ");
            MutableComponent link = Component.literal("CLICK TO JOIN");
            link.setStyle(link.getStyle()
                .withColor(ChatFormatting.YELLOW)
                .withBold(true)
                .withUnderlined(false)
                .withClickEvent(new ClickEvent.OpenUrl(URI.create(DISCORD_INVITE)))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("§e" + DISCORD_INVITE))));
            message.append(link);
            client.player.sendSystemMessage(message);
        });
    }

    private static void runOnClientThread(Runnable runnable) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) client.execute(runnable);
    }
}
