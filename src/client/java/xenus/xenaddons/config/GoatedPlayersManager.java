package xenus.xenaddons.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GoatedPlayersManager {

    // Keep GOATED_PLAYERS for chat commands by name
    private static final Set<String> GOATED_PLAYERS = new HashSet<>(Arrays.asList(
        "xenus",
        "xen",
        "xen62",
        "dtxenus",
        "dtnyro",
        "fxhz",
        "ziimou",
        "e0le_",
        "avgcata12",
        "antorak",
        "icantarch"
    ));

    public static boolean isGoated(String name) {
        return GOATED_PLAYERS.contains(name.toLowerCase());
    }

    public static void init() {
        // No-op
    }
}
