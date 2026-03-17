package xenus.xenaddons.features.impl.dungeons;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import xenus.xenaddons.features.Category;
import xenus.xenaddons.features.Module;
import xenus.xenaddons.settings.NumberSetting;
import xenus.xenaddons.settings.StringSetting;
import xenus.xenaddons.utils.DungeonUtils;

import java.util.Locale;
import java.util.regex.Pattern;

public class MaskReminderModule extends Module {
    private static final Box[] REGIONS = {
            new Box(90, 106, 50, 111, 145, 123),
            new Box(17, 106, 121, 108, 145, 143),
            new Box(-2, 106, 51, 20, 145, 142),
            new Box(-1, 26, 29, 191, 145, 58),
            new Box(3, 5, 0, 128, 48, 140)
    };
    private static final Pattern COLOR_PATTERN = Pattern.compile("§[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);
    private static final String GOLDOR_START = "[boss] goldor: who dares trespass into my domain?";

    private final StringSetting displayText = new StringSetting("Display Text", "&c⚠ MASK NOT EQUIPPED! ⚠", 64);
    private final NumberSetting hudOffsetX = new NumberSetting("HUD Offset X", 0, -1000, 1000, 1);
    private final NumberSetting hudY = new NumberSetting("HUD Y", 60, -1000, 1000, 1);
    private final NumberSetting hudScale = new NumberSetting("HUD Scale", 3.0, 0.5, 6.0, 0.05);

    private int currentRegion = -1;
    private int reminderTicks = 0;
    private boolean shownThisRun = false;

    public MaskReminderModule() {
        super("Mask Reminder", "Shows a mask reminder from boss/lever triggers when you are in leap regions", Category.DUNGEON);

        this.registerSetting(displayText);
        hudOffsetX.setVisibility(() -> false);
        hudY.setVisibility(() -> false);
        hudScale.setVisibility(() -> false);
        this.registerSetting(hudOffsetX);
        this.registerSetting(hudY);
        this.registerSetting(hudScale);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!this.isEnabled()) return;
            if (reminderTicks > 0) reminderTicks--;
            tickRegion();
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (this.isEnabled()) handleChat(message.getString());
        });
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            if (this.isEnabled()) handleChat(message.getString());
            return true;
        });
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (this.isEnabled()) handleChat(message.getString());
            return true;
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (!this.isEnabled() || reminderTicks <= 0) return;
            renderHud(drawContext);
        });
    }

    private void tickRegion() {
        if (!DungeonUtils.inDungeon() || mc.player == null) {
            currentRegion = -1;
            reminderTicks = 0;
            shownThisRun = false;
            return;
        }
        currentRegion = getRegionIndex(mc.player);
    }

    private int getRegionIndex(ClientPlayerEntity player) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        for (int i = 0; i < REGIONS.length; i++) {
            Box box = REGIONS[i];
            if (x >= Math.min(box.minX, box.maxX) && x <= Math.max(box.minX, box.maxX)
                && y >= Math.min(box.minY, box.maxY) && y <= Math.max(box.minY, box.maxY)
                && z >= Math.min(box.minZ, box.maxZ) && z <= Math.max(box.minZ, box.maxZ)) {
                return i;
            }
        }
        return -1;
    }

    private void handleChat(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) return;
        if (currentRegion < 0) return;
        if (shownThisRun) return;
        if (isMaskEquipped()) return;

        String clean = COLOR_PATTERN.matcher(rawMessage).replaceAll("").trim();
        if (clean.isBlank()) return;
        String lower = clean.toLowerCase(Locale.ROOT);

        boolean bossTrigger = lower.contains(GOLDOR_START);
        boolean termTrigger = lower.contains("activated a terminal!")
                || lower.contains("completed a terminal!")
                || lower.contains("activated a lever!")
                || lower.contains("completed a lever!")
                || lower.contains("activated a device!")
                || lower.contains("completed a device!");

        if (bossTrigger || termTrigger) {
            reminderTicks = 20;
            shownThisRun = true;
        }
    }

    private void renderHud(DrawContext drawContext) {
        String text = getDisplayTextFormatted();

        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();
        int textWidth = mc.textRenderer.getWidth(text);
        int textHeight = mc.textRenderer.fontHeight;
        float scale = (float) hudScale.getValue().doubleValue();
        float inv = 1.0f / scale;
        int centerXRaw = (width / 2) + hudOffsetX.getValue().intValue();
        int yRaw = hudY.getValue().intValue();
        int minCenterX = (int) Math.ceil((textWidth * scale) / 2.0f) + 2;
        int maxCenterX = width - minCenterX;
        int centerX = MathHelper.clamp(centerXRaw, minCenterX, Math.max(minCenterX, maxCenterX));
        int maxY = height - (int) Math.ceil(textHeight * scale) - 2;
        int y = MathHelper.clamp(yRaw, 2, Math.max(2, maxY));

        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().scale(scale, scale);
        int x = (int) ((centerX * inv) - (textWidth / 2f));
        int drawY = (int) (y * inv);
        drawContext.drawTextWithShadow(mc.textRenderer, text, x, drawY, 0xFFFF5555);
        drawContext.getMatrices().popMatrix();
    }

    private String getDisplayTextFormatted() {
        String text = displayText.getValue();
        if (text == null || text.isBlank()) text = "&c⚠ MASK NOT EQUIPPED! ⚠";
        return text.replace('&', '§');
    }

    public boolean debugHasMaskEquipped() {
        return isMaskEquipped();
    }

    public void debugTriggerNoMask() {
        reminderTicks = 20;
    }

    private boolean isMaskEquipped() {
        if (mc.player == null) return false;
        ItemStack helmet = mc.player.getEquippedStack(EquipmentSlot.HEAD);
        if (helmet == null || helmet.isEmpty()) return false;

        String name = Formatting.strip(helmet.getName().getString());
        if (name != null) {
            String lowerName = name.toLowerCase(Locale.ROOT);
            if ((lowerName.contains("spirit") || lowerName.contains("bonzo")) && lowerName.contains("mask")) {
                return true;
            }
        }

        String sbId = getSkyblockId(helmet);
        if (sbId == null) return false;
        String upper = sbId.toUpperCase(Locale.ROOT);
        return upper.contains("SPIRIT_MASK") || upper.contains("BONZO_MASK");
    }

    private String getSkyblockId(ItemStack stack) {
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

    @Override
    public void onDisable() {
        reminderTicks = 0;
        shownThisRun = false;
        currentRegion = -1;
    }
}
