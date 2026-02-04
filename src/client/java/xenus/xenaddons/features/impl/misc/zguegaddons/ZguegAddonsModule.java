package xenus.xenaddons.features.impl.misc.zguegaddons;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.BooleanSetting;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// A module that acts as a container for ZguegAddons features
public class ZguegAddonsModule extends Module {
    
    public final BooleanSetting chatHider = new BooleanSetting("Chat Hider", false);
    public final BooleanSetting cryptsNotify = new BooleanSetting("Crypts Notify", false);
    public final BooleanSetting classNotify = new BooleanSetting("Class Notify", false);
    public final BooleanSetting partyCommands = new BooleanSetting("Party Commands", false);
    
    private final String[] RADIO_MESSAGES = {
        "Your radio is weak. Find another enjoyer to boost it.",
        "Your radio signal is strong!",
        "Your radio lost signal. There's too many enjoyers on this channel."
    };
    
    private final String DISCORD_WARNING = "Please be mindful of Discord links in chat as they may pose a security risk";

    public ZguegAddonsModule() {
        super("ZguegAddons", "Collection of ZguegAddons features", Category.MISC);
        this.registerSetting(chatHider);
        this.registerSetting(cryptsNotify);
        this.registerSetting(classNotify);
        this.registerSetting(partyCommands);
        
        // Register Event Listeners
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
             
             // Class Notify
             if (classNotify.getValue()) {
                 if (msg.contains("You have selected the") && msg.contains("Dungeon Class!") && !msg.contains(":")) {
                      if (mc.player != null) {
                         mc.player.networkHandler.sendChatCommand("pc " + msg);
                      }
                 }
             }
        });
    }
    
    private boolean handleChatHide(Text message) {
        String msg = message.getString(); // Raw text
        // System.out.println("[XenAddons DEBUG] Received Chat/Game Message: " + msg);
        
        // Always Check for Party Commands first (even if hiding is disabled)
        // We use unformatted string for Regex to avoid color code issues
        if (partyCommands.getValue()) {
            processPartyMessage(msg);
        }

        if (!chatHider.getValue()) return true;
        
        // Radio Messages (Hide completely)
        // ... rest of method
        for (String criteria : RADIO_MESSAGES) {
            if (msg.contains(criteria)) return false;
        }
        
        // Discord Warning (Hide partial)
        if (msg.contains(DISCORD_WARNING)) {
            // Split by newline
            String[] lines = msg.split("\n");
            boolean modified = false;
            
            for (String line : lines) {
                if (!line.contains(DISCORD_WARNING)) {
                    // Re-print the clean line locally
                     if (mc.player != null) {
                         mc.player.sendMessage(Text.literal(line), false);
                     }
                } else {
                    modified = true;
                }
            }
            // If we found the warning, we cancel the original event because we re-printed valid lines above
            if (modified) return false;
        }
        
        return true;
    }

    private void processPartyMessage(String msg) {
        // Strip color codes properly. 
        // Just replacing "§" leaves the color character (e.g. §a -> a). 
        // We use regex to remove § followed by a valid color code char.
        String cleanMsg = msg.replaceAll("(?i)§[0-9a-fk-or]", ""); 
        
        // Simple manual parsing to avoid strict regex fails
        // Looking for "Party > "
        int partyIndex = cleanMsg.indexOf("Party > ");
        // System.out.println("[XenAddons DEBUG] Checking for 'Party > ': Index=" + partyIndex + " in '" + cleanMsg + "'");

        if (partyIndex != -1) {
            // Extract everything after "Party > "
            String rest = cleanMsg.substring(partyIndex + 8); // 8 is len of "Party > "
            // System.out.println("[XenAddons DEBUG] Rest of message: " + rest);
            
            // Format is now "[Rank] Name: Message" or "Name: Message"
            // Find the colon separating Name and Message
            int colonIndex = rest.indexOf(": ");
            if (colonIndex != -1) {
                String potentialName = rest.substring(0, colonIndex).trim();
                String content = rest.substring(colonIndex + 2).trim();
                
                // Clean Name from Rank [MVP+] etc
                String actualName = potentialName;
                if (potentialName.contains("] ")) {
                    actualName = potentialName.substring(potentialName.lastIndexOf("] ") + 2);
                } else if (potentialName.contains(" ")) {
                     // Fallback: take last word
                     String[] parts = potentialName.split(" ");
                     actualName = parts[parts.length - 1];
                }
                
                // System.out.println("[XenAddons DEBUG] Parsed Sender: " + actualName + ", Content: " + content);
                handlePartyCommand(actualName, content);
            } else {
                 // System.out.println("[XenAddons DEBUG] No colon found in rest.");
            }
        }
    }

    private void handlePartyCommand(String sender, String message) {
        if (mc.player == null) return;
        
        String cleanMsg = message.trim().toLowerCase();
        
        // !boom command
        if (cleanMsg.startsWith("!boom")) {
             String[] SOURATES = {
                "Inna ma'al usri yusra.",
                "Hasbuna Allahu wa ni'mal wakeel.",
                "Rabbana atina fid-dunya hasanatan wa fil-akhirati hasanatan waqina 'adhab an-nar.",
                "La ilaha illa huwa al-hayyul qayyum.",
                "Wa qul rabbi zidni 'ilma.",
                "Innallaha la yughayyiru ma biqawmin hatta yughayyiru ma bianfusihim.",
                "Ya ayyuhalladhina amanu ista'inu bis-sabri was-salat, innallaha ma'as-sabirin.",
                "Wa 'ibadur-Rahmanilladhina yamshuna 'alal-ard hawna wa idha khatabahum al-jahiluna qalu salama.",
                "Wa 'asa an takrahu shay'an wahuwa khayrun lakum, wa 'asa an tuhibbu shay'an wahuwa sharrun lakum, wallahu ya'lamu wa antum la ta'lamun.",
                "Allahu la ilaha illa huwa al-hayyul qayyum.",
                "Qul huwa Allahu ahad, Allahu assamad, lam yalid wa lam yulad, wa lam yakun lahu kufuwan ahad.",
                "Bismillahir rahmanir rahim. Alhamdu lillahi rabbil 'alamin. Ar-rahmanir rahim. Maliki yawmid-din. Iyyaka na'budu wa iyyaka nasta'in. Ihdinas siratal mustaqim. Siratal ladhina an'amta 'alayhim, ghayril maghdubi 'alayhim walad dallin."
            };
            int random = (int) (Math.random() * SOURATES.length);
            mc.player.networkHandler.sendChatCommand("pc " + SOURATES[random]);
        }
        // !rng command
        else if (cleanMsg.startsWith("!rng")) {
            String target = sender;
            String[] args = message.trim().split(" ");
            if (args.length > 1) target = args[1];
            
            int roll = (int) (Math.random() * 101);
            mc.player.networkHandler.sendChatCommand("pc " + target + " got " + roll + "% of rng this run!");
        } 
        // !gay command
        else if (cleanMsg.startsWith("!gay")) {
             String target = sender;
             String[] args = message.trim().split(" ");
             if (args.length > 1) target = args[1];
             
             String targetLower = target.toLowerCase();
             if (targetLower.equals("xen62") || targetLower.equals("xen") || targetLower.equals("xenus") || targetLower.equals("xenus_")) {
                 mc.player.networkHandler.sendChatCommand("pc " + target + " is not gay");
                 return;
             }
             
             boolean isGay = Math.random() < 0.5;
             String response = isGay ? target + " is gay" : target + " is not gay";
             mc.player.networkHandler.sendChatCommand("pc " + response);
        }
        // !racist command
        else if (cleanMsg.startsWith("!racist")) {
             String target = sender;
             String[] args = message.trim().split(" ");
             if (args.length > 1) target = args[1];
             
             int percent;
             String targetLower = target.toLowerCase();
             if (targetLower.equals("xen62") || targetLower.equals("xen") || targetLower.equals("xenus")) {
                 percent = (int) (Math.random() * 21) + 180; // 180-200%
             } else {
                 percent = (int) (Math.random() * 101); // 0-100%
             }
             mc.player.networkHandler.sendChatCommand("pc " + target + " is racist at " + percent + "% !");
        }
        // !py command
        else if (cleanMsg.startsWith("!py")) {
             String target = "Casseur2Metheque";
             String[] args = message.trim().split(" ");
             if (args.length > 1) target = args[1];
             mc.player.networkHandler.sendChatCommand("pc " + target + " can't py big gwer");
        }
        // !gwer command
        else if (cleanMsg.startsWith("!gwer")) {
             String[] GWERLIST = {
                "{pseudo} failed blaze.",
                "{pseudo} died and became a ghost.",
                "{pseudo} is a gwer and dropped his hype.",
                "Look at this gwer : {pseudo} was crushed and became a ghost."
             };
             String target = "Casseur2Metheque";
             String[] args = message.trim().split(" ");
             if (args.length > 1) target = args[1];
             
             int random = (int) (Math.random() * GWERLIST.length);
             String text = GWERLIST[random].replace("{pseudo}", target);
             mc.player.networkHandler.sendChatCommand("pc " + text);
        }
        // !size command
        else if (cleanMsg.startsWith("!size")) {
            String target = sender;
            String[] args = message.trim().split(" ");
            if (args.length > 1) target = args[1];
            
            String targetLower = target.toLowerCase();
            int size;
            if (targetLower.equals("xen62") || targetLower.equals("xen") || targetLower.equals("xenus")) {
                 size = (int) (Math.random() * 10) + 15; // 15-24
            } else {
                 size = (int) (Math.random() * 8) + 1; // 1-8
            }
            
            String sizeBar = "8" + "=".repeat(size) + "D";
            mc.player.networkHandler.sendChatCommand("pc " + target + "'s size: " + sizeBar);
        }
        // !911 command
        else if (cleanMsg.startsWith("!911")) {
             mc.player.networkHandler.sendChatCommand("pc ✈ | |");
        }
        // !help command
        else if (cleanMsg.startsWith("!help")) {
             mc.player.networkHandler.sendChatCommand("pc Commands: !rng, !boom, !gay, !racist, !py, !gwer, !size, !911");
        }
    }
}
