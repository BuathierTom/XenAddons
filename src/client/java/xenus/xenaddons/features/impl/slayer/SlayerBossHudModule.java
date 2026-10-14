package xenus.xenaddons.features.impl.slayer;

import net.minecraft.network.chat.Component;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.hud.HudElement;
import xenus.xenaddons.hud.HudRender;
import xenus.xenaddons.utils.slayer.SlayerTracker;
import xenus.xenaddons.utils.slayer.SlayerTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the nametag of the slayer boss you spawned on your HUD, so you can read its health without
 * looking at it. Modelled on Athen's Slayer display.
 */
public class SlayerBossHudModule extends Module {

    private static final List<Component> PREVIEW_LINES = List.of(
            Component.literal("§c02:46"),
            Component.literal("§c☠ §bRevenant Horror I §a500§c❤")
    );

    private final BooleanSetting showTimer = new BooleanSetting("Show timer", true);

    public SlayerBossHudModule() {
        super("Slayer Boss HUD", "Shows your slayer boss's nametag and health on screen", Category.SLAYER);

        this.registerSetting(showTimer);

        hudElement("Slayer Boss HUD", (context, example) ->
            HudRender.centeredLines(context, example ? getPreviewLines() : getDisplayLines(), 0xFFFFFFFF))
            .centered()
            .defaults(HudElement.CENTER, 60.0f, 1.0f);
    }

    public static boolean isActive() {
        SlayerBossHudModule module = ModuleManager.getModule(SlayerBossHudModule.class);
        return module != null && module.isEnabled();
    }

    private List<Component> getDisplayLines() {
        List<Component> lines = SlayerTracker.getDisplayLines();
        if (showTimer.getValue()) return lines;

        // Without the timer only the "☠ Name Tier 500❤" line is worth keeping.
        List<Component> filtered = new ArrayList<>(1);
        for (Component line : lines) {
            if (SlayerTypes.containsBossName(SlayerTypes.stripFormatting(line.getString()))) filtered.add(line);
        }
        return filtered;
    }

    private List<Component> getPreviewLines() {
        if (showTimer.getValue()) return PREVIEW_LINES;
        return List.of(PREVIEW_LINES.get(1));
    }
}
