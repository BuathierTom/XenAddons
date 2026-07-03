package xenus.xenaddons.features.impl.misc.zguegaddons;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.utils.DungeonUtils;

import java.util.regex.Pattern;

public class ZguegAddonsModule extends Module {
    
    public final BooleanSetting hideRadioMessages = new BooleanSetting("Radio Msg", true); // Renamed
    public final BooleanSetting hideDiscordWarning = new BooleanSetting("Discord Warn", true); // Renamed
    
    public final BooleanSetting cryptsNotify = new BooleanSetting("Crypts Notify", false);
    public final BooleanSetting classNotify = new BooleanSetting("Class Notify", false);
    public final BooleanSetting implosionText = new BooleanSetting("Implosion", true);
    
    // Command Settings
    public final BooleanSetting cmdBoom = new BooleanSetting("!boom", true);
    public final BooleanSetting cmdRng = new BooleanSetting("!rng", true);
    public final BooleanSetting cmdGay = new BooleanSetting("!gay", true);
    public final BooleanSetting cmdRacist = new BooleanSetting("!racist", true);
    public final BooleanSetting cmdPy = new BooleanSetting("!py", true);
    public final BooleanSetting cmdGwer = new BooleanSetting("!gwer", true);
    public final BooleanSetting cmdSize = new BooleanSetting("!size", true);
    
    private final String[] RADIO_MESSAGES = {
        "Your radio is weak. Find another enjoyer to boost it.",
        "Your radio signal is strong!",
        "Your radio lost signal. There's too many enjoyers on this channel."
    };
    
    private final String DISCORD_WARNING = "Please be mindful of Discord links in chat as they may pose a security risk";
    private static final Pattern HIT_TEXT_PATTERN = Pattern.compile(
        "^Your (Spirit Sceptre|Implosion) hit [0-9.,]+ enemies? for [0-9.,]+ damage\\.$"
    );
    private int lastCryptsCount = 0;

    public ZguegAddonsModule() {
        super("ZguegAddons", "Collection of ZguegAddons features", Category.MISC);
        this.registerSetting(cryptsNotify);
        this.registerSetting(classNotify);
        this.registerSetting(implosionText);

        this.registerSetting(hideRadioMessages);
        this.registerSetting(hideDiscordWarning);
        
        this.registerSetting(cmdBoom);
        this.registerSetting(cmdRng);
        this.registerSetting(cmdGay);
        this.registerSetting(cmdRacist);
        this.registerSetting(cmdPy);
        this.registerSetting(cmdGwer);
        this.registerSetting(cmdSize);
        
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!this.isEnabled()) return true;
            return handleChatHide(message);
        });

        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            if (!this.isEnabled()) return true;
            return handleChatHide(message);
        });
        
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
             if (!this.isEnabled()) return;
             String msg = message.getString();
             if (classNotify.getValue()) {
                 if (msg.contains("You have selected the") && msg.contains("Dungeon Class!") && !msg.contains(":")) {
                      if (mc.player != null) {
                         mc.player.connection.sendCommand("pc " + msg);
                      }
                 }
             }
             
             if (msg.contains("is now ready!")) {
                 lastCryptsCount = 0;
             }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!this.isEnabled() || !cryptsNotify.getValue() || mc.level == null) return;
            
            if (!DungeonUtils.inDungeon()) {
                lastCryptsCount = 0;
                return;
            }

            int count = DungeonUtils.crypts;

            // Debug (optional)
            // if (seconds % 10 == 0) mc.player.sendMessage(Component.literal("§7[Debug] Crypts: " + count + ", Secs: " + seconds), false);

            if (count == 5 && lastCryptsCount < 5) {
                if (mc.player != null) {
                    mc.player.connection.sendCommand("pc [ZguegAddons] 5 crypts reached!");
                }
            }
            lastCryptsCount = count;

        });
    }
    
    private boolean handleChatHide(Component message) {
        String msg = message.getString();
        if (hasAnyCommandEnabled()) {
            processPartyMessage(msg);
        }
        
        if (hideRadioMessages.getValue()) {
            for (String criteria : RADIO_MESSAGES) {
                if (msg.contains(criteria)) return false;
            }
        }
        
        if (hideDiscordWarning.getValue() && msg.contains(DISCORD_WARNING)) {
            String[] lines = msg.split("\n");
            boolean modified = false;
            for (String line : lines) {
                if (!line.contains(DISCORD_WARNING)) {
                     if (mc.player != null) mc.player.sendSystemMessage(Component.literal(line));
                } else modified = true;
            }
            if (modified) return false;
        }

        if (implosionText.getValue() && HIT_TEXT_PATTERN.matcher(msg).matches()) {
            return false;
        }

        return true;
    }

    private boolean hasAnyCommandEnabled() {
        return cmdBoom.getValue()
            || cmdRng.getValue()
            || cmdGay.getValue()
            || cmdRacist.getValue()
            || cmdPy.getValue()
            || cmdGwer.getValue()
            || cmdSize.getValue();
    }

    private void processPartyMessage(String msg) {
        String cleanMsg = msg.replaceAll("(?i)§[0-9a-fk-or]", ""); 
        int partyIndex = cleanMsg.indexOf("Party > ");
        if (partyIndex != -1) {
            String rest = cleanMsg.substring(partyIndex + 8);
            int colonIndex = rest.indexOf(": ");
            if (colonIndex != -1) {
                String potentialName = rest.substring(0, colonIndex).trim();
                String content = rest.substring(colonIndex + 2).trim();
                String actualName = potentialName;
                if (potentialName.contains("] ")) {
                    actualName = potentialName.substring(potentialName.lastIndexOf("] ") + 2);
                } else if (potentialName.contains(" ")) {
                     String[] parts = potentialName.split(" ");
                     actualName = parts[parts.length - 1];
                }
                handlePartyCommand(actualName, content);
            }
        }
    }

    private void handlePartyCommand(String sender, String message) {
        if (mc.player == null) return;
        String cleanMsg = message.trim().toLowerCase();
        
        if (cleanMsg.startsWith("!boom") && cmdBoom.getValue()) {
             String[] SOURATES = { "Inna ma'al usri yusra.", "Hasbuna Allahu wa ni'mal wakeel.", "Rabbana atina fid-dunya hasanatan wa fil-akhirati hasanatan waqina 'adhab an-nar.", "La ilaha illa huwa al-hayyul qayyum.", "Wa qul rabbi zidni 'ilma.", "Innallaha la yughayyiru ma biqawmin hatta yughayyiru ma bianfusihim.", "Ya ayyuhalladhina amanu ista'inu bis-sabri was-salat, innallaha ma'as-sabirin.", "Wa 'ibadur-Rahmanilladhina yamshuna 'alal-ard hawna wa idha khatabahum al-jahiluna qalu salama.", "Wa 'asa an takrahu shay'an wahuwa khayrun lakum, wa 'asa an tuhibbu shay'an wahuwa sharrun lakum, wallahu ya'lamu wa antum la ta'lamun.", "Allahu la ilaha illa huwa al-hayyul qayyum.", "Qul huwa Allahu ahad, Allahu assamad, lam yalid wa lam yulad, wa lam yakun lahu kufuwan ahad.", "Bismillahir rahmanir rahim. Alhamdu lillahi rabbil 'alamin. Ar-rahmanir rahim. Maliki yawmid-din. Iyyaka na'budu wa iyyaka nasta'in. Ihdinas siratal mustaqim. Siratal ladhina an'amta 'alayhim, ghayril maghdubi 'alayhim walad dallin." };
             int random = (int) (Math.random() * SOURATES.length);
             mc.player.connection.sendCommand("pc " + SOURATES[random]);
        }
        else if (cleanMsg.startsWith("!rng") && cmdRng.getValue()) {
            String target = sender; String[] args = message.trim().split(" "); if (args.length > 1) target = args[1];
            int roll = (int) (Math.random() * 101); mc.player.connection.sendCommand("pc " + target + " got " + roll + "% of rng this run!");
        } 
        else if (cleanMsg.startsWith("!gay") && cmdGay.getValue()) {
             String target = sender; String[] args = message.trim().split(" "); if (args.length > 1) target = args[1];
             if (xenus.xenaddons.config.GoatedPlayersManager.isGoated(target.toLowerCase())) { mc.player.connection.sendCommand("pc " + target + " is not gay"); return; }
             boolean isGay = Math.random() < 0.5; String response = isGay ? target + " is gay" : target + " is not gay"; mc.player.connection.sendCommand("pc " + response);
        }
        else if (cleanMsg.startsWith("!racist") && cmdRacist.getValue()) {
             String target = sender; String[] args = message.trim().split(" "); if (args.length > 1) target = args[1];
             int percent = xenus.xenaddons.config.GoatedPlayersManager.isGoated(target.toLowerCase()) ? (int) (Math.random() * 21) + 180 : (int) (Math.random() * 101);
             mc.player.connection.sendCommand("pc " + target + " is racist at " + percent + "% !");
        }
        else if (cleanMsg.startsWith("!py") && cmdPy.getValue()) {
             String target = "Casseur2Metheque"; String[] args = message.trim().split(" "); if (args.length > 1) target = args[1];
             mc.player.connection.sendCommand("pc " + target + " can't py big gwer");
        }
        else if (cleanMsg.startsWith("!gwer") && cmdGwer.getValue()) {
             String[] GWERLIST = { "{pseudo} failed blaze.", "{pseudo} died and became a ghost.", "{pseudo} is a gwer and dropped his hype.", "Look at this gwer : {pseudo} was crushed and became a ghost." };
             String target = "Casseur2Metheque"; String[] args = message.trim().split(" "); if (args.length > 1) target = args[1];
             int random = (int) (Math.random() * GWERLIST.length); String text = GWERLIST[random].replace("{pseudo}", target);
             mc.player.connection.sendCommand("pc " + text);
        }
        else if (cleanMsg.startsWith("!size") && cmdSize.getValue()) {
            String target = sender; String[] args = message.trim().split(" "); if (args.length > 1) target = args[1];
            int size = xenus.xenaddons.config.GoatedPlayersManager.isGoated(target.toLowerCase()) ? (int) (Math.random() * 10) + 15 : (int) (Math.random() * 8) + 1;
            String sizeBar = "8" + "=".repeat(size) + "D";
            mc.player.connection.sendCommand("pc " + target + "'s size: " + sizeBar);
        }
    }
}
