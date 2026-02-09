package xenus.xenaddons.features.impl.dungeons;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import com.google.gson.JsonArray;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.impl.dungeons.secretroutes.*;
import xenus.xenaddons.features.impl.dungeons.secretroutes.dungeon.DungeonScanner;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.settings.ColorSetting;
import xenus.xenaddons.settings.NumberSetting;

import java.awt.Color;

public class SecretRoutesModule extends Module {

    public static SecretRoutesModule INSTANCE;

    // General
    public final BooleanSetting renderRoutes = new BooleanSetting("Render Routes", true);
    public final BooleanSetting wholeRoute = new BooleanSetting("Full route", false);
    public final BooleanSetting allSteps = new BooleanSetting("Render steps", false);
    public final BooleanSetting allSecrets = new BooleanSetting("All secrets", false);
    
    // Rendering Config
    public final BooleanSetting renderLines = new BooleanSetting("Render Lines", true);
    public final ColorSetting lineColor = new ColorSetting("Line color", new Color(255, 0, 0).getRGB());
    public final NumberSetting lineWidth = new NumberSetting("Line width", 4, 1, 10, 1);
    public final NumberSetting boxAlpha = new NumberSetting("Box Alpha", 0.5, 0, 1, 0.1);

    // Render Toggles (BooleanSetting with expanded property, like Commands)
    public final BooleanSetting renderToggles = new BooleanSetting("Render Toggles", true);
    
    // Feature Toggles (matching SecretRoutesLite defaults)
    public final BooleanSetting renderEtherwarps = new BooleanSetting("Etherwarps", true);
    public final BooleanSetting renderMines = new BooleanSetting("Mines", true);
    public final BooleanSetting renderInteracts = new BooleanSetting("Interacts", false);
    public final BooleanSetting renderSuperboom = new BooleanSetting("Superbooms", true);
    public final BooleanSetting renderEnderpearls = new BooleanSetting("Enderpearls", false);
    public final BooleanSetting renderSecretsItem = new BooleanSetting("Item secrets", false);
    public final BooleanSetting renderSecretInteract = new BooleanSetting("Interact secrets", false);
    public final BooleanSetting renderSecretBat = new BooleanSetting("Bat secrets", false);
    public final BooleanSetting playerWaypointLine = new BooleanSetting("Player waypoint line", false);

    // Internal settings (not exposed in UI)
    public final BooleanSetting etherwarpFullBlock = new BooleanSetting("Etherwarp full block", false);
    public final BooleanSetting mineFullBlock = new BooleanSetting("Mine full block", false);
    public final BooleanSetting interactsFullBlock = new BooleanSetting("Interacts full block", false);
    public final BooleanSetting superboomsFullBlock = new BooleanSetting("Superbooms full block", false);
    public final BooleanSetting enderpearlFullBlock = new BooleanSetting("Enderpearl full block", false);
    public final BooleanSetting secretsItemFullBlock = new BooleanSetting("Item full block", false);
    public final BooleanSetting secretsInteractFullBlock = new BooleanSetting("Interact full block", false);
    public final BooleanSetting secretsBatFullBlock = new BooleanSetting("Bat full block", false);

    public SecretRoutesModule() {
        super("SecretRoutes", "Displays secret routes in dungeons", xenus.xenaddons.features.Category.DUNGEON);
        INSTANCE = this;

        this.registerSetting(renderRoutes);
        this.registerSetting(wholeRoute);
        this.registerSetting(allSteps);
        this.registerSetting(allSecrets);

        // Rendering subcategory
        this.registerSetting(renderLines);
        this.registerSetting(lineColor);
        this.registerSetting(lineWidth);
        this.registerSetting(boxAlpha);

        // Render Toggles section (like Commands)
        this.registerSetting(renderToggles);
        renderToggles.expanded = false; // Collapsed by default
        renderEtherwarps.setVisibility(() -> renderToggles.expanded);
        this.registerSetting(renderEtherwarps);
        renderMines.setVisibility(() -> renderToggles.expanded);
        this.registerSetting(renderMines);
        renderInteracts.setVisibility(() -> renderToggles.expanded);
        this.registerSetting(renderInteracts);
        renderSuperboom.setVisibility(() -> renderToggles.expanded);
        this.registerSetting(renderSuperboom);
        renderEnderpearls.setVisibility(() -> renderToggles.expanded);
        this.registerSetting(renderEnderpearls);
        renderSecretsItem.setVisibility(() -> renderToggles.expanded);
        this.registerSetting(renderSecretsItem);
        renderSecretInteract.setVisibility(() -> renderToggles.expanded);
        this.registerSetting(renderSecretInteract);
        renderSecretBat.setVisibility(() -> renderToggles.expanded);
        this.registerSetting(renderSecretBat);

        // Initialize the sub-systems
        DungeonScanner.init();
        AnotherRenderingUtil.register();
        AnotherRenderingUtil.preRenderCallback = this::onPreRender;
        SecretRoutesEvents.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (this.isEnabled()) {
                OnEnterNewRoom.checkForNewRoom();
                SecretRoutesEvents.onTick(client);
            }
        });
    }

    public void nextSecret() {
        if (OnEnterNewRoom.currentRoom != null) {
            OnEnterNewRoom.currentRoom.nextSecretKeybind();
        }
    }

    public void previousSecret() {
        if (OnEnterNewRoom.currentRoom != null) {
            OnEnterNewRoom.currentRoom.lastSecretKeybind();
        }
    }

    private void onPreRender() {
        if (!this.isEnabled() || !renderRoutes.getValue()) return;
        if (!LocationUtils.isInDungeons()) return;
        if (OnEnterNewRoom.currentRoom == null) return;
        
        // Hide routes when room is completed
        if (OnEnterNewRoom.currentRoom.isCompleted()) return;

        // Update settings bridge
        AnotherRenderingUtil.boxLineWidth = (float) lineWidth.getValue().doubleValue();
        AnotherRenderingUtil.filledBoxAlpha = (float) boxAlpha.getValue().doubleValue();

        try {
            if (allSecrets.getValue()) {
                SecretUtils.renderSecrets();
            } else if (wholeRoute.getValue()) {
                JsonArray csr = OnEnterNewRoom.currentRoom.currentSecretRoute;
                if (csr != null) {
                    for (int i = 0; i < csr.size(); i++) {
                        SecretUtils.renderingCallback(csr.get(i).getAsJsonObject(), i);
                    }
                }
            } else {
                if (OnEnterNewRoom.currentRoom.currentSecretWaypoints != null) {
                    SecretUtils.renderingCallback(OnEnterNewRoom.currentRoom.currentSecretWaypoints, OnEnterNewRoom.currentRoom.currentSecretIndex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
