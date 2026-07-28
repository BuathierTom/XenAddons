package xenus.xenaddons.render.music.font;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import xenus.xenaddons.XenAddons;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class MusicMsdfFontManager {
    private static final Map<String, MusicMsdfFont> FONTS = new HashMap<>();

    private MusicMsdfFontManager() {}

    public static MusicMsdfFont get(String name, float nominalSize) {
        String key = name + "_" + nominalSize;
        if (FONTS.containsKey(key)) return FONTS.get(key);

        Identifier imageId = Identifier.fromNamespaceAndPath("xenaddons", "music/fonts/" + name + ".png");
        Identifier jsonId = Identifier.fromNamespaceAndPath("xenaddons", "music/fonts/" + name + ".json");
        try (
            InputStream image = Minecraft.getInstance().getResourceManager().open(imageId);
            InputStream json = Minecraft.getInstance().getResourceManager().open(jsonId)
        ) {
            MusicMsdfFont font = new MusicMsdfFont(image, json, nominalSize);
            FONTS.put(key, font);
            return font;
        } catch (Exception exception) {
            XenAddons.LOGGER.error("Failed to load music display font {}", name, exception);
            return null;
        }
    }

    public static void close() {
        for (MusicMsdfFont font : FONTS.values()) {
            font.close();
        }
        FONTS.clear();
    }
}
