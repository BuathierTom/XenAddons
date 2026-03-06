package xenus.xenaddons.features.impl.dungeons;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.utils.ChatUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DungeonsStatsPFModule extends Module {
    private static final int MIN_STATS_LINE_WIDTH = 36;
    private static final Pattern COLOR_PATTERN = Pattern.compile("§[0-9A-FK-ORa-fk-or]");
    private static final Pattern PF_JOIN_PATTERN = Pattern.compile(
            "^Party Finder\\s*>\\s*(.+?)\\s+joined the dungeon group!\\s*\\((\\w+) Level (\\d+)\\)$"
    );
    private static final Pattern PF_CLASS_CHANGE_PATTERN = Pattern.compile(
            "^Party Finder\\s*>\\s*(.+?)\\s+set their class to\\s+(\\w+) Level (\\d+)!$"
    );
    private static final Pattern MINECRAFT_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_]{2,16}");
    private static final String ODIN_HYPIXEL_API = "https://api.odtheking.com/hypixel/get/";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final String MINECRAFT_SERVICES_UUID_API = "https://api.minecraftservices.com/minecraft/profile/lookup/name/";
    private static final Set<String> ALLOWED_PF_PETS = Set.of("EDRAG", "GDRAG", "JELLYFISH", "PHOENIX", "SPIRIT");

    private static final Map<String, String> UUID_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> DUNGEON_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Long> RECENT_TRIGGER = new ConcurrentHashMap<>();
    private static boolean listenersRegistered = false;
    public final BooleanSetting displayStats = new BooleanSetting("Display stats", true);
    public final BooleanSetting betterJoin = new BooleanSetting("Better Join", true);

    public DungeonsStatsPFModule() {
        super("Better Party Finder", "Show dungeon stats when players join through Party Finder", Category.DUNGEON);
        this.registerSetting(displayStats);
        this.registerSetting(betterJoin);

        if (!listenersRegistered) {
            listenersRegistered = true;
            ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
                return handleIncomingMessage(message);
            });
            ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
                return handleIncomingMessage(message);
            });
        }
    }

    private static boolean handleIncomingMessage(Text message) {
        DungeonsStatsPFModule module = ModuleManager.getModule(DungeonsStatsPFModule.class);
        if (module == null || !module.isEnabled() || message == null) return true;
        String rawMessage = message.getString();
        if (rawMessage == null || rawMessage.isBlank()) return true;

        String clean = COLOR_PATTERN.matcher(rawMessage).replaceAll("").trim();
        Matcher matcher = PF_JOIN_PATTERN.matcher(clean);
        if (matcher.matches()) {
            String playerFormatted = matcher.group(1);
            String clazz = matcher.group(2);
            String level = matcher.group(3);
            String playerName = extractPlayerName(playerFormatted);

            if (playerName != null && !playerName.isBlank() && module.displayStats.getValue()) {
                if (module.mc.player == null || !playerName.equalsIgnoreCase(module.mc.player.getGameProfile().name())) {
                    long now = System.currentTimeMillis();
                    Long last = RECENT_TRIGGER.put(playerName, now);
                    if (last == null || now - last >= 1800L) {
                        Thread.startVirtualThread(() -> fetchAndPrint(playerName));
                    }
                }
            }

            if (module.betterJoin.getValue()) {
                postChat(buildBetterJoinMessage(playerFormatted, clazz, level, playerName));
                return false;
            }
            return true;
        }

        if (module.betterJoin.getValue()) {
            Matcher classChange = PF_CLASS_CHANGE_PATTERN.matcher(clean);
            if (classChange.matches()) {
                String player = classChange.group(1);
                String clazz = classChange.group(2);
                String level = classChange.group(3);
                postChat(Text.literal("§d§lPF > §r" + player + " §echanged to §b" + clazz + " " + level + "§e!"));
                return false;
            }

            if ("Party Finder > Your party has been queued in the dungeon finder!".equals(clean)) {
                postChat(Text.literal("§d§lPF > §aParty Queued."));
                return false;
            }
            if ("Party Finder > Your group has been de-listed!".equals(clean)) {
                postChat(Text.literal("§d§lPF > §aParty Delisted.."));
                return false;
            }
        }
        return true;
    }

    public static void debugTrigger(String playerName) {
        if (playerName == null || playerName.isBlank()) return;
        Thread.startVirtualThread(() -> {
            postChat(buildBetterJoinMessage(playerName, "Mage", "0", playerName));
            fetchAndPrint(playerName);
        });
    }

    private static Text buildBetterJoinMessage(String playerFormatted, String clazz, String level, String playerName) {
        MutableText msg = Text.literal("§d§lPF > §r" + playerFormatted + " §8| §b" + clazz + " " + level);
        String self = mcRef() != null && mcRef().player != null ? mcRef().player.getGameProfile().name() : "";
        if (playerName == null || playerName.isBlank() || playerName.equalsIgnoreCase(self)) {
            return msg;
        }
        msg.append(clickableSegment(" §8| §c[Kick]", "/p kick " + playerName, "§c/p kick " + playerName));
        msg.append(clickableSegment(" §7[Block]", "/block add " + playerName, "§7/block add " + playerName));
        msg.append(clickableSegment(" §d[PV]", "/pv " + playerName, "§d/pv " + playerName));
        return msg;
    }

    private static Text clickableSegment(String label, String command, String hover) {
        MutableText out = Text.literal(label);
        out.setStyle(out.getStyle()
                .withClickEvent(new ClickEvent.RunCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(Text.literal(hover))));
        return out;
    }

    private static String extractPlayerName(String raw) {
        if (raw == null || raw.isBlank()) return null;
        Matcher matcher = MINECRAFT_NAME_PATTERN.matcher(raw);
        String last = null;
        while (matcher.find()) {
            last = matcher.group();
        }
        return last;
    }

    private static void fetchAndPrint(String playerName) {
        try {
            JsonObject stats = getDungeonStats(playerName);
            if (stats == null) {
                chat("§7PF Stats §8> §cFailed to fetch stats for §e" + playerName);
                return;
            }

            JsonObject dungeons = getObj(stats, "dungeons");
            if (dungeons == null) {
                chat("§7PF Stats §8> §cNo dungeon data for §e" + playerName);
                return;
            }

            JsonObject catacombs = getObj(dungeons, "catacombs");
            JsonObject masterCatacombs = getObj(dungeons, "master_catacombs");

            double cataXp = getDouble(dungeons, "catacombs_experience", 0.0);
            int cataLevel = getCatacombsLevel(cataXp);
            double classAvg = getClassAverageLevel(getObj(dungeons, "player_classes"));
            int secrets = getInt(dungeons, "secrets", 0);
            int totalRuns = getInt(dungeons, "total_runs", 0);
            double secretAvg = totalRuns > 0 ? ((double) secrets / totalRuns) : 0.0;
            int blood = getInt(stats, "blood_mobs_killed", 0);
            int magicalPower = getInt(stats, "magical_power", -1);
            String selectedArrow = uppercaseFirst(getString(stats, "favorite_arrow"));
            String powerStone = uppercaseFirst(getString(stats, "selected_power"));
            String selectedClass = uppercaseFirst(getString(stats, "selected_dungeon_class"));
            Map<String, Integer> tunings = readTunings(stats);

            String f7Pb = formatPb(getNestedInt(getObj(catacombs, "fastest_time_s_plus"), "7"));
            String m7Pb = formatPb(getNestedInt(getObj(masterCatacombs, "fastest_time_s_plus"), "7"));
            String pets = formatPets(stats);
            String armorBlob = getString(stats, "armor_data");
            List<String> allInvBlobs = getAllInventoryBlobs(stats);
            boolean inventoryApi = armorBlob != null && !armorBlob.isBlank();

            List<DecodedItem> armorItems = decodeItemsFromBlob(armorBlob);
            Set<String> allItemIds = new HashSet<>();
            for (String blob : allInvBlobs) {
                for (DecodedItem item : decodeItemsFromBlob(blob)) {
                    if (item.id != null && !item.id.isBlank()) {
                        allItemIds.add(item.id.toUpperCase());
                    }
                }
            }
            boolean hasTerminator = allItemIds.contains("TERMINATOR");
            boolean hasHyperion = allItemIds.contains("HYPERION");

            int lineWidth = getStatsLineWidth(playerName);
            sendLine(buildStatsHeader(playerName, lineWidth + 5));
            sendOverviewLine(cataLevel, classAvg, selectedClass, magicalPower, inventoryApi, tunings);
            sendLine("  §bSecrets: §d" + secrets + " §b(§d" + String.format("%.2f", secretAvg) + "§b) §f| §cBlood Mobs: §f" + blood);
            sendLine("  Pets: " + pets);
            sendLine("  §9Power Stone: §e" + (powerStone == null ? "N/A" : powerStone)
                    + " §f| §6Arrows: §e" + (selectedArrow == null ? "N/A" : selectedArrow));
            sendLine("  §6Key Items: §eTerminator " + (hasTerminator ? "§aYes" : "§cNo")
                    + " §7| §eHyperion " + (hasHyperion ? "§aYes" : "§cNo"));
            sendLine(" ");
            if (!inventoryApi) {
                sendLine("  §cInventory Api is Off");
            } else if (armorItems.isEmpty()) {
                sendLine("  §7Armor: §8No data");
            } else {
                sendLine("  §7Armor:");
                for (DecodedItem armor : armorItems) {
                    sendArmorLine(armor);
                }
            }
            sendLine(" ");
            sendCompletionComponents(catacombs, masterCatacombs, f7Pb, m7Pb);
            sendLine("§a§l" + "-".repeat(lineWidth) + "§r");
            sendKickPrompt(playerName);
        } catch (Exception e) {
            chat("§7PF Stats §8> §cError while reading stats for §e" + playerName);
        }
    }

    private static JsonObject getDungeonStats(String playerName) throws Exception {
        String key = playerName.toUpperCase();
        JsonObject cached = DUNGEON_CACHE.get(key);
        if (cached != null) return cached;

        String uuid = getUuid(playerName);
        if (uuid == null || uuid.isBlank()) return null;

        String body = readUrl(ODIN_HYPIXEL_API + uuid);
        JsonObject raw = JsonParser.parseString(body).getAsJsonObject();
        JsonObject data = normalizeFromOdin(raw, uuid);
        if (data == null) return null;
        DUNGEON_CACHE.put(key, data);
        return data;
    }

    private static String getUuid(String playerName) throws Exception {
        String key = playerName.toUpperCase();
        if (UUID_CACHE.containsKey(key)) {
            String cached = UUID_CACHE.get(key);
            return (cached == null || cached.isBlank()) ? null : cached;
        }

        UUID_CACHE.put(key, "");
        try {
            String body = readUrl(MINECRAFT_SERVICES_UUID_API + playerName);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String uuid = getString(json, "id");
            if (uuid != null && !uuid.isBlank()) {
                UUID_CACHE.put(key, uuid);
                return uuid;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static JsonObject normalizeFromOdin(JsonObject raw, String uuid) {
        if (raw == null || uuid == null || uuid.isBlank()) return null;
        JsonArray profiles = raw.getAsJsonArray("profiles");
        if (profiles == null || profiles.isEmpty()) return null;

        JsonObject selectedProfile = null;
        for (JsonElement el : profiles) {
            if (!el.isJsonObject()) continue;
            JsonObject p = el.getAsJsonObject();
            if (p.has("selected") && p.get("selected").isJsonPrimitive() && p.get("selected").getAsBoolean()) {
                selectedProfile = p;
                break;
            }
        }
        if (selectedProfile == null && !profiles.isEmpty() && profiles.get(0).isJsonObject()) {
            selectedProfile = profiles.get(0).getAsJsonObject();
        }
        if (selectedProfile == null) return null;

        JsonObject members = getObj(selectedProfile, "members");
        if (members == null || !members.has(uuid) || !members.get(uuid).isJsonObject()) return null;
        JsonObject member = members.getAsJsonObject(uuid);

        JsonObject out = new JsonObject();
        JsonObject dungeons = getObj(member, "dungeons");
        if (dungeons == null) dungeons = new JsonObject();

        JsonObject dungeonTypes = getObj(dungeons, "dungeon_types");
        JsonObject catacombs = dungeonTypes == null ? null : getObj(dungeonTypes, "catacombs");
        JsonObject master = dungeonTypes == null ? null : getObj(dungeonTypes, "master_catacombs");

        JsonObject normalizedDungeons = new JsonObject();
        normalizedDungeons.addProperty("secrets", getInt(dungeons, "secrets", 0));
        normalizedDungeons.addProperty("total_runs", readTotalRunsFromTierCompletions(catacombs, master));

        if (catacombs != null) {
            JsonObject cat = new JsonObject();
            cat.add("tier_completions", copyObjOrEmpty(getObj(catacombs, "tier_completions")));
            cat.add("fastest_time_s_plus", copyObjOrEmpty(getObj(catacombs, "fastest_time_s_plus")));
            normalizedDungeons.add("catacombs", cat);
            normalizedDungeons.addProperty("catacombs_experience", getDouble(catacombs, "experience", 0.0));
        }

        if (master != null) {
            JsonObject m = new JsonObject();
            m.add("tier_completions", copyObjOrEmpty(getObj(master, "tier_completions")));
            m.add("fastest_time_s_plus", copyObjOrEmpty(getObj(master, "fastest_time_s_plus")));
            normalizedDungeons.add("master_catacombs", m);
        }

        JsonObject classes = getObj(dungeons, "player_classes");
        JsonObject normalizedClasses = new JsonObject();
        if (classes != null) {
            for (Map.Entry<String, JsonElement> entry : classes.entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                JsonObject classObj = entry.getValue().getAsJsonObject();
                normalizedClasses.addProperty(entry.getKey(), getDouble(classObj, "experience", 0.0));
            }
        }
        normalizedDungeons.add("player_classes", normalizedClasses);
        out.add("dungeons", normalizedDungeons);

        JsonObject playerStats = getObj(member, "player_stats");
        JsonObject kills = playerStats == null ? null : getObj(playerStats, "kills");
        int bloodKills = 0;
        if (kills != null) {
            bloodKills += (int) Math.round(getDouble(kills, "watcher_summon_undead", 0.0));
            bloodKills += (int) Math.round(getDouble(kills, "master_watcher_summon_undead", 0.0));
        }
        out.addProperty("blood_mobs_killed", bloodKills);

        JsonObject itemData = getObj(member, "item_data");
        String favoriteArrow = itemData == null ? null : getString(itemData, "favorite_arrow");
        if (favoriteArrow != null) out.addProperty("favorite_arrow", favoriteArrow);

        JsonObject accessoryBag = getObj(member, "accessory_bag_storage");
        String selectedPower = accessoryBag == null ? null : getString(accessoryBag, "selected_power");
        if (selectedPower != null) out.addProperty("selected_power", selectedPower);
        int highestMp = accessoryBag == null ? -1 : getInt(accessoryBag, "highest_magical_power", -1);
        if (highestMp >= 0) out.addProperty("magical_power", highestMp);
        JsonObject tuning = accessoryBag == null ? null : getObj(accessoryBag, "tuning");
        JsonObject slot0 = tuning == null ? null : getObj(tuning, "slot_0");
        out.add("tunings", slot0 == null ? new JsonObject() : slot0.deepCopy());

        String selectedClass = getString(dungeons, "selected_dungeon_class");
        if (selectedClass != null) out.addProperty("selected_dungeon_class", selectedClass);

        JsonObject inventory = getObj(member, "inventory");
        JsonObject echest = inventory == null ? null : getObj(inventory, "ender_chest_contents");
        String echestData = echest == null ? null : getString(echest, "data");
        if (echestData != null) {
            out.addProperty("talisman_bag_data", echestData.isBlank() ? "" : "1");
        }
        JsonObject invArmor = inventory == null ? null : getObj(inventory, "inv_armor");
        String armorData = invArmor == null ? null : getString(invArmor, "data");
        if (armorData != null) out.addProperty("armor_data", armorData);
        JsonObject invContents = inventory == null ? null : getObj(inventory, "inv_contents");
        String invData = invContents == null ? null : getString(invContents, "data");
        if (invData != null) out.addProperty("inv_data", invData);
        if (echestData != null) out.addProperty("ender_chest_data", echestData);
        JsonObject backpackContents = inventory == null ? null : getObj(inventory, "backpack_contents");
        JsonArray backpackBlobs = new JsonArray();
        if (backpackContents != null) {
            for (Map.Entry<String, JsonElement> entry : backpackContents.entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                String dataBlob = getString(entry.getValue().getAsJsonObject(), "data");
                if (dataBlob != null && !dataBlob.isBlank()) {
                    backpackBlobs.add(dataBlob);
                }
            }
        }
        out.add("backpack_data", backpackBlobs);

        JsonObject petsData = getObj(member, "pets_data");
        JsonArray pets = petsData == null ? null : petsData.getAsJsonArray("pets");
        out.add("pets", pets == null ? new JsonArray() : pets.deepCopy());

        return out;
    }

    private static JsonObject copyObjOrEmpty(JsonObject obj) {
        return obj == null ? new JsonObject() : obj.deepCopy();
    }

    private static int readTotalRunsFromTierCompletions(JsonObject catacombs, JsonObject master) {
        int catTotal = readTierCompletionTotal(catacombs);
        int masterTotal = readTierCompletionTotal(master);
        return catTotal + masterTotal;
    }

    private static int readTierCompletionTotal(JsonObject dungeonType) {
        if (dungeonType == null) return 0;
        JsonObject tier = getObj(dungeonType, "tier_completions");
        if (tier == null) return 0;
        return getInt(tier, "total", 0);
    }

    private static String readUrl(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(7))
                .header("User-Agent", "XenAddons")
                .GET()
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    private static void chat(String msg) {
        if (msg == null) return;
        if (mcRef() == null) return;
        mcRef().execute(() -> ChatUtils.info(msg));
    }

    private static void sendLine(String line) {
        net.minecraft.client.MinecraftClient mc = mcRef();
        if (mc == null || line == null) return;
        mc.execute(() -> {
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal(line), false);
            }
        });
    }

    private static void sendText(Text text) {
        net.minecraft.client.MinecraftClient mc = mcRef();
        if (mc == null || text == null) return;
        mc.execute(() -> {
            if (mc.player != null) {
                mc.player.sendMessage(text, false);
            }
        });
    }

    private static void postChat(Text text) {
        net.minecraft.client.MinecraftClient mc = mcRef();
        if (mc == null || text == null) return;
        mc.execute(() -> {
            if (mc.inGameHud != null) {
                mc.inGameHud.getChatHud().addMessage(text);
            }
        });
    }

    private static net.minecraft.client.MinecraftClient mcRef() {
        return net.minecraft.client.MinecraftClient.getInstance();
    }

    private static JsonObject getObj(JsonObject parent, String key) {
        if (parent == null || key == null || !parent.has(key)) return null;
        JsonElement e = parent.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    private static String getString(JsonObject parent, String key) {
        if (parent == null || key == null || !parent.has(key)) return null;
        JsonElement e = parent.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    private static int getInt(JsonObject parent, String key, int def) {
        if (parent == null || key == null || !parent.has(key)) return def;
        JsonElement e = parent.get(key);
        try {
            return e.getAsInt();
        } catch (Exception ignored) {
            return def;
        }
    }

    private static double getDouble(JsonObject parent, String key, double def) {
        if (parent == null || key == null || !parent.has(key)) return def;
        JsonElement e = parent.get(key);
        try {
            return e.getAsDouble();
        } catch (Exception ignored) {
            return def;
        }
    }

    private static Integer getNestedInt(JsonObject parent, String key) {
        if (parent == null || key == null || !parent.has(key)) return null;
        try {
            return parent.get(key).getAsInt();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String formatPb(Integer millis) {
        if (millis == null || millis <= 0) return "N/A";
        int sec = millis / 1000;
        int min = sec / 60;
        int rem = sec % 60;
        return String.format("%d:%02d", min, rem);
    }

    private static double getClassAverageLevel(JsonObject classesObj) {
        if (classesObj == null || classesObj.entrySet().isEmpty()) return 0.0;
        double sum = 0.0;
        int count = 0;
        for (Map.Entry<String, JsonElement> entry : classesObj.entrySet()) {
            try {
                double xp = entry.getValue().getAsDouble();
                sum += getCatacombsLevel(xp);
                count++;
            } catch (Exception ignored) {
            }
        }
        return count == 0 ? 0.0 : (sum / count);
    }

    // Same progression logic used in Noamm's ProfileUtils.
    private static final int[] CATA_XP_REQUIREMENTS = {
            50, 125, 235, 395, 625, 955, 1425, 2095, 3045, 4385,
            6275, 8940, 12700, 17960, 25340, 35640, 50040, 70040, 97640, 135640,
            188140, 259640, 356640, 488640, 668640, 911640, 1239640, 1683640, 2284640, 3084640,
            4149640, 5559640, 7459640, 9959640, 13259640, 17559640, 23159640, 30359640, 39559640, 51559640,
            66559640, 85559640, 109559640, 139559640, 177559640, 225559640, 285559640, 360559640, 453559640, 569809640
    };

    private static int getCatacombsLevel(double totalXp) {
        if (totalXp < 0) return 0;
        for (int i = 0; i < CATA_XP_REQUIREMENTS.length; i++) {
            if (totalXp < CATA_XP_REQUIREMENTS[i]) return i;
        }
        int lastLevel = CATA_XP_REQUIREMENTS.length;
        double extraXp = totalXp - CATA_XP_REQUIREMENTS[CATA_XP_REQUIREMENTS.length - 1];
        int bonus = (int) (extraXp / 200_000_000.0);
        return lastLevel + Math.max(0, bonus);
    }

    private static boolean hasNonBlankString(JsonObject obj, String key) {
        String value = getString(obj, key);
        return value != null && !value.isBlank();
    }

    private static String uppercaseFirst(String value) {
        if (value == null || value.isBlank()) return null;
        String lower = value.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1).replace("_", " ");
    }

    private static String formatPets(JsonObject data) {
        JsonElement petsElement = data.get("pets");
        if (petsElement == null || !petsElement.isJsonArray()) return "§7N/A";

        Set<String> owned = new HashSet<>();
        for (JsonElement el : petsElement.getAsJsonArray()) {
            if (!el.isJsonObject()) continue;
            JsonObject pet = el.getAsJsonObject();
            String type = getString(pet, "type");
            if (type == null) continue;
            String normalized = normalizePetName(type);
            if (normalized != null && ALLOWED_PF_PETS.contains(normalized)) {
                owned.add(normalized);
            }
        }
        List<String> ordered = List.of("EDRAG", "GDRAG", "JELLYFISH", "PHOENIX", "SPIRIT");
        List<String> rendered = new ArrayList<>();
        for (String pet : ordered) {
            boolean has = owned.contains(pet);
            rendered.add((has ? "§a✔ " : "§c✘ ") + formatPetLabel(pet));
        }
        return String.join(" §7| §r", rendered);
    }

    private static String formatPetLabel(String normalized) {
        return switch (normalized) {
            case "EDRAG" -> "Edrag";
            case "GDRAG" -> "Gdrag";
            case "BLACK_CAT" -> "Black Cat";
            default -> uppercaseFirst(normalized);
        };
    }

    private static String normalizePetName(String type) {
        if (type == null || type.isBlank()) return null;
        String up = type.toUpperCase();
        if (up.endsWith("_DRAGON")) return up.startsWith("GOLDEN") ? "GDRAG" : "EDRAG";
        return up;
    }

    private static void sendArmorLine(DecodedItem armor) {
        if (armor == null) return;
        String name = armor.name == null || armor.name.isBlank() ? "Unknown Armor Piece" : armor.name;
        MutableText line = Text.literal("  " + name);
        if (!armor.lore.isEmpty() || (armor.name != null && !armor.name.isBlank())) {
            List<String> hoverLines = new ArrayList<>();
            hoverLines.add(name);
            if (!armor.lore.isEmpty()) {
                hoverLines.add("");
                hoverLines.addAll(armor.lore);
            }
            String hover = String.join("\n", hoverLines);
            line.setStyle(line.getStyle().withHoverEvent(new HoverEvent.ShowText(Text.literal(hover))));
        }
        sendText(line);
    }

    private static void sendKickPrompt(String playerName) {
        if (playerName == null || playerName.isBlank()) return;
        MutableText text = Text.literal("§8[§6XenAddons§8] §ePress to kick §b" + playerName);
        Style style = text.getStyle()
                .withClickEvent(new ClickEvent.RunCommand("/p kick " + playerName))
                .withHoverEvent(new HoverEvent.ShowText(Text.literal("§e/p kick " + playerName)));
        text.setStyle(style);
        sendText(text);
    }

    private static int getStatsLineWidth(String playerName) {
        String name = (playerName == null || playerName.isBlank()) ? "Player" : playerName;
        String center = name + "'s Dungeon Stats";
        return Math.max(MIN_STATS_LINE_WIDTH, center.length() + 2);
    }

    private static String buildStatsHeader(String playerName, int totalWidth) {
        String name = (playerName == null || playerName.isBlank()) ? "Player" : playerName;
        String center = name + "'s Dungeon Stats";

        int available = totalWidth - center.length() - 2; // spaces around center
        if (available < 2) {
            return "§a§l" + "-".repeat(3) + " §r§b" + center + " §a§l" + "-".repeat(3) + "§r";
        }

        int left = available / 2;
        int right = available - left;
        return "§a§l" + "-".repeat(left) + " §r§b" + center + " §a§l" + "-".repeat(right) + "§r";
    }

    private static List<String> getAllInventoryBlobs(JsonObject stats) {
        List<String> blobs = new ArrayList<>();
        String armor = getString(stats, "armor_data");
        String inv = getString(stats, "inv_data");
        String echest = getString(stats, "ender_chest_data");
        if (armor != null && !armor.isBlank()) blobs.add(armor);
        if (inv != null && !inv.isBlank()) blobs.add(inv);
        if (echest != null && !echest.isBlank()) blobs.add(echest);
        JsonElement backpack = stats.get("backpack_data");
        if (backpack != null && backpack.isJsonArray()) {
            for (JsonElement e : backpack.getAsJsonArray()) {
                if (e != null && e.isJsonPrimitive()) {
                    String b = e.getAsString();
                    if (b != null && !b.isBlank()) blobs.add(b);
                }
            }
        }
        return blobs;
    }

    private static List<DecodedItem> decodeItemsFromBlob(String base64Data) {
        List<DecodedItem> out = new ArrayList<>();
        if (base64Data == null || base64Data.isBlank()) return out;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64Data);
            NbtCompound root = NbtIo.readCompressed(new java.io.ByteArrayInputStream(bytes), NbtSizeTracker.ofUnlimitedBytes());
            if (root == null) return out;
            NbtList items = root.getList("i").orElse(null);
            if (items == null || items.isEmpty()) return out;

            for (NbtElement entry : items) {
                NbtCompound stackCompound = entry.asCompound().orElse(null);
                if (stackCompound == null) continue;
                NbtCompound tag = stackCompound.getCompound("tag").orElse(null);
                if (tag == null) continue;

                NbtCompound extra = tag.getCompound("ExtraAttributes").orElse(null);
                String id = extra == null ? "" : extra.getString("id", "");
                if (id == null || id.isBlank()) id = extra == null ? "" : extra.getString("ID", "");

                NbtCompound display = tag.getCompound("display").orElse(null);
                if (display == null) continue;
                String name = display.getString("Name", "");
                NbtList loreList = display.getList("Lore").orElse(null);
                List<String> lore = new ArrayList<>();
                if (loreList != null) {
                    for (NbtElement line : loreList) {
                        String s = line.asString().orElse("");
                        if (s != null) lore.add(s);
                    }
                }
                out.add(new DecodedItem(name, id, lore));
            }
            // NBT armor list comes boots->helmet, reverse to display helmet->boots.
            java.util.Collections.reverse(out);
        } catch (Exception ignored) {
        }
        return out;
    }

    private record DecodedItem(String name, String id, List<String> lore) {}

    private static void sendOverviewLine(int cataLevel, double classAvg, String selectedClass, int magicalPower, boolean inventoryApi, Map<String, Integer> tunings) {
        String mpText = magicalPower >= 0 ? "§e" + magicalPower : (inventoryApi ? "§7N/A" : "§cAPI off");
        String clazz = selectedClass == null ? "None" : selectedClass;

        MutableText line = Text.literal(
                "  §4Cata Level: §b" + cataLevel
                        + " §f| §dClass Avg: §b" + String.format("%.1f", classAvg)
                        + " §f| §bClass: §e" + clazz
                        + " §f| §dMP: " + mpText
        );
        if (tunings != null && !tunings.isEmpty()) {
            StringBuilder hover = new StringBuilder("§dTunings");
            for (Map.Entry<String, Integer> entry : tunings.entrySet()) {
                hover.append("\n§7")
                        .append(uppercaseFirst(entry.getKey()))
                        .append(": §f")
                        .append(entry.getValue());
            }
            line.setStyle(line.getStyle().withHoverEvent(new HoverEvent.ShowText(Text.literal(hover.toString()))));
        }
        sendText(line);
    }

    private static Map<String, Integer> readTunings(JsonObject stats) {
        Map<String, Integer> out = new LinkedHashMap<>();
        JsonObject tuningObj = getObj(stats, "tunings");
        if (tuningObj == null) return out;
        for (Map.Entry<String, JsonElement> entry : tuningObj.entrySet()) {
            try {
                out.put(entry.getKey(), entry.getValue().getAsInt());
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    private static void sendCompletionComponents(JsonObject catacombs, JsonObject masterCatacombs, String f7Pb, String m7Pb) {
        Text floorComp = buildCompletionComponent(
                "  §aFloor Completions §7(F7): " + f7Pb,
                getObj(catacombs, "tier_completions"),
                getObj(catacombs, "fastest_time_s_plus"),
                1,
                "§2§l●§a"
        );
        if (floorComp != null) sendText(floorComp);

        Text masterComp = buildCompletionComponent(
                "  §l§4Master Completions §7(M7): " + m7Pb,
                getObj(masterCatacombs, "tier_completions"),
                getObj(masterCatacombs, "fastest_time_s_plus"),
                1,
                "§c§l●§4"
        );
        if (masterComp != null) sendText(masterComp);
    }

    private static Text buildCompletionComponent(String title, JsonObject completionObj, JsonObject pbObj, int minFloor, String bulletStyle) {
        if (completionObj == null || completionObj.entrySet().isEmpty()) return null;
        int maxFloor = minFloor;
        for (Map.Entry<String, JsonElement> entry : completionObj.entrySet()) {
            try {
                maxFloor = Math.max(maxFloor, Integer.parseInt(entry.getKey()));
            } catch (Exception ignored) {
            }
        }

        StringBuilder hover = new StringBuilder();
        for (int floor = minFloor; floor <= maxFloor; floor++) {
            if (hover.length() > 0) hover.append("\n");
            Integer completions = getNestedInt(completionObj, Integer.toString(floor));
            Integer pb = pbObj == null ? null : getNestedInt(pbObj, Integer.toString(floor));
            String floorName = (minFloor == 0 && floor == 0) ? "Entrance" : Integer.toString(floor);
            hover.append(bulletStyle).append(" Floor ").append(floorName).append(": §e");
            if (completions == null) {
                hover.append("§cDNF");
            } else {
                hover.append(completions).append(" §7(§6S+ §e").append(formatPb(pb)).append("§7)");
            }
        }

        MutableText line = Text.literal(title);
        Style style = line.getStyle().withHoverEvent(new HoverEvent.ShowText(Text.literal(hover.toString())));
        line.setStyle(style);
        return line;
    }
}
