package xenus.xenaddons.features.impl.misc;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import xenus.xenaddons.features.impl.misc.cooldown.AbilityColor;
import xenus.xenaddons.features.impl.misc.cooldown.ItemAbility;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CooldownItemsTracker {
    private static final Pattern ABILITY_USE_PATTERN = Pattern.compile(".*-\\d+ Mana \\((?<type>.*)\\).*");
    private static final Pattern YOU_ALIGNED_OTHERS_PATTERN = Pattern.compile("You aligned .* other players?!");
    private static boolean initialized = false;
    private static String lastAbility = "";
    private static int ticks = 0;

    private static final Map<String, List<ItemAbility>> abilityItems = new HashMap<>();
    private static final Map<String, List<ItemText>> textsByIdentifier = new HashMap<>();

    private CooldownItemsTracker() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!CooldownItemsModule.isActive()) return;
            if (client.player == null || client.world == null) return;
            tick(client);
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!CooldownItemsModule.isActive()) return;
            String raw = message.getString();
            if (overlay) onActionBar(raw);
            else onChat(raw);
        });

        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            if (!CooldownItemsModule.isActive()) return true;
            onChat(message.getString());
            return true;
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    public static void onItemRightClick(ItemStack itemInHand) {
        if (!CooldownItemsModule.isActive()) return;
        if (itemInHand == null || itemInHand.isEmpty()) return;
        List<ItemAbility> abilities = hasAbility(itemInHand);
        for (ItemAbility ability : abilities) {
            ability.setItemClick();
        }

        if (abilities.contains(ItemAbility.WITHER_IMPACT)) {
            activateIfReady(ItemAbility.WITHER_IMPACT);
        }
        if (abilities.contains(ItemAbility.GYROKINETIC_WAND_RIGHT)) {
            activateIfReady(ItemAbility.GYROKINETIC_WAND_RIGHT);
        }
        if (abilities.contains(ItemAbility.ICE_SPRAY_WAND)) {
            activateIfReady(ItemAbility.ICE_SPRAY_WAND);
        }
        if (abilities.contains(ItemAbility.WEIRD_TUBA)) {
            activateIfReady(ItemAbility.WEIRD_TUBA);
        }
        if (abilities.contains(ItemAbility.WEIRDER_TUBA)) {
            activateIfReady(ItemAbility.WEIRDER_TUBA);
        }
        if (abilities.contains(ItemAbility.SWORD_OF_BAD_HEALTH)) {
            activateIfReady(ItemAbility.SWORD_OF_BAD_HEALTH);
        }
        if (abilities.contains(ItemAbility.ROGUE_SWORD)) {
            activateIfReady(ItemAbility.ROGUE_SWORD);
        }
        if (abilities.contains(ItemAbility.RAGNAROCK_AXE)) {
            activateIfReady(ItemAbility.RAGNAROCK_AXE, AbilityColor.WHITE, 3_000);
        }
    }

    public static void onItemLeftClick(ItemStack itemInHand) {
        if (!CooldownItemsModule.isActive()) return;
        if (itemInHand == null || itemInHand.isEmpty()) return;
        List<ItemAbility> abilities = hasAbility(itemInHand);
        for (ItemAbility ability : abilities) {
            ability.setItemClick();
        }
        if (abilities.contains(ItemAbility.GYROKINETIC_WAND_LEFT)) {
            activateIfReady(ItemAbility.GYROKINETIC_WAND_LEFT);
        }
    }

    public static String debugGetItemId(ItemStack stack) {
        return getItemId(stack);
    }

    public static String debugGetItemUuid(ItemStack stack) {
        return getItemUuid(stack);
    }

    public static List<String> debugGetAbilities(ItemStack stack) {
        List<String> out = new ArrayList<>();
        for (ItemAbility ability : hasAbility(stack)) {
            out.add(ability.name());
        }
        return out;
    }

    public static void renderBackground(DrawContext context, ItemStack stack, int x, int y, boolean guiOpen) {
        if (!CooldownItemsModule.isActive()) return;
        CooldownItemsModule module = CooldownItemsModule.get();
        if (module == null || !module.cooldownBackground.getValue()) return;
        ItemText itemText = findRenderableText(stack, guiOpen);
        if (itemText == null) return;

        int opacity = 130;
        if (itemText.color == AbilityColor.GREEN) {
            opacity = 80;
            if (!module.showWhenReady.getValue()) return;
        }

        int argb = ((opacity & 0xFF) << 24) | (itemText.color.rgb & 0xFFFFFF);
        context.fill(x, y, x + 16, y + 16, argb);
    }

    public static void renderText(DrawContext context, TextRenderer textRenderer, ItemStack stack, int x, int y, boolean guiOpen) {
        if (!CooldownItemsModule.isActive()) return;
        ItemText itemText = findRenderableText(stack, guiOpen);
        if (itemText == null) return;
        int w = textRenderer.getWidth(itemText.text);
        int drawX = x + 19 - 2 - w + (itemText.alternativePosition ? -8 : 0);
        int drawY = y + 9 + (itemText.alternativePosition ? -10 : 0);
        context.drawTextWithShadow(textRenderer, itemText.text, drawX, drawY, 0xFF000000 | itemText.color.rgb);
    }

    public static java.util.List<OverlayText> getOverlayTexts(ItemStack stack, boolean guiOpen) {
        java.util.List<OverlayText> out = new java.util.ArrayList<>();
        if (!CooldownItemsModule.isActive()) return out;
        if (stack == null || stack.isEmpty()) return out;
        String id = getIdentifier(stack);
        if (id == null) return out;
        List<ItemText> list = textsByIdentifier.get(id);
        if (list == null || list.isEmpty()) return out;
        CooldownItemsModule module = CooldownItemsModule.get();
        boolean showReadyInInventory = module != null && module.seeInInventory.getValue();
        for (ItemText itemText : list) {
            if (guiOpen && !itemText.onCooldown && !showReadyInInventory) continue;
            if (itemText.text.isEmpty()) continue;
            out.add(new OverlayText(itemText.text, itemText.alternativePosition, itemText.color.rgb));
        }
        return out;
    }

    private static void tick(MinecraftClient client) {
        ticks++;
        refreshItems(client, ticks % 10 == 0 || abilityItems.isEmpty());
    }

    private static void refreshItems(MinecraftClient client, boolean rescanInventory) {
        PlayerEntity player = client.player;
        if (player == null) return;

        if (rescanInventory) {
            abilityItems.clear();
            var inventory = player.getInventory();
            for (int i = 0; i < inventory.size(); i++) {
                putItemAbility(inventory.getStack(i));
            }
        }

        textsByIdentifier.clear();
        for (Map.Entry<String, List<ItemAbility>> entry : abilityItems.entrySet()) {
            List<ItemText> list = new ArrayList<>();
            for (ItemAbility ability : entry.getValue()) {
                list.add(createItemText(ability));
            }
            textsByIdentifier.put(entry.getKey(), list);
        }
    }

    private static void putItemAbility(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        String id = getIdentifier(stack);
        if (id == null || id.isBlank()) return;
        List<ItemAbility> list = hasAbility(stack);
        if (list.isEmpty()) return;
        abilityItems.put(id, list);
    }

    private static List<ItemAbility> hasAbility(ItemStack stack) {
        String itemName = stripFormatting(stack.getName().getString());
        String internalName = getItemId(stack);
        Set<ItemAbility> scrolls = ItemAbility.getAllAbilityScrolls(stack);

        LinkedHashSet<ItemAbility> set = new LinkedHashSet<>(scrolls);
        for (ItemAbility ability : ItemAbility.values()) {
            if (internalName != null) {
                if (ability.internalNames.contains(internalName)) {
                    set.add(ability);
                }
                if (internalName.equals(ability.name())) {
                    set.add(ability);
                }
                for (String name : ability.itemNames) {
                    if (internalName.equals(toInternalName(name))) {
                        set.add(ability);
                        break;
                    }
                }
            }

            for (String name : ability.itemNames) {
                if (itemName.contains(name)) {
                    set.add(ability);
                    break;
                }
            }
        }
        return new ArrayList<>(set);
    }

    private static String stripFormatting(String input) {
        if (input == null) return "";
        return input.replaceAll("(?i)[§&][0-9A-FK-OR]", "");
    }

    private static String toInternalName(String displayName) {
        if (displayName == null) return "";
        return displayName
                .toUpperCase(java.util.Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private static ItemText createItemText(ItemAbility ability) {
        CooldownItemsModule module = CooldownItemsModule.get();
        boolean showWhenReady = module != null && module.showWhenReady.getValue();
        AbilityColor specialColor = ability.getSpecialColor();

        if (ability.isOnCooldown()) {
            long duration = ability.remainingCooldownMs();
            AbilityColor color = specialColor != null ? specialColor : (duration < 600 ? AbilityColor.RED : AbilityColor.YELLOW);
            return new ItemText(color, ability.getDurationText(), true, ability.alternativePosition);
        }

        if (specialColor != null) {
            ability.clearSpecialColor();
            tryHandleNextPhase(ability, specialColor);
            return createItemText(ability);
        }

        return new ItemText(AbilityColor.GREEN, showWhenReady ? "R" : "", false, ability.alternativePosition);
    }

    private static void tryHandleNextPhase(ItemAbility ability, AbilityColor specialColor) {
        if (ability == ItemAbility.RAGNAROCK_AXE && specialColor == AbilityColor.WHITE) {
            ability.activate(AbilityColor.DARK_PURPLE, 10_000);
        } else if (ability == ItemAbility.RAGNAROCK_AXE && specialColor == AbilityColor.DARK_PURPLE) {
            ability.activate(AbilityColor.YELLOW, 7_000);
        } else if (ability == ItemAbility.WITHER_SHIELD_SCROLL && specialColor == AbilityColor.DARK_PURPLE) {
            ability.activate(null, Math.max((int) (10_000 * ability.getMultiplier()) - 5_000, 0));
        }
    }

    private static ItemText findRenderableText(ItemStack stack, boolean guiOpen) {
        if (stack == null || stack.isEmpty()) return null;
        String id = getIdentifier(stack);
        if (id == null) return null;
        List<ItemText> list = textsByIdentifier.get(id);
        if (list == null || list.isEmpty()) return null;
        for (ItemText text : list) {
            if (guiOpen && !text.onCooldown) continue;
            if (!text.text.isEmpty()) return text;
        }
        return null;
    }

    private static void onActionBar(String message) {
        Matcher matcher = ABILITY_USE_PATTERN.matcher(message);
        if (matcher.matches()) {
            String name = matcher.group("type");
            if (!Objects.equals(name, lastAbility)) {
                lastAbility = name;
                for (ItemAbility ability : ItemAbility.values()) {
                    if (ability.actionBarDetection && ability.abilityName.equals(name)) {
                        ability.activate();
                        break;
                    }
                }
            }
            return;
        }
        lastAbility = "";

    }

    private static void onChat(String message) {
        if (message.equals("Creeper Veil Activated!")) {
            ItemAbility.WITHER_CLOAK.activate(AbilityColor.LIGHT_PURPLE, (int) ItemAbility.WITHER_CLOAK.getCooldownMs());
        }
        if (message.equals("Creeper Veil De-activated! (Expired)") ||
            message.equals("Not enough mana! Creeper Veil De-activated!")) {
            ItemAbility.WITHER_CLOAK.activate();
        }
        if (message.equals("Creeper Veil De-activated!")) {
            ItemAbility.WITHER_CLOAK.activate(null, 5_000);
        }
    }

    private static String getIdentifier(ItemStack stack) {
        String uuid = getItemUuid(stack);
        if (uuid != null && !uuid.isBlank()) return uuid;
        return getItemId(stack);
    }

    private static String getItemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) return null;
        NbtCompound root = customData.copyNbt();
        if (root == null) return null;
        NbtCompound extra = root.getCompound("ExtraAttributes").orElse(root.getCompound("extra_attributes").orElse(null));
        if (extra != null) {
            String id = extra.getString("id", "");
            if (id != null && !id.isBlank()) return id;
            id = extra.getString("ID", "");
            if (id != null && !id.isBlank()) return id;
        }
        String direct = root.getString("id", "");
        if (direct != null && !direct.isBlank()) return direct;
        direct = root.getString("ID", "");
        if (direct != null && !direct.isBlank()) return direct;
        return null;
    }

    private static String getItemUuid(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) return null;
        NbtCompound root = customData.copyNbt();
        if (root == null) return null;
        NbtCompound extra = root.getCompound("ExtraAttributes").orElse(root.getCompound("extra_attributes").orElse(null));
        if (extra == null) return null;
        String uuid = extra.getString("uuid", "");
        if (uuid != null && !uuid.isBlank()) return uuid;
        uuid = extra.getString("UUID", "");
        return (uuid == null || uuid.isBlank()) ? null : uuid;
    }

    private static void reset() {
        lastAbility = "";
        ticks = 0;
        abilityItems.clear();
        textsByIdentifier.clear();
        for (ItemAbility ability : ItemAbility.values()) {
            ability.resetState();
        }
    }

    private static void activateIfReady(ItemAbility ability) {
        if (ability == null || ability.isOnCooldown()) return;
        ability.activate();
    }

    private static void activateIfReady(ItemAbility ability, AbilityColor color, int customCooldownMs) {
        if (ability == null || ability.isOnCooldown()) return;
        ability.activate(color, customCooldownMs);
    }

    private record ItemText(AbilityColor color, String text, boolean onCooldown, boolean alternativePosition) {
    }

    public record OverlayText(String text, boolean alternativePosition, int colorRgb) {
    }
}
