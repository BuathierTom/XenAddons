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

public class DungeonUtils {
    
    public static int floorNumber = 0;
    public static int currentPhase = 0;
    public static int currentStage = 0;
    public static int crypts = 0;
    public static int dungeonSeconds = 0;
    public static boolean inCatacombs = false;
    
    private static final Map<String, String> PLAYER_CLASSES = new HashMap<>();
    private static int tickCounter = 0;
    
    private static final Pattern CRYPTS_PATTERN = Pattern.compile("Crypts:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

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

    public static void reset() {
        floorNumber = 0;
        currentPhase = 0;
        currentStage = 0;
        crypts = 0;
        dungeonSeconds = 0;
        inCatacombs = false;
        PLAYER_CLASSES.clear();
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
        
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            Text displayName = entry.getDisplayName();
            String name = entry.getProfile().name();
            if (displayName == null) continue;
            
            String text = displayName.getString();
            String[] classes = {"Mage", "Archer", "Berserk", "Tank", "Healer"};
            for (String c : classes) {
                if (text.contains("(" + c) || text.contains("[" + c)) {
                    PLAYER_CLASSES.put(name, c);
                    break;
                }
            }
        }
    }
    
    public static String getPlayerClass(String playerName) {
        if (PLAYER_CLASSES.containsKey(playerName)) return PLAYER_CLASSES.get(playerName);
        return (floorNumber > 0 || inCatacombs) ? "Mage" : null;
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
        positionDefinitions.add(new PositionDefinition("AtSS", "At SS!", (clazz) -> true, (entity) -> isInBox(106, 110, 118, 122, 92, 96, entity), "at ss", "at ssc"));
        positionDefinitions.add(new PositionDefinition("AtEE2", "At Pre Enter 2!", (clazz) -> true, (entity) -> isInBox(49, 58, 108, 115, 129, 133, entity), "pre enter 2", "at ee2"));
        positionDefinitions.add(new PositionDefinition("AtMid", "At Mid!", (clazz) -> true, (entity) -> isInBox(47, 61, 64, 75, 69, 83, entity), "at mid", "in mid"));
    }
}
