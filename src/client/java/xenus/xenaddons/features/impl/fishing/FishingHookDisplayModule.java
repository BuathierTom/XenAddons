package xenus.xenaddons.features.impl.fishing;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.AABB;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.hud.HudElement;
import xenus.xenaddons.hud.HudRender;
import xenus.xenaddons.settings.StringSetting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FishingHookDisplayModule extends Module {

    private static final Pattern TIMER_PATTERN = Pattern.compile("(?:§.)*(?<alert>!!!)|(?:§.)*(?<time>\\d+(?:\\.\\d+)?)");
    private static final double BOBBER_SEARCH_RADIUS = 16.0;
    private static final double STAND_SEARCH_RADIUS = 2.0;

    private final StringSetting alertText = new StringSetting("Custom Alert Text", "!!!", 32);

    private int trackedBobberId = -1;
    private int trackedStandId = -1;
    private String displayText = null;

    public FishingHookDisplayModule() {
        super("Fishing Hook Display", "Shows the Hypixel fishing timer and pull alert on your HUD", Category.FISHING);

        this.registerSetting(alertText);

        ClientTickEvents.END_CLIENT_TICK.register(client -> onTick());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());

        hudElement("Fishing Hook Display", (context, example) ->
            HudRender.centeredText(context, Component.literal(example ? buildAlertText() : displayText), 0xFFFFFFFF))
            .visibleWhen(() -> displayText != null)
            .centered()
            .defaults(HudElement.CENTER, 130.0f, 2.0f);
    }

    private void onTick() {
        if (!this.isEnabled() || mc.player == null || mc.level == null) {
            reset();
            return;
        }

        LocalPlayer player = mc.player;
        FishingHook bobber = findBobber(player);
        if (bobber == null) {
            reset();
            return;
        }

        if (trackedBobberId != bobber.getId()) {
            trackedBobberId = bobber.getId();
            trackedStandId = -1;
            displayText = null;
        }

        ArmorStand stand = findIndicatorStand(bobber);
        if (stand == null) {
            trackedStandId = -1;
            displayText = null;
            return;
        }

        Component name = stand.getCustomName();
        String raw = name == null ? null : name.getString();
        Matcher matcher = raw == null ? null : TIMER_PATTERN.matcher(raw);
        if (matcher == null || !matcher.find()) {
            displayText = null;
            return;
        }

        trackedStandId = stand.getId();
        if (matcher.group("alert") != null) {
            displayText = buildAlertText();
        } else {
            displayText = "§e§l" + matcher.group("time");
        }
    }

    private String buildAlertText() {
        return "§c§l" + alertText.getValue().replace('&', '§');
    }

    private FishingHook findBobber(LocalPlayer player) {
        AABB box = player.getBoundingBox().inflate(BOBBER_SEARCH_RADIUS);
        for (FishingHook hook : mc.level.getEntitiesOfClass(FishingHook.class, box)) {
            if (hook.getPlayerOwner() == player) return hook;
        }
        return null;
    }

    private ArmorStand findIndicatorStand(FishingHook bobber) {
        AABB box = bobber.getBoundingBox().inflate(STAND_SEARCH_RADIUS);
        ArmorStand best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ArmorStand stand : mc.level.getEntitiesOfClass(ArmorStand.class, box)) {
            if (!stand.hasCustomName()) continue;
            double distance = stand.distanceToSqr(bobber);
            if (best == null || distance < bestDistance) {
                best = stand;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void reset() {
        trackedBobberId = -1;
        trackedStandId = -1;
        displayText = null;
    }

    public static boolean shouldHideVanillaIndicator(Entity entity) {
        FishingHookDisplayModule module = ModuleManager.getModule(FishingHookDisplayModule.class);
        if (module == null || !module.isEnabled()) return false;
        return module.trackedStandId != -1 && entity.getId() == module.trackedStandId;
    }
}
