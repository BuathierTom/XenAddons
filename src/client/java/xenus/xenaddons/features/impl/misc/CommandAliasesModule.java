package xenus.xenaddons.features.impl.misc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.settings.BooleanSetting;

public class CommandAliasesModule extends Module {
    
    public final BooleanSetting warpDungeon = new BooleanSetting("/d, /dung", true);
    public final BooleanSetting allInvite = new BooleanSetting("/ai", true);
    public final BooleanSetting kickOffline = new BooleanSetting("/pko", true);
    public final BooleanSetting partyDisband = new BooleanSetting("/pd", true);
    public final BooleanSetting partyKick = new BooleanSetting("/pk", true);
    public final BooleanSetting partyTransfer = new BooleanSetting("/pt", true);
    public final BooleanSetting lobbySb = new BooleanSetting("/lsb, /ld", true);
    public final BooleanSetting floorShortcuts = new BooleanSetting("/f1-f7, /m1-m7", true);

    public CommandAliasesModule() {
        super("Command Aliases", "Useful warp and utility command shortcuts", Category.MISC);
        this.registerSetting(warpDungeon);
        this.registerSetting(allInvite);
        this.registerSetting(kickOffline);
        this.registerSetting(partyDisband);
        this.registerSetting(partyKick);
        this.registerSetting(partyTransfer);
        this.registerSetting(lobbySb);
        this.registerSetting(floorShortcuts);
    }
    
    private static CommandAliasesModule getInstance() {
        return (CommandAliasesModule) ModuleManager.getModule("Command Aliases");
    }

    private static boolean isEnabled(BooleanSetting setting) {
        CommandAliasesModule mod = getInstance();
        return mod != null && mod.isEnabled() && setting.getValue();
    }

    public static void init() {
         ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
             
             // d, dung -> warp dungeon_hub
             dispatcher.register(ClientCommandManager.literal("d").executes(context -> {
                 if (!isEnabled(getInstance().warpDungeon)) return 0;
                 return runCommand("warp dungeon_hub");
             }));
             dispatcher.register(ClientCommandManager.literal("dung").executes(context -> {
                 if (!isEnabled(getInstance().warpDungeon)) return 0;
                 return runCommand("warp dungeon_hub");
             }));
             
             // ai -> p settings allinvite
             dispatcher.register(ClientCommandManager.literal("ai").executes(context -> {
                 if (!isEnabled(getInstance().allInvite)) return 0;
                 return runCommand("p settings allinvite");
             }));
             
             // pko -> p kickoffline
             dispatcher.register(ClientCommandManager.literal("pko").executes(context -> {
                 if (!isEnabled(getInstance().kickOffline)) return 0;
                 return runCommand("p kickoffline");
             }));
             
             // pd -> p disband
             dispatcher.register(ClientCommandManager.literal("pd").executes(context -> {
                 if (!isEnabled(getInstance().partyDisband)) return 0;
                 return runCommand("p disband");
             }));
             
             // pk <player>
             dispatcher.register(ClientCommandManager.literal("pk")
                 .then(ClientCommandManager.argument("player", StringArgumentType.word())
                 .executes(context -> {
                     if (!isEnabled(getInstance().partyKick)) return 0;
                     return runCommand("p kick " + StringArgumentType.getString(context, "player"));
                 })));
                 
             // pt <player>
             dispatcher.register(ClientCommandManager.literal("pt")
                 .then(ClientCommandManager.argument("player", StringArgumentType.word())
                 .executes(context -> {
                     if (!isEnabled(getInstance().partyTransfer)) return 0;
                     return runCommand("p transfer " + StringArgumentType.getString(context, "player"));
                 })));
             
             // lsb
             dispatcher.register(ClientCommandManager.literal("lsb").executes(context -> {
                 if (!isEnabled(getInstance().lobbySb)) return 0;
                 executeLsb();
                 return 1;
             }));
             dispatcher.register(ClientCommandManager.literal("ld").executes(context -> {
                 if (!isEnabled(getInstance().lobbySb)) return 0;
                 executeLd();
                 return 1;
             }));
             
             // Floors
             registerJoinCommand(dispatcher, "f1", "CATACOMBS_FLOOR_ONE");
             registerJoinCommand(dispatcher, "f2", "CATACOMBS_FLOOR_TWO");
             registerJoinCommand(dispatcher, "f3", "CATACOMBS_FLOOR_THREE");
             registerJoinCommand(dispatcher, "f4", "CATACOMBS_FLOOR_FOUR");
             registerJoinCommand(dispatcher, "f5", "CATACOMBS_FLOOR_FIVE");
             registerJoinCommand(dispatcher, "f6", "CATACOMBS_FLOOR_SIX");
             registerJoinCommand(dispatcher, "f7", "CATACOMBS_FLOOR_SEVEN");
             registerJoinCommand(dispatcher, "m1", "MASTER_CATACOMBS_FLOOR_ONE");
             registerJoinCommand(dispatcher, "m2", "MASTER_CATACOMBS_FLOOR_TWO");
             registerJoinCommand(dispatcher, "m3", "MASTER_CATACOMBS_FLOOR_THREE");
             registerJoinCommand(dispatcher, "m4", "MASTER_CATACOMBS_FLOOR_FOUR");
             registerJoinCommand(dispatcher, "m5", "MASTER_CATACOMBS_FLOOR_FIVE");
             registerJoinCommand(dispatcher, "m6", "MASTER_CATACOMBS_FLOOR_SIX");
             registerJoinCommand(dispatcher, "m7", "MASTER_CATACOMBS_FLOOR_SEVEN");

         });
    }

    private static int runCommand(String cmd) {
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.networkHandler.sendChatCommand(cmd);
        }
        return 1;
    }
    
    private static void registerJoinCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, String alias, String floor) {
        dispatcher.register(ClientCommandManager.literal(alias).executes(context -> {
            if (!isEnabled(getInstance().floorShortcuts)) return 0;
            return runCommand("joininstance " + floor);
        }));
    }
    
    private static void executeLsb() {
        new Thread(() -> {
            try {
                // l -> /l
                if (MinecraftClient.getInstance().player != null) 
                    MinecraftClient.getInstance().player.networkHandler.sendChatCommand("l");
                
                Thread.sleep(500);
                
                if (MinecraftClient.getInstance().player != null) 
                    MinecraftClient.getInstance().player.networkHandler.sendChatCommand("play sb");
            } catch (Exception e) {}
        }).start();
    }
    
    private static void executeLd() {
         new Thread(() -> {
            try {
                if (MinecraftClient.getInstance().player != null) 
                    MinecraftClient.getInstance().player.networkHandler.sendChatCommand("l");
                
                Thread.sleep(500);
                
                if (MinecraftClient.getInstance().player != null) 
                    MinecraftClient.getInstance().player.networkHandler.sendChatCommand("play sb");
                    
                Thread.sleep(1000);
                
                if (MinecraftClient.getInstance().player != null) 
                    MinecraftClient.getInstance().player.networkHandler.sendChatCommand("warp dungeon_hub");
            } catch (Exception e) {}
        }).start();
    }
}
