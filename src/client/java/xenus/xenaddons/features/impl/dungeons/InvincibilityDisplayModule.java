package xenus.xenaddons.features.impl.dungeons;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.properties.Property;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import xenus.xenaddons.XenAddons;
import xenus.xenaddons.config.ConfigManager;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.hud.HudElement;
import xenus.xenaddons.settings.BooleanSetting;
import xenus.xenaddons.settings.CollapsibleSetting;
import xenus.xenaddons.settings.ColorSetting;
import xenus.xenaddons.settings.ModeSetting;
import xenus.xenaddons.settings.Setting;
import xenus.xenaddons.settings.StringSetting;
import xenus.xenaddons.utils.DungeonUtils;
import xenus.xenaddons.utils.ItemPriceManager;

public class InvincibilityDisplayModule extends Module {
    private static final Pattern FORMATTING_PATTERN = Pattern.compile("§.");
    private static final Pattern COOLDOWN_PATTERN = Pattern.compile("^Cooldown: (\\d+)s$");
    private static final Pattern PETS_MENU_PATTERN = Pattern.compile("^(?:\\(\\d+/\\d+\\) )?Pets(?: \\(\\d+/\\d+\\))?$");
    private static final Pattern TEXTURE_HASH_PATTERN = Pattern.compile("texture/([0-9a-fA-F]+)");
    private static final Pattern AUTOPET_PATTERN = Pattern.compile("^Autopet equipped your (?<pet>.+?)! VIEW RULE$");
    private static final Pattern SUMMON_PATTERN = Pattern.compile("^You summoned your (?<pet>.+?)!$");
    private static final Pattern DESPAWN_PATTERN = Pattern.compile("^You despawned your .+!$");
    private static final Pattern PET_BRACKETS_PATTERN = Pattern.compile("\\[[^\\]]*\\]");
    private static final Pattern PET_MARKS_PATTERN = Pattern.compile("[✦⭐]");
    private static final String SKINNED_PET_MARK = "✦";
    private static final String PET_SKIN_PREFIX = "PET_SKIN_";
    private static final String SUMMONED_PET_LORE = "Click to despawn";
    private static final int SKIN_SCAN_INTERVAL_TICKS = 10;
    private static final int ROW_HEIGHT = 14;
    private static final int READY_COLOR = 0xFF55FF55;
    private static final int ACTIVE_COLOR = 0xFFFFAA00;
    private static final int COOLDOWN_COLOR = 0xFFFF5555;

    private final CollapsibleSetting displayedItems = new CollapsibleSetting("Displayed Items");
    private final BooleanSetting showSpirit = new BooleanSetting("Show Spirit Mask", true);
    private final BooleanSetting showBonzo = new BooleanSetting("Show Bonzo Mask", true);
    private final BooleanSetting showPhoenix = new BooleanSetting("Show Phoenix Pet", true);

    private final CollapsibleSetting invincibilityHud = new CollapsibleSetting("Invincibility Hud");
    private final BooleanSetting showHud = new BooleanSetting("Invincibility Timer", true);
    private final ModeSetting showWhen = new ModeSetting("Show", "Always", "Always", "Any", "When Active", "On Cooldown");
    private final ColorSetting equippedMaskColor = new ColorSetting("Equipped Mask", 0xFFAA00AA);

    private final StringSetting spiritSkin = new StringSetting("HUD Spirit Mask Skin", "", 128);
    private final StringSetting bonzoSkin = new StringSetting("HUD Bonzo Mask Skin", "", 128);
    private final StringSetting phoenixSkin = new StringSetting("HUD Phoenix Skin", "", 128);
    private final BooleanSetting phoenixSkinned = new BooleanSetting("HUD Phoenix Skinned", true);
    private final BooleanSetting phoenixActive = new BooleanSetting("HUD Phoenix Active", false);

    private ClientLevel lastLevel;
    private int skinScanTicks;

    public InvincibilityDisplayModule() {
        super("Invincibility Display", "Shows your Spirit Mask, Bonzo Mask and Phoenix Pet invincibility and cooldowns", Category.DUNGEON);

        for (Setting<?> setting : List.of(showSpirit, showBonzo, showPhoenix)) {
            setting.setParent(displayedItems);
            this.registerSetting(setting);
        }
        for (Setting<?> setting : List.of(showHud, showWhen, equippedMaskColor)) {
            setting.setParent(invincibilityHud);
            this.registerSetting(setting);
        }
        this.registerSetting(spiritSkin);
        this.registerSetting(bonzoSkin);
        this.registerSetting(phoenixSkin);
        this.registerSetting(phoenixSkinned);
        this.registerSetting(phoenixActive);

        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
        ClientReceiveMessageEvents.GAME.register(this::onGameMessage);
        ClientReceiveMessageEvents.GAME_CANCELED.register(this::onGameMessage);

        hudElement("Invincibility Display", this::renderHud)
            .enabledWhen(() -> isEnabled() && showHud.getValue())
            .visibleWhen(DungeonUtils::inDungeon)
            .defaults(10.0f, 220.0f, 1.0f);
    }

    private void tick() {
        if (mc.level != lastLevel) {
            lastLevel = mc.level;
            for (InvincibilityType type : InvincibilityType.values()) type.reset();
        }
        for (InvincibilityType type : InvincibilityType.values()) type.tick();

        if (!this.isEnabled() || mc.player == null || ++skinScanTicks < SKIN_SCAN_INTERVAL_TICKS) return;
        skinScanTicks = 0;
        scanSkins();
    }

    private void onGameMessage(Component message, boolean overlay) {
        if (!overlay && this.isEnabled()) handleChat(message.getString());
    }

    private void handleChat(String rawMessage) {
        if (rawMessage == null) return;

        String message = strip(rawMessage).strip();
        handlePetMessage(message);
        if (!DungeonUtils.inDungeon()) return;

        for (InvincibilityType type : InvincibilityType.values()) {
            if (!type.pattern.matcher(message).matches()) continue;
            type.proc(type == InvincibilityType.BONZO ? helmetCooldownSeconds() : null);
            return;
        }
    }

    private void handlePetMessage(String message) {
        if (DESPAWN_PATTERN.matcher(message).matches()) {
            setFlag(phoenixActive, false);
            return;
        }

        Matcher autopet = AUTOPET_PATTERN.matcher(message);
        Matcher summon = SUMMON_PATTERN.matcher(message);
        String pet = autopet.matches() ? autopet.group("pet") : summon.matches() ? summon.group("pet") : null;
        if (pet == null) return;

        String withoutBrackets = PET_BRACKETS_PATTERN.matcher(pet).replaceAll("");
        String name = PET_MARKS_PATTERN.matcher(withoutBrackets).replaceAll("").strip();
        boolean phoenix = name.equalsIgnoreCase("Phoenix");
        setFlag(phoenixActive, phoenix);
        if (phoenix) setFlag(phoenixSkinned, withoutBrackets.contains(SKINNED_PET_MARK));
    }

    private Integer helmetCooldownSeconds() {
        if (mc.player == null) return null;
        ItemLore lore = mc.player.getItemBySlot(EquipmentSlot.HEAD).get(DataComponents.LORE);
        if (lore == null) return null;

        List<Component> lines = lore.lines();
        for (int i = lines.size() - 1; i >= 0; i--) {
            Matcher matcher = COOLDOWN_PATTERN.matcher(strip(lines.get(i).getString()).strip());
            if (matcher.matches()) return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private void scanSkins() {
        ItemStack helmet = mc.player.getItemBySlot(EquipmentSlot.HEAD);
        for (InvincibilityType type : List.of(InvincibilityType.SPIRIT, InvincibilityType.BONZO)) {
            ItemStack mask = findMask(type, helmet);
            if (mask != null) updateSkin(type, mask, readString(extraAttributes(mask), "skin"));
        }

        scanPhoenix();
    }

    private ItemStack findMask(InvincibilityType type, ItemStack helmet) {
        if (type.matches(helmet)) return helmet;

        Inventory inventory = mc.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (type.matches(stack)) return stack;
        }
        return null;
    }

    private void scanPhoenix() {
        List<ItemStack> stacks = new ArrayList<>();
        if (mc.screen instanceof AbstractContainerScreen<?> screen
            && PETS_MENU_PATTERN.matcher(strip(screen.getTitle().getString()).strip()).matches()) {
            for (Slot slot : screen.getMenu().slots) stacks.add(slot.getItem());
        }
        Inventory inventory = mc.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) stacks.add(inventory.getItem(i));

        String fallbackHash = null;
        for (ItemStack stack : stacks) {
            JsonObject petInfo = petInfo(stack);
            if (petInfo == null) continue;

            boolean phoenix = "PHOENIX".equals(jsonString(petInfo, "type"));
            boolean summoned = isSummoned(stack);
            if (summoned) setFlag(phoenixActive, phoenix);
            if (!phoenix) continue;

            String hash = resolveSkinHash(InvincibilityType.PHOENIX, stack, phoenixSkinId(stack));
            if (summoned) {
                setFlag(phoenixSkinned, !hash.isEmpty());
                if (!hash.isEmpty()) setSkinSetting(phoenixSkin, hash);
                return;
            }
            if (fallbackHash == null && !hash.isEmpty()) fallbackHash = hash;
        }

        if (fallbackHash != null && phoenixSkin.getValue().isEmpty()) setSkinSetting(phoenixSkin, fallbackHash);
    }

    private static void setFlag(BooleanSetting setting, boolean value) {
        if (setting.getValue() == value) return;

        setting.setValue(value);
        ConfigManager.save();
    }

    private void updateSkin(InvincibilityType type, ItemStack stack, String skinId) {
        setSkinSetting(skinSetting(type), resolveSkinHash(type, stack, skinId));
    }

    private static void setSkinSetting(StringSetting setting, String hash) {
        if (hash.equals(setting.getValue())) return;

        setting.setValue(hash);
        ConfigManager.save();
    }

    private static String resolveSkinHash(InvincibilityType type, ItemStack stack, String skinId) {
        if (skinId == null) return "";

        String known = type.skins.get(skinId.startsWith(PET_SKIN_PREFIX) ? skinId.substring(PET_SKIN_PREFIX.length()) : skinId);
        if (known != null) return known;

        String hash = profileTextureHash(stack);
        return hash == null || hash.equals(type.textureHash) ? "" : hash;
    }

    private static String profileTextureHash(ItemStack stack) {
        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile == null) return null;

        for (Property property : profile.partialProfile().properties().get("textures")) {
            try {
                String decoded = new String(Base64.getDecoder().decode(property.value()), StandardCharsets.UTF_8);
                Matcher matcher = TEXTURE_HASH_PATTERN.matcher(decoded);
                if (matcher.find()) return matcher.group(1).toLowerCase(Locale.ROOT);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    private static boolean isSummoned(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;

        for (Component line : lore.lines()) {
            if (strip(line.getString()).contains(SUMMONED_PET_LORE)) return true;
        }
        return false;
    }

    private static String phoenixSkinId(ItemStack stack) {
        JsonObject petInfo = petInfo(stack);
        String skin = petInfo == null ? null : jsonString(petInfo, "skin");
        return skin == null || skin.equalsIgnoreCase("NONE") || skin.equalsIgnoreCase("null") ? null : skin;
    }

    private static JsonObject petInfo(ItemStack stack) {
        String raw = readString(extraAttributes(stack), "petInfo");
        if (raw == null) return null;

        try {
            JsonElement element = JsonParser.parseString(raw);
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String jsonString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) return null;
        String value = element.getAsString();
        return value.isBlank() ? null : value;
    }

    private static CompoundTag extraAttributes(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;

        CompoundTag root = customData.copyTag();
        return root.getCompound("ExtraAttributes").orElse(root.getCompound("extra_attributes").orElse(root));
    }

    private static String readString(CompoundTag tag, String key) {
        if (tag == null) return null;
        String value = tag.getStringOr(key, "");
        return value.isBlank() ? null : value;
    }

    private static String strip(String text) {
        return FORMATTING_PATTERN.matcher(text).replaceAll("");
    }

    private StringSetting skinSetting(InvincibilityType type) {
        return switch (type) {
            case SPIRIT -> spiritSkin;
            case BONZO -> bonzoSkin;
            case PHOENIX -> phoenixSkin;
        };
    }

    private String displayedSkin(InvincibilityType type) {
        if (type == InvincibilityType.PHOENIX && !phoenixSkinned.getValue()) return "";
        return skinSetting(type).getValue();
    }

    private HudElement.Size renderHud(GuiGraphicsExtractor context, boolean example) {
        List<InvincibilityType> visible = new ArrayList<>();
        for (InvincibilityType type : InvincibilityType.values()) {
            if (example || (isDisplayed(type) && matchesShowWhen(type))) visible.add(type);
        }
        if (visible.isEmpty()) return HudElement.Size.NONE;

        Font font = mc.font;
        ItemStack helmet = mc.player == null ? ItemStack.EMPTY : mc.player.getItemBySlot(EquipmentSlot.HEAD);
        boolean wearingMask = InvincibilityType.SPIRIT.matches(helmet) || InvincibilityType.BONZO.matches(helmet);
        int width = 0;

        for (int i = 0; i < visible.size(); i++) {
            InvincibilityType type = visible.get(i);
            context.item(type.icon(displayedSkin(type)), 0, -1 + i * ROW_HEIGHT);

            int y = i * ROW_HEIGHT + 3;
            boolean equipped = type == InvincibilityType.PHOENIX ? phoenixActive.getValue() && !wearingMask : type.matches(helmet);
            if (equipped) {
                context.fill(14, y, 15, y + 9, equippedMaskColor.getValue());
            }

            String text;
            int color;
            if (type.activeTime > 0) {
                text = seconds(type.activeTime);
                color = ACTIVE_COLOR;
            } else if (type.currentCooldown > 0) {
                text = seconds(type.currentCooldown);
                color = COOLDOWN_COLOR;
            } else {
                text = "✔";
                color = READY_COLOR;
            }
            context.text(font, text, 16, y, color);
            width = Math.max(width, font.width(text));
        }

        return new HudElement.Size(width + 20, visible.size() * ROW_HEIGHT);
    }

    private boolean isDisplayed(InvincibilityType type) {
        return switch (type) {
            case SPIRIT -> showSpirit.getValue();
            case BONZO -> showBonzo.getValue();
            case PHOENIX -> showPhoenix.getValue();
        };
    }

    private boolean matchesShowWhen(InvincibilityType type) {
        return switch (showWhen.getValue()) {
            case "Any" -> type.activeTime > 0 || type.currentCooldown > 0;
            case "When Active" -> type.activeTime > 0;
            case "On Cooldown" -> type.currentCooldown > 0;
            default -> true;
        };
    }

    private static String seconds(int ticks) {
        return String.format(Locale.US, "%.2fs", ticks / 20.0f);
    }

    private enum InvincibilityType {
        SPIRIT(
            "^Second Wind Activated! Your Spirit Mask saved your life!$",
            60, 30,
            "9bbe721d7ad8ab965f08cbec0b834f779b5197f79da4aea3d13d253ece9dec2",
            Set.of("SPIRIT_MASK", "STARRED_SPIRIT_MASK"),
            Map.of(
                "PANDA_SPIRIT", "9649fad8b25b7ddaa1b99b7ee86573ea5a948df4453bfde7d1fe016516ee3e34",
                "WISP_SPIRIT", "111b1c166d1eb3325278a911d5dc1d94d570f70f7d326ee8a0450adb64d78eab"
            )
        ),
        BONZO(
            "^Your (?:. )?Bonzo's Mask saved your life!$",
            60, 180,
            "12716ecbf5b8da00b05f316ec6af61e8bd02805b21eb8e440151468dc656549c",
            Set.of("BONZO_MASK", "STARRED_BONZO_MASK"),
            Map.of(
                "BONZO_MASK_JESTER", "1d93b180a1c707825bb901c6dcd8d16e761b37994527f7f75e3f32feafea9484"
            )
        ),
        PHOENIX(
            "^Your Phoenix Pet saved you from certain death!$",
            80, 60,
            "66b1b59bc890c9c97527787dde20600c8b86f6b9912d51a6bfcdb0e4c2aa3c97",
            Set.of(),
            Map.of(
                "PHEONIX_FLAMINGO", "cdf48d0d3c90e3a08e411ed8e14ee769a86ba9e83def4161f101c72ffcbe821b",
                "PHOENIX_ICE", "12582057e52d0f7fffd1a1f93acf196db5f09b76f1ba3ede28476cc4cd82da97",
                "PHOENIX_SEAGULL", "ae9ccb75cf794bd2b1091562a5e64ea563ccb6913e2c8d11e59c40df8cc6fe73"
            )
        );

        private final Pattern pattern;
        private final int maxInvincibilityTicks;
        private final int maxCooldownSeconds;
        private final String textureHash;
        private final Set<String> helmetIds;
        private final Map<String, String> skins;
        private ItemStack icon;
        private String iconHash;
        private int activeTime;
        private int currentCooldown;

        InvincibilityType(String regex, int maxInvincibilityTicks, int maxCooldownSeconds, String textureHash, Set<String> helmetIds, Map<String, String> skins) {
            this.pattern = Pattern.compile(regex);
            this.maxInvincibilityTicks = maxInvincibilityTicks;
            this.maxCooldownSeconds = maxCooldownSeconds;
            this.textureHash = textureHash;
            this.helmetIds = helmetIds;
            this.skins = skins;
        }

        private boolean matches(ItemStack stack) {
            String id = ItemPriceManager.extractSkyBlockId(stack);
            return id != null && helmetIds.contains(id);
        }

        private ItemStack icon(String skinHash) {
            String hash = skinHash == null || skinHash.isBlank() ? textureHash : skinHash;
            if (icon == null || !hash.equals(iconHash)) {
                icon = XenAddons.CustomButtonData.getSkullStack("skull:" + hash);
                iconHash = hash;
            }
            return icon;
        }

        private void proc(Integer cooldownSeconds) {
            activeTime = maxInvincibilityTicks;
            currentCooldown = (cooldownSeconds != null ? cooldownSeconds : maxCooldownSeconds) * 20;
        }

        private void tick() {
            if (currentCooldown > 0) currentCooldown--;
            if (activeTime > 0) activeTime--;
        }

        private void reset() {
            currentCooldown = 0;
            activeTime = 0;
        }
    }
}
