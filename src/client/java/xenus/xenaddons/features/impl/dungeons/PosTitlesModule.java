package xenus.xenaddons.features.impl.dungeons;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.hud.HudElement;
import xenus.xenaddons.hud.HudRender;
import xenus.xenaddons.utils.ChatUtils;
import xenus.xenaddons.utils.DungeonUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PosTitlesModule extends Module {

    private final Map<String, Map<String, Boolean>> lastLocations = new HashMap<>();
    private final Pattern CHAT_PATTERN = Pattern.compile("Party > (\\[.+?\\] )?(.+?): (.+)");
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Custom HUD Rendering
    private String currentTitle = null;
    private int titleTimer = 0;

    public PosTitlesModule() {
        super("Pos Titles", "Shows titles when teammates reach specific points", Category.DUNGEON);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (this.isEnabled()) {
                onTick();
            }
            if (titleTimer > 0) titleTimer--;
            else currentTitle = null;
        });
        
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (this.isEnabled()) handleChat(message.getString());
        });

        hudElement("Pos Titles", (context, example) -> HudRender.centeredText(context, example ? "Player (M) At P2!" : currentTitle, 0xFFFFFFFF))
            .visibleWhen(() -> currentTitle != null && titleTimer > 0)
            .centered()
            .defaults(HudElement.CENTER, 30.0f, 1.0f);
    }

    private void onTick() {
        if (!DungeonUtils.inDungeon()) {
            if (!lastLocations.isEmpty()) lastLocations.clear();
            return;
        }
        if (mc.level == null || mc.player == null) return;

        for (AbstractClientPlayer entity : mc.level.players()) {
            if (entity.getUUID().equals(mc.player.getUUID())) continue;
            if (entity.isInvisible()) continue;

            String playerName = entity.getName().getString();
            String playerClass = DungeonUtils.getPlayerClass(entity);
            if (playerClass == null) continue;

            Map<String, Boolean> playerVisits = lastLocations.computeIfAbsent(playerName, k -> new HashMap<>());

            for (DungeonUtils.PositionDefinition pos : DungeonUtils.positionDefinitions) {
                if (playerVisits.getOrDefault(pos.id, false)) continue;
                if ("AtP5".equals(pos.id) && !DungeonUtils.hasHealerBeenAtMidThisRun()) continue;

                if (pos.checkCondition.apply(playerClass) && pos.checkPosition.apply(entity)) {
                    if (pos.checkPosition.apply(mc.player)) {
                         playerVisits.put(pos.id, true);
                         continue;
                    }

                    playerVisits.put(pos.id, true);
                    showAlert(playerName, playerClass, pos.messageText);
                }
            }
        }
    }

    private void handleChat(String msg) {
        if (!DungeonUtils.inDungeon()) return;
        if (mc == null || mc.player == null) return;
        
        String clean = msg.replaceAll("§[0-9a-fk-or]", "");
        Matcher matcher = CHAT_PATTERN.matcher(clean);
        if (matcher.find()) {
            String playerName = matcher.group(2).trim();
            if (playerName.contains(" ")) playerName = playerName.substring(playerName.lastIndexOf(" ") + 1);
            if (playerName.equals(mc.player.getName().getString())) return;

            String messageContent = matcher.group(3).toLowerCase();
            String playerClass = DungeonUtils.getPlayerClass(playerName);
            if (playerClass == null) return;

            Map<String, Boolean> playerVisits = lastLocations.computeIfAbsent(playerName, k -> new HashMap<>());

            for (DungeonUtils.PositionDefinition pos : DungeonUtils.positionDefinitions) {
                if (playerVisits.getOrDefault(pos.id, false)) continue;
                if ("AtP5".equals(pos.id) && !DungeonUtils.hasHealerBeenAtMidThisRun()) continue;

                if (pos.checkCondition.apply(playerClass)) {
                    boolean match = false;
                    for (String valid : pos.validMessages) {
                        if (messageContent.contains(valid)) {
                            match = true;
                            break;
                        }
                    }

                    if (match) {
                        if (pos.checkPosition.apply(mc.player)) {
                            playerVisits.put(pos.id, true);
                            return;
                        }
                        
                        playerVisits.put(pos.id, true);
                        showAlert(playerName, playerClass, pos.messageText);
                    }
                }
            }
        }
    }

    public void debugTrigger() {
        showAlert("DebugPlayer", "Mage", "Reached Test Point!");
    }

    public void showAlert(String playerName, String playerClass, String text) {
        if (mc == null || mc.player == null) return;
        String classColor = DungeonUtils.getClassColor(playerClass);
        String classLetter = playerClass.substring(0, 1);
        String formatted = String.format("%s%s (%s) §e%s", classColor, playerName, classLetter, text);
        
        mc.execute(() -> {
            this.currentTitle = formatted;
            this.titleTimer = 60; // 3 seconds
            ChatUtils.info(String.format("%s (%s) %s", playerName, classLetter, text));
        });

        playHarpRoll();
    }


    
    private void playHarpRoll() {
        for (int i = 0; i < 5; i++) {
            final float pitch = 1.6f;
            scheduler.schedule(() -> {
                if (mc.player != null) {
                    mc.execute(() -> mc.player.playSound(SoundEvents.NOTE_BLOCK_HARP.value(), 0.7f, pitch));
                }
            }, i * 50, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void onDisable() {
        lastLocations.clear();
        currentTitle = null;
        titleTimer = 0;
    }
}
