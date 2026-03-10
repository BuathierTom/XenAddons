package xenus.xenaddons.features.impl.dungeons;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.network.ClientPlayerEntity;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.utils.DungeonUtils;

import java.util.HashMap;
import java.util.Map;

public class PosMsgModule extends Module {

    private final Map<String, Boolean> lastLocation = new HashMap<>();
    private long lastSendTime = 0;

    public PosMsgModule() {
        super("Pos Message", "Sends a chat message when you reach specific points in F7/M7", Category.DUNGEON);
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (this.isEnabled()) onTick();
        });
    }

    private void onTick() {
        if (!DungeonUtils.inDungeon()) {
            if (!lastLocation.isEmpty()) lastLocation.clear();
            return;
        }
        if (!DungeonUtils.inBoss()) {
            return;
        }
        
        ClientPlayerEntity player = mc.player;
        if (player == null || player.isInvisible()) return;

        String playerClass = DungeonUtils.getPlayerClass(player);
        if (playerClass == null) playerClass = "";

        for (DungeonUtils.PositionDefinition pos : DungeonUtils.positionDefinitions) {
            if (lastLocation.getOrDefault(pos.id, false)) continue;
            
            if (pos.checkCondition.apply(playerClass) && pos.checkPosition.apply(player)) {
                
                // Anti-spam
                if (System.currentTimeMillis() - lastSendTime < 500) continue;
                
                lastLocation.put(pos.id, true);
                lastSendTime = System.currentTimeMillis();
                
                // Send Party Chat
                String cmd = "pc " + pos.messageText;
                if (mc.getNetworkHandler() != null) {
                    mc.getNetworkHandler().sendChatCommand(cmd);
                }
                
                // Client side confirmation
                xenus.xenaddons.utils.ChatUtils.info("Announcing -> " + pos.messageText);
            }
        }
    }
    
    @Override
    public void onDisable() {
        lastLocation.clear();
    }
}
