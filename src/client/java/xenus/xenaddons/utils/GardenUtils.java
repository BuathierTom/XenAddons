package xenus.xenaddons.utils;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.Collection;
import java.util.regex.Pattern;

public final class GardenUtils {

    private static final Pattern AREA_PATTERN =
        Pattern.compile("^(?:Area|Location):\\s*(?:The\\s+)?Garden$", Pattern.CASE_INSENSITIVE);
    private static final String LOCATION_MARKER = "⏣";
    private static final String GARDEN_NAME = "Garden";
    private static final int REFRESH_TICKS = 10;

    private static volatile boolean inGarden = false;
    private static int tickCounter = 0;

    private GardenUtils() {
    }

    public static boolean inGarden() {
        return inGarden;
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (++tickCounter < REFRESH_TICKS) return;
            tickCounter = 0;
            inGarden = detect(client);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> inGarden = false);
    }

    private static boolean detect(Minecraft client) {
        if (client == null || client.level == null || client.player == null) return false;
        return sidebarSaysGarden(client) || tabSaysGarden(client);
    }

    private static boolean sidebarSaysGarden(Minecraft client) {
        Scoreboard scoreboard = client.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return false;

        Collection<PlayerScoreEntry> entries = scoreboard.listPlayerScores(objective);
        for (PlayerScoreEntry entry : entries) {
            PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
            Component formatted = PlayerTeam.formatNameForTeam(team, entry.ownerName());
            String line = strip(formatted.getString());
            if (line.contains(LOCATION_MARKER) && line.contains(GARDEN_NAME)) return true;
        }
        return false;
    }

    private static boolean tabSaysGarden(Minecraft client) {
        if (client.getConnection() == null) return false;

        for (PlayerInfo info : client.getConnection().getListedOnlinePlayers()) {
            Component display = info.getTabListDisplayName();
            if (display == null) continue;
            if (AREA_PATTERN.matcher(strip(display.getString())).matches()) return true;
        }
        return false;
    }

    private static String strip(String input) {
        if (input == null) return "";
        return input.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
    }
}
