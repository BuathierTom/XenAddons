package xenus.xenaddons.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GoatedPlayersManager {
    
    // Internal hardcoded list - Only editable via source code
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
        "antorak"
    ));

    public static boolean isGoated(String name) {
        return GOATED_PLAYERS.contains(name.toLowerCase());
    }
    
    // No initialization needed logic-wise, but kept empty if needed for consistency, 
    // though the user wants to revert to "no json", so I'll follow the previous "internal" implementation pattern.
    public static void init() {
        // No-op
    }
}
