package xenus.xenaddons.features.impl.dungeons;

import xenus.xenaddons.hud.HudElement;
import xenus.xenaddons.hud.HudState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.utils.DungeonUtils;

public class AutoPetRuleTitleModule extends Module {
    private static final Component PREVIEW_TITLE = Component.literal("§6Golden Dragon");
    private static final Component PREVIEW_SUBTITLE = Component.literal("§cAutopet");
    private static final float SUBTITLE_SCALE = 0.6f;

    private final BooleanSetting inDungeonsOnly = new BooleanSetting("In Dungeons Only", false);

    public AutoPetRuleTitleModule() {
        super("AutoPet Title", "Shows a title when AutoPet equips a pet", Category.DUNGEON);
        this.registerSetting(inDungeonsOnly);

        hudElement("AutoPet / Loadout Title", this::renderNotification)
            .enabledWhen(() -> isEnabled() || isLoadoutTitleEnabled())
            .visibleWhen(() -> HudState.PET_NOTIFICATION.isActive() && !mc.options.hideGui)
            .centered()
            .defaults(HudElement.CENTER, 250.0f, 2.5f);
    }

    private HudElement.Size renderNotification(GuiGraphicsExtractor context, boolean example) {
        Font font = mc.font;
        Component title = example ? PREVIEW_TITLE : HudState.PET_NOTIFICATION.getTitle();
        Component subtitle = example ? PREVIEW_SUBTITLE : HudState.PET_NOTIFICATION.getSubtitle();

        int titleWidth = font.width(title);
        context.text(font, title, -titleWidth / 2, 0, 0xFFFFFFFF);

        int subtitleWidth = font.width(subtitle);
        float subtitleTop = font.lineHeight + 1.0f;
        context.pose().pushMatrix();
        context.pose().translate(0.0f, subtitleTop);
        context.pose().scale(SUBTITLE_SCALE, SUBTITLE_SCALE);
        context.text(font, subtitle, -subtitleWidth / 2, 0, 0xFFFFFFFF);
        context.pose().popMatrix();

        float width = Math.max(titleWidth, subtitleWidth * SUBTITLE_SCALE);
        return new HudElement.Size(width, subtitleTop + font.lineHeight * SUBTITLE_SCALE);
    }

    private static boolean isLoadoutTitleEnabled() {
        LoadoutTitleModule loadout = ModuleManager.getModule(LoadoutTitleModule.class);
        return loadout != null && loadout.isEnabled();
    }

    public static void init() {
        // Hides original message and triggers our custom one
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            return !checkAndProcess(message.getString());
        });
        
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            return !checkAndProcess(message.getString());
        });

        // Tick Event for our Custom Notification
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            HudState.PET_NOTIFICATION.tick();
        });
    }

    private static boolean checkAndProcess(String msg) {
        AutoPetRuleTitleModule module = null;
        for (Module m : ModuleManager.modules) {
            if (m instanceof AutoPetRuleTitleModule) {
                module = (AutoPetRuleTitleModule) m;
                break;
            }
        }

        if (module != null && module.isEnabled()) {
            return module.handleAndFilter(msg);
        }
        return false;
    }

    private boolean handleAndFilter(String msg) {
        if (msg.contains("Autopet") && msg.contains("equipped your")) {
            String colorCode = "§a"; 
            String petName = "Pet";
            
            try {
                int equippedIndex = msg.indexOf("equipped your");
                if (equippedIndex != -1) {
                    String rest = msg.substring(equippedIndex + 13);
                    int bracketEnd = rest.indexOf("]");
                    if (bracketEnd != -1) {
                        int colorIndex = rest.indexOf("§", bracketEnd);
                        if (colorIndex != -1 && colorIndex + 2 <= rest.length()) {
                            colorCode = rest.substring(colorIndex, colorIndex + 2);
                            int nameStart = colorIndex + 2;
                            int exclamationIndex = rest.indexOf("!", nameStart);
                            if (exclamationIndex != -1) {
                                petName = rest.substring(nameStart, exclamationIndex).trim();
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            
            if (petName.endsWith(" ✦")) {
                petName = petName.substring(0, petName.length() - 2);
            }

            final String finalPetName = petName;
            final String finalColor = colorCode;
            
            mc.execute(() -> showSmallNotification(finalPetName, finalColor));
            
            return true; // Cancel original message
        }
        return false;
    }

    public void debugTrigger() {
        showSmallNotification("Golden Dragon", "§6");
    }

    private void showSmallNotification(String petName, String colorCode) {
        int duration = 8; // 400ms (8 ticks) 

        if (!inDungeonsOnly.getValue() || DungeonUtils.inDungeon()) {
            HudState.PET_NOTIFICATION.show(
                Component.literal(colorCode + petName),
                Component.literal("§cAutopet§r"),
                duration
            );
        }

        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("§cAutopet §7> " + colorCode + petName));
        }
    }
}
