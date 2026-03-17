package xenus.xenaddons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.features.ModuleManager;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.settings.ColorSetting;
import xenus.xenaddons.settings.KeybindSetting;
import xenus.xenaddons.settings.ModeSetting;
import xenus.xenaddons.settings.NumberSetting;
import xenus.xenaddons.settings.Setting;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {

    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "xenaddons_config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void save() {
        JsonObject json = new JsonObject();
        JsonObject modulesObj = new JsonObject();

        for (Module module : ModuleManager.modules) {
            JsonObject moduleJson = new JsonObject();
            moduleJson.addProperty("enabled", module.isEnabled());

            JsonObject settingsJson = new JsonObject();
            for (Setting<?> setting : module.settings) {
                if (setting instanceof BooleanSetting) {
                    settingsJson.addProperty(setting.name, (Boolean) setting.getValue());
                } else if (setting instanceof ColorSetting) {
                    settingsJson.addProperty(setting.name, (Integer) setting.getValue());
                } else if (setting instanceof NumberSetting) {
                    settingsJson.addProperty(setting.name, (Number) setting.getValue());
                } else if (setting instanceof ModeSetting) {
                    settingsJson.addProperty(setting.name, (String) setting.getValue());
                } else if (setting instanceof KeybindSetting) {
                    settingsJson.addProperty(setting.name, (Integer) setting.getValue());
                }
            }
            moduleJson.add("settings", settingsJson);

            modulesObj.add(module.name, moduleJson);
        }

        json.add("Modules", modulesObj);

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save(); // Create with defaults
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) return;
            
            JsonObject root = rootElement.getAsJsonObject();
            if (!root.has("Modules")) return;
            
            JsonObject modulesObj = root.getAsJsonObject("Modules");

            for (Module module : ModuleManager.modules) {
                if (modulesObj.has(module.name)) {
                    JsonElement moduleElement = modulesObj.get(module.name);
                    if (moduleElement.isJsonObject()) {
                        JsonObject moduleJson = moduleElement.getAsJsonObject();
                        
                        // Load Enabled State
                        if (moduleJson.has("enabled")) {
                            boolean enabled = moduleJson.get("enabled").getAsBoolean();
                            if (enabled != module.isEnabled()) {
                                module.setEnabled(enabled);
                            }
                        }

                        // Load Settings
                        if (moduleJson.has("settings")) {
                            JsonObject settingsJson = moduleJson.getAsJsonObject("settings");
                            for (Setting<?> setting : module.settings) {
                                if (settingsJson.has(setting.name)) {
                                    if (setting instanceof BooleanSetting) {
                                        ((BooleanSetting) setting).setValue(settingsJson.get(setting.name).getAsBoolean());
                                    } else if (setting instanceof ColorSetting) {
                                        ((ColorSetting) setting).setValue(settingsJson.get(setting.name).getAsInt());
                                    } else if (setting instanceof NumberSetting) {
                                        ((NumberSetting) setting).setValue(settingsJson.get(setting.name).getAsDouble());
                                    } else if (setting instanceof ModeSetting) {
                                        ((ModeSetting) setting).setValue(settingsJson.get(setting.name).getAsString());
                                    } else if (setting instanceof KeybindSetting) {
                                        ((KeybindSetting) setting).setValue(settingsJson.get(setting.name).getAsInt());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
