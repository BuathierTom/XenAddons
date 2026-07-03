package xenus.xenaddons.features.impl.dungeons;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.NumberSetting;
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
    private final NumberSetting hudOffsetX = new NumberSetting("HUD Offset X", 0, -1000, 1000, 1);
    private final NumberSetting hudY = new NumberSetting("HUD Y", 30, -1000, 1000, 1);
    private final NumberSetting hudScale = new NumberSetting("HUD Scale", 1.0, 0.5, 6.0, 0.05);

    public PosTitlesModule() {
        super("Pos Titles", "Shows titles when teammates reach specific points", Category.DUNGEON);
        hudOffsetX.setVisibility(() -> false);
        hudY.setVisibility(() -> false);
        hudScale.setVisibility(() -> false);
        this.registerSetting(hudOffsetX);
        this.registerSetting(hudY);
        this.registerSetting(hudScale);
        
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

        // Use HudRenderCallback for TOP-MIDDLE position
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("xenaddons", "pos_titles_hud"), (drawContext, tickDelta) -> {
            if (currentTitle == null || titleTimer <= 0) return;
            renderTopTitle(drawContext);
        });
    }

    private void renderTopTitle(GuiGraphicsExtractor drawContext) {
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int textWidth = mc.font.width(currentTitle);
        int textHeight = mc.font.lineHeight;
        float scale = (float) hudScale.getValue().doubleValue();
        float inv = 1.0f / scale;
        int centerXRaw = (width / 2) + hudOffsetX.getValue().intValue();
        int yRaw = hudY.getValue().intValue();
        int minCenterX = (int) Math.ceil((textWidth * scale) / 2.0f) + 2;
        int maxCenterX = width - minCenterX;
        int centerX = Mth.clamp(centerXRaw, minCenterX, Math.max(minCenterX, maxCenterX));
        int maxY = height - (int) Math.ceil(textHeight * scale) - 2;
        int y = Mth.clamp(yRaw, 2, Math.max(2, maxY));

        drawContext.pose().pushMatrix();
        drawContext.pose().scale(scale, scale);
        int x = (int) ((centerX * inv) - (textWidth / 2f));
        int drawY = (int) (y * inv);
        drawContext.text(mc.font, currentTitle, x, drawY, 0xFFFFFFFF);
        drawContext.pose().popMatrix();

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

    public int getHudOffsetX() {
        return hudOffsetX.getValue().intValue();
    }

    public int getHudY() {
        return hudY.getValue().intValue();
    }

    public void setHudOffsetX(int value) {
        hudOffsetX.setValue((double) value);
    }

    public void setHudY(int value) {
        hudY.setValue((double) value);
    }

    public double getHudScale() {
        return hudScale.getValue();
    }

    public void setHudScale(double value) {
        hudScale.setValue(value);
    }
}
