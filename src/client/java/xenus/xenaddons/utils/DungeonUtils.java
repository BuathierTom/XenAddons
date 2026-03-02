package xenus.xenaddons.utils;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.text.Text;
import xenus.xenaddons.mixin.client.AccessorPlayerListHud;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.util.Set;

public class DungeonUtils {
    
    public static int floorNumber = 0;
    public static int currentPhase = 0;
    public static int currentStage = 0;
    public static int crypts = 0;
    public static int dungeonSeconds = 0;
    public static boolean inCatacombs = false;
    
    private static final Map<String, String> PLAYER_CLASSES = new HashMap<>();
    private static final Map<java.util.UUID, String> PLAYER_CLASSES_UUID = new HashMap<>();
    private static int tickCounter = 0;
    
    private static final Pattern CRYPTS_PATTERN = Pattern.compile("Crypts:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    public static void reset() {
        floorNumber = 0;
        currentPhase = 0;
        currentStage = 0;
        crypts = 0;
        dungeonSeconds = 0;
        inCatacombs = false;
        PLAYER_CLASSES.clear();
        PLAYER_CLASSES_UUID.clear();
    }

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            handleChat(message.getString());
        });
        
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            handleChat(message.getString());
            return true;
        });

        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            reset();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null) return;
            tickCounter++;
            
            if (tickCounter % 20 == 0) { // Every 1 second
                updateFromTab();
                updateFromScoreboard();
                if (inDungeon()) {
                    updateClasses();
                }
            }
        });

        // Reset dungeon state when joining a world to prevent stale state issues
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            reset();
        });
    }

    private static void updateFromScoreboard() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        
        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) return;

        Collection<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(objective);
        for (ScoreboardEntry entry : entries) {
            String line = entry.name().getString().replaceAll("§[0-9a-fk-or]", "");
            
            if (line.contains("The Catacombs (F7)") || line.contains("Floor VII")) {
                floorNumber = 7;
            }

            if (line.contains("Time: ") || (line.contains(":") && !line.contains("Party") && !line.contains("Guild"))) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+):(\\d+)").matcher(line);
                if (m.find()) {
                    try {
                        int mins = Integer.parseInt(m.group(1));
                        int secs = Integer.parseInt(m.group(2));
                        dungeonSeconds = mins * 60 + secs;
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    private static void updateFromTab() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return;
        
        boolean foundDungeon = false;
        int foundCrypts = crypts;

        // Process List Entries
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            Text displayName = entry.getDisplayName();
            String nameString = (displayName != null) ? displayName.getString() : entry.getProfile().name();
            if (checkTabLine(nameString)) foundDungeon = true;
            
            int c = parseCrypts(nameString);
            if (c != -1) foundCrypts = c;
        }
        
        // Process Header and Footer (Accessor required)
        try {
            AccessorPlayerListHud accessor = (AccessorPlayerListHud) mc.inGameHud.getPlayerListHud();
            Text header = accessor.getHeader();
            Text footer = accessor.getFooter();
            
            if (header != null) {
                String h = header.getString();
                if (checkTabLine(h)) foundDungeon = true;
                int c = parseCrypts(h);
                if (c != -1) foundCrypts = c;
            }
            if (footer != null) {
                String f = footer.getString();
                if (checkTabLine(f)) foundDungeon = true;
                int c = parseCrypts(f);
                if (c != -1) foundCrypts = c;
            }
        } catch (Exception ignored) {}
        
        inCatacombs = foundDungeon;
        crypts = foundCrypts;
    }
    
    private static boolean checkTabLine(String text) {
        if (text == null) return false;
        String clean = text.replaceAll("§[0-9a-fk-or]", "").toLowerCase();
        return clean.contains("dungeon:") || clean.contains("catacombs");
    }
    
    private static int parseCrypts(String text) {
        if (text == null) return -1;
        String clean = text.replaceAll("§[0-9a-fk-or]", "");
        Matcher matcher = CRYPTS_PATTERN.matcher(clean);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (Exception ignored) {}
        }
        return -1;
    }

    private static void handleChat(String cleanMsg) {
        String msg = cleanMsg.replaceAll("§[0-9a-fk-or]", "");

        if (msg.contains("The catacombs - Floor VII")) {
            floorNumber = 7;
        }

        if (floorNumber != 7) return;

        if (msg.contains("[BOSS] Storm: I should have known that I stood no chance.")) currentStage = 1;
        if (msg.contains("activated a terminal!") || msg.contains("completed a terminal!") ||
            msg.contains("activated a device!") || msg.contains("completed a device!") ||
            msg.contains("activated a lever!") || msg.contains("completed a lever!")) {
            if (msg.contains("(7/7)") || msg.contains("(8/8)")) currentStage += 1;
        }
        if (msg.contains("The Core entrance is opening!")) currentStage = 5;

        // Phase Detection
        if (msg.contains("[BOSS] Maxor:")) currentPhase = 1;
        else if (msg.contains("[BOSS] Storm:")) currentPhase = 2;
        else if (msg.contains("[BOSS] Goldor:")) currentPhase = 3;
        else if (msg.contains("[BOSS] Necron:")) currentPhase = 4;
        else if (msg.contains("[BOSS] Wither King:")) currentPhase = 5;
        
        if (msg.contains("is now ready!")) {
            reset(); 
        }
    }

    public static void updateClasses() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return;
        
        // 1. Identify real players (ignore Hypixel sorting entries starting with !)
        Map<String, java.util.UUID> realPlayers = new HashMap<>();
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            String name = entry.getProfile().name();
            if (name != null && !name.startsWith("!")) {
                 realPlayers.put(name, entry.getProfile().id());
            }
        }

        // 2. Scan tab list for class tokens and match with real players
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            Text displayName = entry.getDisplayName();
            if (displayName == null) continue;
            
            String text = displayName.getString();
            if (text == null) continue;

            String cleanText = text.replaceAll("§[0-9a-fk-or]", "");
            String[] classes = {"Mage", "Archer", "Berserk", "Tank", "Healer"};
            
            for (String c : classes) {
                if (cleanText.contains(c)) {
                    // This tab entry contains a class definition.
                    // Check if any real player's name is inside this text.
                    for (Map.Entry<String, java.util.UUID> realPlayer : realPlayers.entrySet()) {
                        String realName = realPlayer.getKey();
                        if (cleanText.contains(realName)) {
                            PLAYER_CLASSES.put(realName, c);
                            if (realPlayer.getValue() != null) {
                                PLAYER_CLASSES_UUID.put(realPlayer.getValue(), c);
                            }
                        }
                    }
                    // Also map the direct profile name just in case (e.g. !A-b -> Archer)
                    String profileName = entry.getProfile().name();
                    PLAYER_CLASSES.put(profileName, c);
                    break;
                }
            }
        }
    }
    
    public static String getPlayerClass(String playerName) {
        if (PLAYER_CLASSES.containsKey(playerName)) return PLAYER_CLASSES.get(playerName);
        return null;
    }

    public static String getPlayerClass(java.util.UUID uuid) {
        if (PLAYER_CLASSES_UUID.containsKey(uuid)) return PLAYER_CLASSES_UUID.get(uuid);
        return null;
    }

    public static String getPlayerClass(net.minecraft.entity.player.PlayerEntity player) {
        if (player == null) return null;
        return getPlayerClass(player.getUuid());
    }
    
    public static boolean inDungeon() { return floorNumber > 0 || inCatacombs; }
    
    public static boolean isInBox(double x1, double x2, double y1, double y2, double z1, double z2, Entity entity) {
        if (entity == null) return false;
        double minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        double minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        double minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        return entity.getX() >= minX && entity.getX() <= maxX &&
               entity.getY() >= minY && entity.getY() <= maxY &&
               entity.getZ() >= minZ && entity.getZ() <= maxZ;
    }
    
    public static String getClassColor(String className) {
        if (className == null) return "§f";
        switch (className) {
            case "Healer": return "§d";
            case "Tank": return "§a";
            case "Mage": return "§b";
            case "Berserk": return "§c";
            case "Archer": return "§6";
            default: return "§f";
        }
    }

    public static int getDungeonPartyCountFromTab() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return 0;

        Map<String, java.util.UUID> realPlayers = new HashMap<>();
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            String name = entry.getProfile().name();
            if (name != null && !name.startsWith("!")) {
                realPlayers.put(name, entry.getProfile().id());
            }
        }

        String[] classes = {"Mage", "Archer", "Berserk", "Tank", "Healer"};
        Set<String> foundPlayers = new HashSet<>();
        int classEntryCount = 0;

        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            Text displayName = entry.getDisplayName();
            if (displayName == null) continue;

            String cleanText = displayName.getString().replaceAll("§[0-9a-fk-or]", "");
            boolean classLine = false;
            for (String c : classes) {
                if (cleanText.contains(c)) {
                    classLine = true;
                    break;
                }
            }
            if (!classLine) continue;
            classEntryCount++;

            for (String realName : realPlayers.keySet()) {
                if (cleanText.contains(realName)) {
                    foundPlayers.add(realName);
                }
            }
        }

        // Prefer class lines in dungeon tab (works even if profile names are obfuscated or nicked).
        int fromClassLines = Math.max(foundPlayers.size(), classEntryCount);
        if (fromClassLines > 0 && fromClassLines <= 5) {
            return fromClassLines;
        }

        // Fallback: real visible players in this instance.
        int realCount = realPlayers.size();
        if (realCount > 0 && realCount <= 5) {
            return realCount;
        }

        return foundPlayers.size();
    }
    
    public static boolean inPhase(int phase) { return currentPhase == phase; }
    public static boolean inPhase(int[] phases) {
        for (int p : phases) if (currentPhase == p) return true;
        return false;
    }
    public static boolean inStage(int stage) { return currentStage == stage; }
    public static boolean inStage(int[] stages) {
        for (int s : stages) if (currentStage == s) return true;
        return false;
    }
    
    public static class PositionDefinition {
        public String id, messageText;
        public java.util.function.Function<String, Boolean> checkCondition;
        public java.util.function.Function<Entity, Boolean> checkPosition;
        public String[] validMessages;
        public PositionDefinition(String id, String messageText, java.util.function.Function<String, Boolean> checkCondition, java.util.function.Function<Entity, Boolean> checkPosition, String... validMessages) {
            this.id = id; this.messageText = messageText; this.checkCondition = checkCondition; this.checkPosition = checkPosition; this.validMessages = validMessages;
        }
    }

    public static final java.util.List<PositionDefinition> positionDefinitions = new java.util.ArrayList<>();

    static {
        positionDefinitions.add(new PositionDefinition("AtP2", "At P2!", (clazz) -> (currentPhase == 1 || currentPhase == 0) && !"Healer".equals(clazz), (entity) -> entity.getY() < 205 && entity.getY() > 164, "at p2", "in p2"));
        positionDefinitions.add(new PositionDefinition("AtSS", "At SS!", (clazz) -> inPhase(new int[]{2, 3}), (entity) -> isInBox(106, 110, 118, 122, 92, 96, entity), "at ss", "at simon says", "at ssc"));
        positionDefinitions.add(new PositionDefinition("AtEE2", "At Pre Enter 2!", (clazz) -> inStage(1), (entity) -> isInBox(49, 58, 108, 115, 129, 133, entity), "early enter 2", "pre enter 2", "at ee2", "entered 3.2"));
        positionDefinitions.add(new PositionDefinition("AtEE2High", "At Pre Enter 2 (High)!", (clazz) -> inStage(1), (entity) -> isInBox(62, 59, 132, 135, 143, 138, entity), "high pre enter 2", "high ee2", "ee2 high", "early enter high", "high early enter 2", "pre enter 2 (high)"));
        positionDefinitions.add(new PositionDefinition("AtSafespot2", "At Safespot 2!", (clazz) -> inStage(1), (entity) -> isInBox(46, 49, 109, 109, 121.987, 121.988, entity), "2 safespot", "safespot 2"));
        positionDefinitions.add(new PositionDefinition("AtEE3", "At Pre Enter 3!", (clazz) -> inStage(2), (entity) -> isInBox(0, 4, 108, 115, 98, 107, entity), "early enter 3", "pre enter 3", "at ee3", "entered 3.3"));
        positionDefinitions.add(new PositionDefinition("AtSafespot3", "At Safespot 3!", (clazz) -> inStage(2), (entity) -> isInBox(19, 18, 121.5, 125, 91, 100, entity), "safespot 3", "3 safespot"));
        positionDefinitions.add(new PositionDefinition("AtCore", "At Core!", (clazz) -> inPhase(new int[]{2, 3}), (entity) -> isInBox(52, 56, 113, 117, 49, 53, entity), "at core", "pre enter 4", "early enter 4", "at ee4", "entered 3.4"));
        positionDefinitions.add(new PositionDefinition("InGoldorTunnel", "Inside Goldor Tunnel!", (clazz) -> inStage(4), (entity) -> isInBox(41, 68, 110, 150, 59, 117, entity), "in goldor tunnel", "inside goldor tunnel", "in core", "entered 3.5", "at ee5", "pre enter 5"));
        positionDefinitions.add(new PositionDefinition("AtMid", "At Mid!", (clazz) -> inPhase(4), (entity) -> isInBox(47, 61, 64, 75, 69, 83, entity), "at mid", "in mid"));
        positionDefinitions.add(new PositionDefinition("AtPre4Entry", "At Pre4 Entry!", (clazz) -> inPhase(2) && !"Healer".equals(clazz), (entity) -> isInBox(91, 93, 129, 133, 44, 46, entity), "i4 entry", "pre4 entry"));
        positionDefinitions.add(new PositionDefinition("AtP5", "At P5!", (clazz) -> inPhase(4) && "Healer".equals(clazz), (entity) -> entity.getY() < 50 && entity.getY() > 4, "at p5", "in p5"));
    }
}
