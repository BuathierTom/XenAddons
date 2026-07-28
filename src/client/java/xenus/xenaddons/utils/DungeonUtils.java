package xenus.xenaddons.utils;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.network.chat.Component;
import xenus.xenaddons.compat.XenPlayerListCompat;

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
    public static int milestone = -1;
    public static int dungeonSeconds = 0;
    public static boolean inCatacombs = false;
    public static boolean inBoss = false;
    public static boolean isUniqueClass = false;
    private static boolean healerSeenAtMidThisRun = false;
    
    private static final Map<String, String> PLAYER_CLASSES = new HashMap<>();
    private static final Map<java.util.UUID, String> PLAYER_CLASSES_UUID = new HashMap<>();
    private static final Map<java.util.UUID, Integer> PLAYER_CLASS_LEVELS_UUID = new HashMap<>();
    private static int tickCounter = 0;
    
    private static final Pattern CRYPTS_PATTERN = Pattern.compile("Crypts:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DUNGEON_FLOOR_PATTERN =
            Pattern.compile("The Catacombs\\s*\\(([FM])(\\d+)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNIQUE_CLASS_BONUS_PATTERN =
            Pattern.compile("^Your\\s+([A-Za-z]+)\\s+stats are doubled because you are the only player using this class!$", Pattern.CASE_INSENSITIVE);

    public static void reset() {
        floorNumber = 0;
        currentPhase = 0;
        currentStage = 0;
        crypts = 0;
        milestone = -1;
        dungeonSeconds = 0;
        inCatacombs = false;
        inBoss = false;
        isUniqueClass = false;
        healerSeenAtMidThisRun = false;
        PLAYER_CLASSES.clear();
        PLAYER_CLASSES_UUID.clear();
        PLAYER_CLASS_LEVELS_UUID.clear();
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
            if (client.level == null) return;
            tickCounter++;
            updateBossState();
            updateHealerMidSeenState();
            
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        
        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return;

        boolean foundDungeon = false;
        Collection<PlayerScoreEntry> entries = scoreboard.listPlayerScores(objective);
        for (PlayerScoreEntry entry : entries) {
            PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
            Component formattedLine = PlayerTeam.formatNameForTeam(team, entry.ownerName());
            String line = formattedLine.getString().replaceAll("§[0-9a-fk-or]", "").trim();
            
            Matcher floorMatcher = DUNGEON_FLOOR_PATTERN.matcher(line);
            if (floorMatcher.find()) {
                foundDungeon = true;
                try {
                    floorNumber = Integer.parseInt(floorMatcher.group(2));
                } catch (NumberFormatException ignored) {
                }
            } else if (line.contains("Floor VII")) {
                foundDungeon = true;
                floorNumber = 7;
            }

            if (checkTabLine(line)) foundDungeon = true;

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

        if (foundDungeon) {
            inCatacombs = true;
        }
    }

    private static void updateFromTab() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;
        
        boolean foundDungeon = false;
        int foundCrypts = crypts;
        int foundMilestone = milestone;

        // Process List Entries
        for (PlayerInfo entry : mc.getConnection().getListedOnlinePlayers()) {
            Component displayName = entry.getTabListDisplayName();
            String nameString = (displayName != null) ? displayName.getString() : entry.getProfile().name();
            if (checkTabLine(nameString)) foundDungeon = true;
            
            int c = parseCrypts(nameString);
            if (c != -1) foundCrypts = c;

            int m = parseMilestone(nameString);
            if (m != -1) foundMilestone = m;
        }
        
        // Process Header and Footer (Accessor required)
        try {
            Component header = XenPlayerListCompat.getHeader(mc);
            Component footer = XenPlayerListCompat.getFooter(mc);
            
            if (header != null) {
                String h = header.getString();
                if (checkTabLine(h)) foundDungeon = true;
                int c = parseCrypts(h);
                if (c != -1) foundCrypts = c;
                int m = parseMilestone(h);
                if (m != -1) foundMilestone = m;
            }
            if (footer != null) {
                String f = footer.getString();
                if (checkTabLine(f)) foundDungeon = true;
                int c = parseCrypts(f);
                if (c != -1) foundCrypts = c;
                int m = parseMilestone(f);
                if (m != -1) foundMilestone = m;
            }
        } catch (Exception ignored) {}

        if (!foundDungeon) {
            clearDungeonRuntimeState();
        }
        inCatacombs = foundDungeon;
        crypts = foundCrypts;
        milestone = foundDungeon ? foundMilestone : -1;
    }

    private static void clearDungeonRuntimeState() {
        floorNumber = 0;
        currentPhase = 0;
        currentStage = 0;
        dungeonSeconds = 0;
        milestone = -1;
        inBoss = false;
        isUniqueClass = false;
        healerSeenAtMidThisRun = false;
        PLAYER_CLASSES.clear();
        PLAYER_CLASSES_UUID.clear();
        PLAYER_CLASS_LEVELS_UUID.clear();
    }
    
    private static boolean checkTabLine(String text) {
        if (text == null) return false;
        String clean = text.replaceAll("§[0-9a-fk-or]", "").toLowerCase();
        // "Dungeon: Catacombs" can appear outside active runs (profile/account tab info).
        // Only treat as in-dungeon when run-specific stats are present.
        return clean.contains("crypts:")
            || clean.contains("secrets found")
            || clean.contains("completed rooms")
            || clean.contains("opened rooms")
            || clean.contains("puzzles:")
            || clean.contains("team deaths")
            || clean.contains("your milestone:");
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

    private static final Pattern MILESTONE_PATTERN = Pattern.compile("(?i)your milestone:\\s*(.+)$");
    private static final String MILESTONE_SYMBOLS = "⓿❶❷❸❹❺❻❼❽❾";

    private static int parseMilestone(String text) {
        if (text == null) return -1;
        String clean = text.replaceAll("§[0-9a-fk-or]", "").trim();
        Matcher matcher = MILESTONE_PATTERN.matcher(clean);
        if (!matcher.find()) return -1;

        String value = matcher.group(1);
        if (value == null) return -1;
        value = value.trim();
        if (value.isEmpty()) return -1;

        String numeric = value.replaceAll("[^0-9]", "");
        if (!numeric.isEmpty()) {
            try {
                return Integer.parseInt(numeric);
            } catch (Exception ignored) {}
        }

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            int idx = MILESTONE_SYMBOLS.indexOf(ch);
            if (idx >= 0) return idx;
            if (Character.isDigit(ch)) return Character.digit(ch, 10);
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

        Matcher uniqueClass = UNIQUE_CLASS_BONUS_PATTERN.matcher(msg.trim());
        if (uniqueClass.matches()) {
            isUniqueClass = true;
        }
    }

    public static void updateClasses() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;
        
        // 1. Identify real players (ignore Hypixel sorting entries starting with !)
        Map<String, java.util.UUID> realPlayers = new HashMap<>();
        for (PlayerInfo entry : mc.getConnection().getListedOnlinePlayers()) {
            String name = entry.getProfile().name();
            if (name != null && !name.startsWith("!")) {
                 realPlayers.put(name, entry.getProfile().id());
            }
        }

        // 2. Scan tab list for class tokens and match with real players
        for (PlayerInfo entry : mc.getConnection().getListedOnlinePlayers()) {
            Component displayName = entry.getTabListDisplayName();
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
                                int classLevel = parseClassLevel(cleanText, c);
                                if (classLevel > 0) {
                                    PLAYER_CLASS_LEVELS_UUID.put(realPlayer.getValue(), classLevel);
                                }
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

    public static String getPlayerClass(net.minecraft.world.entity.player.Player player) {
        if (player == null) return null;
        return getPlayerClass(player.getUUID());
    }

    public static int getPlayerClassLevel(java.util.UUID uuid) {
        if (uuid == null) return -1;
        return PLAYER_CLASS_LEVELS_UUID.getOrDefault(uuid, -1);
    }

    public static int getPlayerClassLevel(net.minecraft.world.entity.player.Player player) {
        if (player == null) return -1;
        return getPlayerClassLevel(player.getUUID());
    }

    public static boolean isUniqueClass() {
        return isUniqueClass;
    }
    
    public static boolean inDungeon() { return inCatacombs; }

    public static int getMilestone() {
        return milestone;
    }

    public static boolean inBoss() {
        return inDungeon() && (inBoss || currentPhase > 0);
    }

    private static void updateBossState() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !inDungeon()) {
            inBoss = false;
            return;
        }

        // Fallback for clients/mods that hide boss chat lines:
        // entering the P5 vertical range implies Wither King phase.
        if (hasHealerBeenAtMidThisRun()) {
            double y = mc.player.getY();
            if (y > 4.0 && y < 50.0) {
                currentPhase = 5;
            }
        }

        if (currentPhase > 0) {
            inBoss = true;
            return;
        }

        // Fallback while phase text has not arrived yet: F7/M7 boss-map bounds only.
        if (floorNumber == 7) {
            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();
            inBoss = x >= -5.0 && x <= 120.0
                && y >= 3.0 && y <= 230.0
                && z >= 25.0 && z <= 170.0;
            return;
        }

        inBoss = false;
    }
    
    public static boolean isInBox(double x1, double x2, double y1, double y2, double z1, double z2, Entity entity) {
        if (entity == null) return false;
        double minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        double minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        double minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        return entity.getX() >= minX && entity.getX() <= maxX &&
               entity.getY() >= minY && entity.getY() <= maxY &&
               entity.getZ() >= minZ && entity.getZ() <= maxZ;
    }

    public static boolean isHealerAtMidDetected() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || !inDungeon()) return false;

        for (Player player : mc.level.players()) {
            if (player == null || player.isInvisible() || player.isSpectator()) continue;
            String clazz = getPlayerClass(player);
            if (clazz == null) clazz = getPlayerClass(player.getGameProfile().name());
            if (!"Healer".equals(clazz)) continue;
            if (isInBox(47, 61.999, 64, 75, 69, 83.999, player)) return true;
        }
        return false;
    }

    public static boolean hasHealerBeenAtMidThisRun() {
        return healerSeenAtMidThisRun;
    }

    private static void updateHealerMidSeenState() {
        if (!inDungeon()) {
            healerSeenAtMidThisRun = false;
            return;
        }
        if (!healerSeenAtMidThisRun && isHealerAtMidDetected()) {
            healerSeenAtMidThisRun = true;
        }
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return 0;

        Map<String, java.util.UUID> realPlayers = new HashMap<>();
        for (PlayerInfo entry : mc.getConnection().getListedOnlinePlayers()) {
            String name = entry.getProfile().name();
            if (name != null && !name.startsWith("!")) {
                realPlayers.put(name, entry.getProfile().id());
            }
        }

        String[] classes = {"Mage", "Archer", "Berserk", "Tank", "Healer"};
        Set<String> foundPlayers = new HashSet<>();
        int classEntryCount = 0;

        for (PlayerInfo entry : mc.getConnection().getListedOnlinePlayers()) {
            Component displayName = entry.getTabListDisplayName();
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

    public static int getDetectedDungeonClassCount() {
        int count = PLAYER_CLASSES_UUID.size();
        if (count <= 0) count = PLAYER_CLASSES.size();
        if (count < 0) return 0;
        return Math.min(count, 5);
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

    private static int parseClassLevel(String cleanText, String className) {
        if (cleanText == null || className == null) return -1;
        Matcher afterClass = Pattern.compile("\\b" + Pattern.quote(className) + "\\b\\s*([0-9]+|[IVXLCDM]+)\\b", Pattern.CASE_INSENSITIVE).matcher(cleanText);
        if (afterClass.find()) {
            int parsed = parseIntOrRoman(afterClass.group(1));
            if (parsed > 0) return parsed;
        }
        Matcher beforeClass = Pattern.compile("\\b([0-9]+|[IVXLCDM]+)\\b\\s*" + Pattern.quote(className) + "\\b", Pattern.CASE_INSENSITIVE).matcher(cleanText);
        if (beforeClass.find()) {
            int parsed = parseIntOrRoman(beforeClass.group(1));
            if (parsed > 0) return parsed;
        }
        return -1;
    }

    private static int parseIntOrRoman(String token) {
        if (token == null || token.isBlank()) return -1;
        try {
            return Integer.parseInt(token);
        } catch (Exception ignored) {
        }
        return romanToInt(token);
    }

    private static int romanToInt(String roman) {
        if (roman == null || roman.isBlank()) return -1;
        String upper = roman.toUpperCase();
        int total = 0;
        int prev = 0;
        for (int i = upper.length() - 1; i >= 0; i--) {
            int curr = switch (upper.charAt(i)) {
                case 'I' -> 1;
                case 'V' -> 5;
                case 'X' -> 10;
                case 'L' -> 50;
                case 'C' -> 100;
                case 'D' -> 500;
                case 'M' -> 1000;
                default -> -1;
            };
            if (curr < 0) return -1;
            if (curr < prev) total -= curr;
            else total += curr;
            prev = curr;
        }
        return total;
    }

    static {
        // Phase checks disabled: allow position messages any time regardless of boss phase/stage.
        positionDefinitions.add(new PositionDefinition("AtP2", "At P2!", (clazz) -> true, (entity) -> entity.getY() < 205 && entity.getY() > 164, "at p2", "in p2"));
        positionDefinitions.add(new PositionDefinition("AtSS", "At SS!", (clazz) -> true, (entity) -> isInBox(106, 110, 118, 122, 92, 96, entity), "at ss", "at simon says", "at ssc"));
        positionDefinitions.add(new PositionDefinition("AtEE2", "At Pre Enter 2!", (clazz) -> true, (entity) -> isInBox(49, 58, 108, 115, 129, 133, entity), "early enter 2", "pre enter 2", "at ee2", "entered 3.2"));
        positionDefinitions.add(new PositionDefinition("AtEE2High", "At Pre Enter 2 (High)!", (clazz) -> true, (entity) -> isInBox(62, 59, 132, 135, 143, 138, entity), "high pre enter 2", "high ee2", "ee2 high", "early enter high", "high early enter 2", "pre enter 2 (high)"));
        positionDefinitions.add(new PositionDefinition("AtSafespot2", "At Early Enter 2 Safe Spot!", (clazz) -> true, (entity) -> {
            double radiusSq = 0.4999 * 0.4999;
            return entity.distanceToSqr(69.5, 109.0, 121.5) <= radiusSq
                    || entity.distanceToSqr(67.5, 109.0, 121.5) <= radiusSq
                    || entity.distanceToSqr(48.5, 109.0, 121.5) <= radiusSq
                    || entity.distanceToSqr(46.5, 109.0, 121.5) <= radiusSq;
        }, "2 safespot", "safespot 2", "early enter 2 safe spot"));
        positionDefinitions.add(new PositionDefinition("AtSafespot2High", "At Early Enter 2 Safe Spot! (High)", (clazz) -> true, (entity) -> isInBox(63, 66, 132, 133, 146, 147, entity), "2 safespot high", "safespot 2 high", "early enter 2 safe spot high"));
        positionDefinitions.add(new PositionDefinition("AtEE3", "At Pre Enter 3!", (clazz) -> true, (entity) -> isInBox(0, 4, 108, 115, 98, 107, entity), "early enter 3", "pre enter 3", "at ee3", "entered 3.3"));
        positionDefinitions.add(new PositionDefinition("AtSafespot3", "At Early Enter 3 Safe Spot!", (clazz) -> true, (entity) -> {
            double radiusSq = 0.45 * 0.45;
            for (double z = 91.5; z <= 99.5; z += 1.0) {
                if (entity.distanceToSqr(18.5, 121.5, z) <= radiusSq) return true;
            }
            return false;
        }, "safespot 3", "3 safespot", "early enter 3 safe spot"));
        positionDefinitions.add(new PositionDefinition("AtArrowsDevice", "At Arrows Device!", (clazz) -> true, (entity) -> entity.distanceToSqr(-0.5, 120.0, 77.5) <= (1.5 * 1.5), "at arrows device"));
        positionDefinitions.add(new PositionDefinition("EnteringCore", "Entering Core!", (clazz) -> true, (entity) -> entity.distanceToSqr(58.0, 120.0, 122.0) <= (0.75 * 0.75), "entering core"));
        positionDefinitions.add(new PositionDefinition("AtCore", "At Core!", (clazz) -> true, (entity) -> isInBox(52, 57, 113, 117, 49, 53, entity), "at core", "pre enter 4", "early enter 4", "at ee4", "entered 3.4"));
        positionDefinitions.add(new PositionDefinition("InGoldorTunnel", "Inside Goldor Tunnel!", (clazz) -> true, (entity) -> isInBox(41, 68, 110, 150, 59, 117, entity), "in goldor tunnel", "inside goldor tunnel", "in core", "entered 3.5", "at ee5", "pre enter 5"));
        positionDefinitions.add(new PositionDefinition("AtPewPewDev", "At Pew Pew Dev!", (clazz) -> true, (entity) -> entity.distanceToSqr(63.5, 127.0, 35.5) <= (1.5 * 1.5), "at pew pew dev"));
        positionDefinitions.add(new PositionDefinition("AtMid", "At Mid!", (clazz) -> true, (entity) -> isInBox(47, 61.999, 64, 75, 69, 83.999, entity), "at mid", "in mid"));
        positionDefinitions.add(new PositionDefinition("AtPre4Entry", "At Pre4 Entry!", (clazz) -> true, (entity) -> isInBox(91, 93, 129, 133, 44, 46, entity), "i4 entry", "pre4 entry"));
        positionDefinitions.add(new PositionDefinition("AtP5", "At P5!", (clazz) -> "Healer".equals(clazz), (entity) -> entity.getY() < 50 && entity.getY() > 4, "at p5", "in p5"));
    }
}
