package xenus.xenaddons.features.impl.dungeons;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.utils.DungeonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MelodyWarningModule extends Module {

    private final List<String> playersStackingMelody = new ArrayList<>();
    private String playerName = "";
    private String melodyProgress = "";
    private int furthestAlongMelody = 0;
    private int lastPhase = 0;

    // Regex patterns
    private final Pattern MELODY_PATTERN = Pattern.compile("^Party >(?: \\[.+?\\])? (\\w+): .*(\\d/\\d|\\d\\d%)$");
    private final Pattern TERMINAL_PATTERN = Pattern.compile("^(\\w+) activated a terminal! \\(\\d+/\\d+\\)$");

    public MelodyWarningModule() {
        super("Melody Warning", "Alerts when a teammate has the Melody terminal", Category.DUNGEON);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!this.isEnabled()) return;
            
            // Periodically check for phase change to reset
            if (DungeonUtils.currentPhase != lastPhase) {
                resetMelody();
                lastPhase = DungeonUtils.currentPhase;
            }
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (this.isEnabled()) handleChat(message.getString());
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (!this.isEnabled() || melodyProgress.isEmpty() || !DungeonUtils.inDungeon()) return;
            renderMelodyWarning(drawContext);
        });
    }

    private void handleChat(String text) {
        if (!DungeonUtils.inDungeon()) return;
        String clean = text.replaceAll("§[0-9a-fk-or]", "");

        Matcher melodyMatch = MELODY_PATTERN.matcher(clean);
        if (melodyMatch.find()) {
            String playerMatched = melodyMatch.group(1);
            
            if (!playersStackingMelody.contains(playerMatched) && !playerMatched.equals(mc.player.getName().getString())) {
                playersStackingMelody.add(playerMatched);
            }

            if (playerMatched.equals(mc.player.getName().getString())) return;

            String progressStr = melodyMatch.group(2);
            int progressVal = 0;
            String displayProgress = progressStr;

            if (progressStr.contains("%")) {
                try {
                    int percentage = Integer.parseInt(progressStr.replace("%", ""));
                    if (percentage >= 25) {
                        displayProgress = (percentage / 25) + "/4";
                        progressVal = percentage / 25;
                    } else {
                        progressVal = 0;
                    }
                } catch (NumberFormatException ignored) {}
            } else if (progressStr.contains("/")) {
                 try {
                     progressVal = Integer.parseInt(progressStr.split("/")[0]);
                 } catch (Exception ignored) {}
            }

            if (progressVal > furthestAlongMelody || furthestAlongMelody == 0) {
                playerName = playerMatched;
                furthestAlongMelody = progressVal;
                melodyProgress = displayProgress;
                if (mc.player != null) {
                    mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                }
            }
            return;
        }

        Matcher termMatch = TERMINAL_PATTERN.matcher(clean);
        if (termMatch.find()) {
            String completedPlayer = termMatch.group(1);
            playersStackingMelody.remove(completedPlayer);
            if (completedPlayer.equals(playerName)) {
                resetMelody();
            }
        }
    }

    private void renderMelodyWarning(DrawContext drawContext) {
        if (mc.getWindow() == null) return;
        
        String playerClass = DungeonUtils.getPlayerClass(playerName);
        String classColor = DungeonUtils.getClassColor(playerClass);
        String classLetter = (playerClass != null && !playerClass.isEmpty()) ? playerClass.substring(0, 1) : "?";

        String text = String.format("%s%s (%s) §ehas Melody! %s", classColor, playerName, classLetter, melodyProgress);

        int width = mc.getWindow().getScaledWidth();
        int textWidth = mc.textRenderer.getWidth(text);
        int x = (width - textWidth) / 2;
        int y = 50; // A bit lower than PosTitles (which is at 30)

        drawContext.drawTextWithShadow(mc.textRenderer, text, x, y, 0xFFFFFFFF);
    }

    private void resetMelody() {
        playersStackingMelody.clear();
        melodyProgress = "";
        furthestAlongMelody = 0;
        playerName = "";
    }
    
    @Override
    public void onEnable() {
        resetMelody();
        lastPhase = 0;
    }
}
