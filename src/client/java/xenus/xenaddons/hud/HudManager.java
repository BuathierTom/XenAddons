package xenus.xenaddons.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;

public final class HudManager {
    private static final List<HudElement> ELEMENTS = new ArrayList<>();
    private static boolean editing;

    private HudManager() {}

    public static void init() {
        for (Module module : ModuleManager.modules) {
            ELEMENTS.addAll(module.hudElements);
        }
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("xenaddons", "hud"), (context, tickCounter) -> render(context));
    }

    public static List<HudElement> elements() {
        return Collections.unmodifiableList(ELEMENTS);
    }

    public static void setEditing(boolean editing) {
        HudManager.editing = editing;
    }

    public static void resetAll() {
        ELEMENTS.forEach(HudElement::reset);
    }

    private static void render(GuiGraphicsExtractor context) {
        if (editing) return;

        HudResolution.push(context);
        for (HudElement element : ELEMENTS) {
            if (element.isEnabled() && element.shouldDraw()) {
                element.render(context, false);
            }
        }
        HudResolution.pop(context);
    }

    public static JsonObject save() {
        JsonObject json = new JsonObject();
        for (HudElement element : ELEMENTS) {
            JsonObject placement = new JsonObject();
            if (element.hasStoredX()) placement.addProperty("x", element.x());
            if (element.hasStoredY()) placement.addProperty("y", element.y());
            placement.addProperty("scale", element.scale());
            json.add(element.name(), placement);
        }
        return json;
    }

    public static void load(JsonObject json) {
        if (json == null) return;

        for (HudElement element : ELEMENTS) {
            JsonElement entry = json.get(element.name());
            if (entry == null || !entry.isJsonObject()) continue;

            JsonObject placement = entry.getAsJsonObject();
            element.setPosition(
                placement.has("x") ? placement.get("x").getAsFloat() : HudElement.CENTER,
                placement.has("y") ? placement.get("y").getAsFloat() : HudElement.CENTER
            );
            if (placement.has("scale")) element.setScale(placement.get("scale").getAsFloat());
        }
    }
}
