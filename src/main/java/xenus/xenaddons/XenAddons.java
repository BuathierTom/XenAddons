package xenus.xenaddons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.GameProfile;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;

import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.fabricmc.api.ModInitializer;

public class XenAddons implements ModInitializer {
	@Override
	public void onInitialize() {
		load();
		LOGGER.info("XenAddons initialized!");
	}
	public static final Logger LOGGER = LoggerFactory.getLogger("XenAddons");

	private static final Path BASE_DIR = FabricLoader.getInstance().getConfigDir().resolve("xenaddons");
	private static final Path CONFIG_PATH = BASE_DIR.resolve("invbuttons.json");
	private static final Path PROFILES_DIR = BASE_DIR.resolve("profiles");

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static XenAddons instance = new XenAddons();

	// --- Settings ---
	public boolean enabled = true;
	public boolean showTooltips = true;
	public boolean gridSnap = false;
	public boolean hideInCreative = false;
	public boolean hideRecipeBook = false;

	public List<CustomButtonData> buttons = new ArrayList<>();

	// --- GLOBAL CUSTOM TEXTURES ---
	public static final Map<String, Identifier> CUSTOM_TEXTURES = new LinkedHashMap<>();
	static {
		registerCustom("baubles ring", "baubles");
		registerCustom("baubles gold ring", "baubles_gold");
		registerCustom("cross x", "cross");
		registerCustom("green check mark", "green_check");
		registerCustom("white check mark", "white_check");
		registerCustom("question mark help", "question");
		registerCustom("settings cog config", "settings");
		registerCustom("accessory ring", "accessory");
		registerCustom("accessory ring gold", "accessory_gold");
		registerCustom("armor chestplate", "armor");
		registerCustom("armor gold chestplate", "armor_gold");
		registerCustom("pet cat", "pet");
		registerCustom("pet cat gold", "pet_gold");
		registerCustom("skyblock menu", "skyblock_menu");
		registerCustom("recipe book", "recipe");
		registerCustom("search glass", "search");
	}

	private static void registerCustom(String name, String fileName) {
		CUSTOM_TEXTURES.put(name, Identifier.of("xenaddons", "textures/icons/custom/" + fileName + ".png"));
	}

	public static class CustomButtonData {
		public int x, y;
		public String command;
		public String itemId;
		public int backgroundIndex = 0;
		public boolean anchorRight = false;
		public boolean anchorBottom = false;

		public CustomButtonData(int x, int y, String cmd, String item) {
			this.x = x; this.y = y; this.command = cmd; this.itemId = item;
		}

		public ItemStack getItemStack() {
			if (itemId == null || itemId.isEmpty()) return ItemStack.EMPTY;
			if (itemId.startsWith("skull:")) return getSkullStack(itemId);
			try {
				if (!itemId.contains(":")) return ItemStack.EMPTY;
				return new ItemStack(Registries.ITEM.get(Identifier.of(itemId)));
			} catch (Exception e) { return ItemStack.EMPTY; }
		}

		public static ItemStack getSkullStack(String skullStr) {
			try {
				String textureId = skullStr.substring("skull:".length());
				ItemStack head = new ItemStack(Items.PLAYER_HEAD);
				String textureUrl = "https://textures.minecraft.net/texture/" + textureId;
				String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + textureUrl + "\"}}}";
				String base64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
				UUID uuid = UUID.nameUUIDFromBytes(textureId.getBytes(StandardCharsets.UTF_8));
				com.google.common.collect.Multimap<String, com.mojang.authlib.properties.Property> map = com.google.common.collect.HashMultimap.create();
				map.put("textures", new com.mojang.authlib.properties.Property("textures", base64));
				com.mojang.authlib.properties.PropertyMap properties = new com.mojang.authlib.properties.PropertyMap(map);
				GameProfile profile = new GameProfile(uuid, "Skull", properties);
				head.set(DataComponentTypes.PROFILE, ProfileComponent.ofStatic(profile));
				return head;
			} catch (Exception e) {
				LOGGER.error("Failed to create skull stack", e);
				return new ItemStack(Items.PLAYER_HEAD);
			}
		}
	}

    private static List<CustomButtonData> getDefaultButtons() {
        List<CustomButtonData> list = new ArrayList<>();
        list.add(createBtn(97, 17, "/f7", "minecraft:air"));
        list.add(createBtn(156, -20, "/warp dh", "skull:9b56895b9659896ad647f58599238af532d46db9c1b0389b8bbeb70999dab33d"));
        list.add(createBtn(137, -20, "/is", "skull:c9c8881e42915a9d29bb61a16fb26d059913204d265df5b439b3d792acd56"));
        list.add(createBtn(118, -20, "/hub", "skull:d7cc6687423d0570d556ac53e0676cb563bbdd9717cd8269bdebed6f6d4e7bf8"));
        list.add(createBtn(115, 17, "/gfs ender_pearl 16", "minecraft:ender_pearl"));
        list.add(createBtn(97, 61, "/storage", "minecraft:ender_chest"));
        list.add(createBtn(115, 61, "/pet", "minecraft:bone"));
        list.add(createBtn(133, 61, "/wardrobe", "xenaddons:textures/icons/custom/armor.png"));
        list.add(createBtn(151, 61, "/potionsbag", "skull:60226d4c1d30fbebecae939da900603e4cd0fed8592a1bb3e11f9ac92391a45a"));
        list.add(createBtn(153, 27, "/craft", "minecraft:crafting_table"));
        list.add(createBtn(1, -20, "/sacks", "skull:746a6063263ba7208a2a06cef73234791c92d5f67080a365508b02483eefaf9a"));
        list.add(createBtn(21, -20, "/accessories", "skull:8ae187145ee732e7c00905a9c16ed1e43ba88d5624a6f4af829f10453fcc9165"));
        return list;
    }

    private static CustomButtonData createBtn(int x, int y, String cmd, String item) {
        CustomButtonData btn = new CustomButtonData(x, y, cmd, item);
        btn.backgroundIndex = 2;
        return btn;
    }

	public static void load() {
		if (Files.exists(CONFIG_PATH)) {
			try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
				instance = GSON.fromJson(reader, XenAddons.class);
			} catch (IOException e) {
				LOGGER.error("Failed to load config", e);
			}
		} else {
            instance.buttons = getDefaultButtons();
            save();
            saveProfile("Main");
        }
	}

	public static void save() {
		try {
			if (!Files.exists(BASE_DIR)) Files.createDirectories(BASE_DIR);
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to save config", e);
		}
	}

	// --- Profile Management ---

	public static void saveProfile(String name) {
		try {
			if (!Files.exists(PROFILES_DIR)) Files.createDirectories(PROFILES_DIR);
			Path profilePath = PROFILES_DIR.resolve(name + ".json");
			try (Writer writer = Files.newBufferedWriter(profilePath)) {
				GSON.toJson(instance.buttons, writer);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to save profile: " + name, e);
		}
	}

	public static void loadProfile(String name) {
		try {
			Path profilePath = PROFILES_DIR.resolve(name + ".json");
			if (Files.exists(profilePath)) {
				try (Reader reader = Files.newBufferedReader(profilePath)) {
					@SuppressWarnings("unchecked")
					List<CustomButtonData> loaded = (List<CustomButtonData>) GSON.fromJson(reader, new TypeToken<List<CustomButtonData>>(){}.getType());
					if (loaded != null) {
						instance.buttons = loaded;
						save();
					}
				}
			}
		} catch (IOException e) {
			LOGGER.error("Failed to load profile: " + name, e);
		}
	}

	public static void deleteProfile(String name) {
		try {
			Path profilePath = PROFILES_DIR.resolve(name + ".json");
			Files.deleteIfExists(profilePath);
		} catch (IOException e) {
			LOGGER.error("Failed to delete profile: " + name, e);
		}
	}

	public static List<String> getProfileNames() {
		if (!Files.exists(PROFILES_DIR)) return Collections.emptyList();
		try (Stream<Path> stream = Files.list(PROFILES_DIR)) {
			return stream
					.filter(file -> !Files.isDirectory(file) && file.getFileName().toString().endsWith(".json"))
					.map(file -> file.getFileName().toString().replace(".json", ""))
					.collect(Collectors.toList());
		} catch (IOException e) {
			LOGGER.error("Failed to list profiles", e);
			return Collections.emptyList();
		}
	}
}