package xenus.xenaddons.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;

/**
 * Rebuilds the client-side command tree on demand.
 *
 * Fabric merges our client commands into the tree the server sends, and it evaluates every node's
 * requires() predicate once, at merge time. So a command hidden behind a toggle keeps its old
 * visibility in autocomplete until the next command tree arrives. Replaying the last packet re-runs
 * that merge, which re-evaluates the predicates and refreshes the suggestions immediately.
 */
public final class CommandTreeRefresher {

    private static ClientboundCommandsPacket lastPacket;

    private CommandTreeRefresher() {
    }

    public static void remember(ClientboundCommandsPacket packet) {
        lastPacket = packet;
    }

    /** Must run on the client thread; a no-op before the first command tree arrives. */
    public static void refresh() {
        ClientboundCommandsPacket packet = lastPacket;
        if (packet == null) return;

        Minecraft client = Minecraft.getInstance();
        // Config load fires setting listeners before the game loop exists; both checks cover it.
        if (client == null || !client.isSameThread()) return;

        ClientPacketListener connection = client.getConnection();
        if (connection == null) return;

        connection.handleCommands(packet);
    }
}
