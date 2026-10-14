package xenus.xenaddons.gui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import xenus.xenaddons.XenAddons;
import net.minecraft.client.gui.components.EditBox;
import xenus.xenaddons.utils.HypixelItemManager;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import xenus.xenaddons.compat.XenDrawContextCompat;

import java.awt.Point;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GuiInvButtonEditor extends Screen {

    private static final Identifier INVENTORY_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/container/inventory.png");
    private static final Identifier BUTTONS_TEXTURE = Identifier.fromNamespaceAndPath("xenaddons", "textures/gui/buttons.png");
    private static final Identifier INFO_ICON_TEXTURE = Identifier.fromNamespaceAndPath("xenaddons", "textures/gui/icons/info.png");

    private static final int TEXTURE_WIDTH = 90;
    private static final int TEXTURE_HEIGHT = 36;

    private static final String SEARCH_HINT = "Search or paste a skull id";
    private static final String ICON_SECTION_LABEL = "Icon";
    /** Mojang texture hashes are long hex strings; short queries stay plain text searches. */
    private static final java.util.regex.Pattern SKULL_TEXTURE_PATTERN = java.util.regex.Pattern.compile("[0-9a-fA-F]{24,}");

    private static final int BUTTON_SIZE = 18;
    private static final int OUTER_PADDING = 2;
    private static final int OUTER_GRID_SIZE = BUTTON_SIZE + OUTER_PADDING;
    private static final int TOP_BOTTOM_START_X = 8;

    private static final List<Point> INVENTORY_FIXED_SLOTS = new ArrayList<>();
    static {
        INVENTORY_FIXED_SLOTS.add(new Point(25, 8));
        INVENTORY_FIXED_SLOTS.add(new Point(57, 8));
        INVENTORY_FIXED_SLOTS.add(new Point(25, 60));
        INVENTORY_FIXED_SLOTS.add(new Point(57, 60));
        INVENTORY_FIXED_SLOTS.add(new Point(97, 17));
        INVENTORY_FIXED_SLOTS.add(new Point(115, 17));
        INVENTORY_FIXED_SLOTS.add(new Point(97, 35));
        INVENTORY_FIXED_SLOTS.add(new Point(115, 35));
        INVENTORY_FIXED_SLOTS.add(new Point(152, 27));
        INVENTORY_FIXED_SLOTS.add(new Point(98, 61));
        INVENTORY_FIXED_SLOTS.add(new Point(116, 61));
        INVENTORY_FIXED_SLOTS.add(new Point(134, 61));
        INVENTORY_FIXED_SLOTS.add(new Point(152, 61));
    }

    private static final Map<String, String> SKULL_ICONS = new HashMap<>();
    static {
        SKULL_ICONS.put("Personal Bank", "skull:e36e94f6c34a35465fce4a90f2e25976389eb9709a12273574ff70fd4daa6852");
        SKULL_ICONS.put("Skyblock Hub", "skull:d7cc6687423d0570d556ac53e0676cb563bbdd9717cd8269bdebed6f6d4e7bf8");
        SKULL_ICONS.put("Private Island", "skull:c9c8881e42915a9d29bb61a16fb26d059913204d265df5b439b3d792acd56");
        SKULL_ICONS.put("Castle", "skull:f4559d75464b2e40a518e4de8e6cf3085f0a3ca0b1b7012614c4cd96fed60378");
        SKULL_ICONS.put("Sirius Shack", "skull:7ab83858ebc8ee85c3e54ab13aabfcc1ef2ad446d6a900e471c3f33b78906a5b");
        SKULL_ICONS.put("Crypts", "skull:25d2f31ba162fe6272e831aed17f53213db6fa1c4cbe4fc827f3963cc98b9");
        SKULL_ICONS.put("Spiders Den", "skull:c754318a3376f470e481dfcd6c83a59aa690ad4b4dd7577fdad1c2ef08d8aee6");
        SKULL_ICONS.put("Top Of The Nest", "skull:9d7e3b19ac4f3dee9c5677c135333b9d35a7f568b63d1ef4ada4b068b5a25");
        SKULL_ICONS.put("The End", "skull:7840b87d52271d2a755dedc82877e0ed3df67dcc42ea479ec146176b02779a5");
        SKULL_ICONS.put("The End Dragons Nest", "skull:a1cd6d2d03f135e7c6b5d6cdae1b3a68743db4eb749faf7341e9fb347aa283b");
        SKULL_ICONS.put("The Park", "skull:a221f813dacee0fef8c59f76894dbb26415478d9ddfc44c2e708a6d3b7549b");
        SKULL_ICONS.put("The Park Jungle", "skull:79ca3540621c1c79c32bf42438708ff1f5f7d0af9b14a074731107edfeb691c");
        SKULL_ICONS.put("The Park Howling Cave", "skull:1832d53997b451635c9cf9004b0f22bb3d99ab5a093942b5b5f6bb4e4de47065");
        SKULL_ICONS.put("Gold Mines", "skull:73bc965d579c3c6039f0a17eb7c2e6faf538c7a5de8e60ec7a719360d0a857a9");
        SKULL_ICONS.put("Deep Caverns", "skull:569a1f114151b4521373f34bc14c2963a5011cdc25a6554c48c708cd96ebfc");
        SKULL_ICONS.put("The Barn", "skull:4d3a6bd98ac1833c664c4909ff8d2dc62ce887bdcf3cc5b3848651ae5af6b");
        SKULL_ICONS.put("Mushroom Desert", "skull:6b20b23c1aa2be0270f016b4c90d6ee6b8330a17cfef87869d6ad60b2ffbf3b5");
        SKULL_ICONS.put("Dungeon Hub", "skull:9b56895b9659896ad647f58599238af532d46db9c1b0389b8bbeb70999dab33d");
        SKULL_ICONS.put("Dwarven Mines", "skull:51539dddf9ed255ece6348193cd75012c82c93aec381f05572cecf7379711b3b");
        SKULL_ICONS.put("HOTM Heart Of The Mountain", "skull:86f06eaa3004aeed09b3d5b45d976de584e691c0e9cade133635de93d23b9edb");
        SKULL_ICONS.put("Bazaar Dude", "skull:c232e3820897429157619b0ee099fec0628f602fff12b695de54aef11d923ad7");
        SKULL_ICONS.put("Museum", "skull:438cf3f8e54afc3b3f91d20a49f324dca1486007fe545399055524c17941f4dc");
        SKULL_ICONS.put("Crystal Hollows", "skull:21dbe30b027acbceb612563bd877cd7ebb719ea6ed1399027dcee58bb9049d4a");
        SKULL_ICONS.put("Dwarven Forge", "skull:5cbd9f5ec1ed007259996491e69ff649a3106cf920227b1bb3a71ee7a89863f");
        SKULL_ICONS.put("Forgotton Skull", "skull:6becc645f129c8bc2faa4d8145481fab11ad2ee75749d628dcd999aa94e7");
        SKULL_ICONS.put("Crystal Nucleus", "skull:34d42f9c461cee1997b67bf3610c6411bf852b9e5db607bbf626527cfb42912c");
        SKULL_ICONS.put("Void Sepulture", "skull:eb07594e2df273921a77c101d0bfdfa1115abed5b9b2029eb496ceba9bdbb4b3");
        SKULL_ICONS.put("Crimson Isle", "skull:c3687e25c632bce8aa61e0d64c24e694c3eea629ea944f4cf30dcfb4fbce071");
        SKULL_ICONS.put("Trapper Den", "skull:6102f82148461ced1f7b62e326eb2db3a94a33cba81d4281452af4d8aeca4991");
        SKULL_ICONS.put("Arachne Sanctuary", "skull:35e248da2e108f09813a6b848a0fcef111300978180eda41d3d1a7a8e4dba3c3");
        SKULL_ICONS.put("Garden", "skull:f4880d2c1e7b86e87522e20882656f45bafd42f94932b2c5e0d6ecaa490cb4c");
        SKULL_ICONS.put("Winter", "skull:6dd663136cafa11806fdbca6b596afd85166b4ec02142c8d5ac8941d89ab7");
        SKULL_ICONS.put("Wizard Tower", "skull:838564e28aba98301dbda5fafd86d1da4e2eaeef12ea94dcf440b883e559311c");
        SKULL_ICONS.put("Dwarven Mines Base Camp", "skull:2461ec3bd654f62ca9a393a32629e21b4e497c877d3f3380bcf2db0e20fc0244");
    }

    private enum FilterMode {
        ALL(Items.BOOK),
        ITEMS(Items.DIAMOND_SWORD),
        BLOCKS(Items.BEDROCK),
        SKULLS(Items.SKELETON_SKULL),
        MISC(Items.BUCKET);

        final ItemStack icon;
        FilterMode(Item item) { this.icon = new ItemStack(item); }
    }
    private FilterMode currentMode = FilterMode.ALL;

    private interface IconResult {
        void render(GuiGraphicsExtractor context, int x, int y);
        String getDisplayName();
        String getConfigId();
    }

    private record ItemStackResult(ItemStack stack) implements IconResult {
        @Override
        public void render(GuiGraphicsExtractor context, int x, int y) {
            XenDrawContextCompat.item(context, stack, x, y);
        }
        @Override
        public String getDisplayName() { return stack.getHoverName().getString(); }
        @Override
        public String getConfigId() {
            if (stack.getItem() == net.minecraft.world.item.Items.PLAYER_HEAD) {
                String name = stack.getHoverName().getString();
                if (SKULL_ICONS.containsKey(name)) return SKULL_ICONS.get(name);
            }
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        }
    }

    /** A skull typed straight into the search rather than picked from the known list. */
    private record CustomSkullResult(String configId, ItemStack stack) implements IconResult {
        @Override
        public void render(GuiGraphicsExtractor context, int x, int y) {
            XenDrawContextCompat.item(context, stack, x, y);
        }
        @Override
        public String getDisplayName() { return "Custom skull"; }
        @Override
        public String getConfigId() { return configId; }
    }

    private record TextureResult(String name, Identifier textureId) implements IconResult {
        @Override
        public void render(GuiGraphicsExtractor context, int x, int y) {
            XenDrawContextCompat.drawTexture(context, textureId, x, y, 0, 0, 16, 16, 16, 16);
        }
        @Override
        public String getDisplayName() { return name; }
        @Override
        public String getConfigId() { return textureId.toString(); }
    }

    private record HypixelResult(HypixelItemManager.HypixelItem item) implements IconResult {
        @Override
        public void render(GuiGraphicsExtractor context, int x, int y) {
            XenDrawContextCompat.item(context, item.iconStack(), x, y);
        }
        @Override
        public String getDisplayName() { return item.name(); }
        @Override
        public String getConfigId() { return item.configId(); }
    }

    private final Screen parent;
    private final int xSize = 176;
    private final int ySize = 166;
    private int guiLeft;
    private int guiTop;

    private int dragOffsetX, dragOffsetY;
    private boolean isDragging = false;
    private boolean isEditorOpen = false;
    private boolean isInfoPanelOpen = false;

    private boolean isSavePanelOpen = false;
    private EditBox saveProfileField;


    // Editor panel layout. Every offset is relative to the panel origin so the drawing code
    // and the hit testing below can never drift apart.
    private static final int PANEL_W = 190;
    private static final int PANEL_H = 288;
    private static final int PANEL_PAD = 8;
    private static final int PANEL_HEADER_H = 22;
    private static final int PANEL_INNER_W = PANEL_W - (PANEL_PAD * 2);
    private static final int FIELD_H = 17;

    private static final int CMD_LABEL_Y = 28;
    private static final int CMD_FIELD_Y = 38;
    private static final int BG_LABEL_Y = 61;
    private static final int BG_ROW_Y = 71;
    private static final int SWATCH_TILE = 28;
    private static final int SWATCH_ART = 18;
    private static final int SWATCH_GAP = 8;
    private static final int ICON_LABEL_Y = 105;
    private static final int CHIPS_Y = 115;
    private static final int CHIP_H = 21;
    private static final int SEARCH_Y = 140;
    private static final int MIN_PANEL_H = 214;
    private static final int DELETE_W = 54;

    private static final int GRID_COLS = 7;
    private static final int GRID_CELL = 24;
    private static final int GRID_W = GRID_COLS * GRID_CELL;

    private static final int IO_BTN_W = 108;
    private static final int IO_BTN_H = 18;
    private static final int IO_GAP = 4;
    private static final int IO_X = 10;
    private static final int IO_Y = 10;

    private static final int SAVE_PANEL_W = 208;
    private static final int SAVE_PANEL_H = 104;
    private static final int INFO_PANEL_W = 168;
    private static final int INFO_PANEL_H = 196;

    private int editorLeft;
    private int editorTop;

    private EditBox commandTextField;
    private EditBox iconTextField;

    private XenAddons.CustomButtonData editingButton = null;

    private final List<IconResult> searchedIcons = new ArrayList<>();
    private final LerpingInteger itemScroll = new LerpingInteger(0, 100);

    private String actionStatusText = "";
    private long actionStatusEndTime = 0;

    // Local gridSnap state
    private boolean localGridSnap;

    public GuiInvButtonEditor(Screen parent) {
        super(Component.literal("NEU Button Editor Port"));
        this.parent = parent;
        // Initialize local state from global default
        this.localGridSnap = XenAddons.instance.gridSnap;
    }

    protected void init() {
        this.guiLeft = (this.width - xSize) / 2;
        this.guiTop = (this.height - ySize) / 2;

        // Text fields are drawn unbordered inside our own frames, so they only own the text area.
        int fieldWidth = PANEL_INNER_W - 12;
        int panelX = (this.width - SAVE_PANEL_W) / 2;
        int panelY = (this.height - SAVE_PANEL_H) / 2;

        this.commandTextField = new EditBox(this.font, 0, 0, fieldWidth, 10, Component.literal("Command"));
        this.commandTextField.setMaxLength(256);
        this.commandTextField.setResponder(s -> {
            if (editingButton != null) {
                String text = s;
                if (text.isEmpty()) {
                    text = "/";
                    commandTextField.setValue(text);
                    commandTextField.moveCursorTo(1, false);
                } else if (!text.startsWith("/")) {
                    text = "/" + text.replace("/", "");
                    commandTextField.setValue(text);
                    commandTextField.moveCursorTo(text.length(), false);
                }
                editingButton.command = text;
            }
        });

        this.iconTextField = new EditBox(this.font, 0, 0, fieldWidth, 10, Component.literal("Icon ID"));
        this.iconTextField.setMaxLength(256);
        this.iconTextField.setResponder(this::search);

        this.saveProfileField = new EditBox(this.font, panelX + 22, panelY + 47, SAVE_PANEL_W - 44, 10, Component.literal("Profile Name"));
        this.saveProfileField.setMaxLength(32);

        styleField(this.commandTextField, "/command");
        styleField(this.iconTextField, SEARCH_HINT);
        styleField(this.saveProfileField, "Profile name");

        search("");
    }

    private void styleField(EditBox field, String hint) {
        field.setBordered(false);
        field.setTextColor(InvButtonsUi.TEXT);
        field.setHint(Component.literal(hint));
    }

    private boolean isSkullMode() {
        return currentMode == FilterMode.SKULLS;
    }

    /** The panel shrinks rather than running off screen at large GUI scales. */
    private int panelHeight() {
        return Math.max(MIN_PANEL_H, Math.min(PANEL_H, height - 10));
    }

    private int footerY() {
        return panelHeight() - 25;
    }

    private int gridTop() {
        return Math.min(editorTop + SEARCH_Y + FIELD_H + 2, gridBottom() - GRID_CELL);
    }


    private int swatchX(int contentX, int index) {
        return contentX + (index * (SWATCH_TILE + SWATCH_GAP));
    }

    private int infoIconX(int contentX) {
        return contentX + font.width(ICON_SECTION_LABEL) + 5;
    }

    private int gridBottom() {
        return editorTop + footerY() - 8;
    }

    /** Whole rows only, so a half cut icon row never shows at the bottom of the grid. */
    private int gridHeight() {
        int available = gridBottom() - gridTop();
        return Math.max(GRID_CELL, (available / GRID_CELL) * GRID_CELL);
    }

    private int gridContentHeight() {
        return (int) Math.ceil(searchedIcons.size() / (double) GRID_COLS) * GRID_CELL;
    }

    private int chipWidth() {
        return (PANEL_INNER_W - ((FilterMode.values().length - 1) * 2)) / FilterMode.values().length;
    }

    private void updateEditorCoordinates() {
        if (editingButton == null) return;

        int btnX = guiLeft + editingButton.x;
        int btnY = guiTop + editingButton.y;
        if (editingButton.anchorRight) btnX += xSize;
        if (editingButton.anchorBottom) btnY += ySize;

        editorLeft = btnX + 25;
        editorTop = btnY - 40;

        if (editorLeft + PANEL_W > width) editorLeft = btnX - PANEL_W - 7;
        if (editorLeft < 5) editorLeft = 5;
        if (editorTop + panelHeight() > height) editorTop = height - panelHeight() - 5;
        if (editorTop < 5) editorTop = 5;

        placeField(commandTextField, CMD_FIELD_Y);
        placeField(iconTextField, SEARCH_Y);
    }

    /** Centres an unbordered field inside the frame drawn at the given panel offset. */
    private void placeField(EditBox field, int panelOffsetY) {
        field.setX(editorLeft + PANEL_PAD + 6);
        field.setY(editorTop + panelOffsetY + ((FIELD_H - 8) / 2));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        guiLeft = (this.width - xSize) / 2;
        guiTop = (this.height - ySize) / 2;

        XenDrawContextCompat.drawTexture(context, INVENTORY_TEXTURE, guiLeft, guiTop, 0.0f, 0.0f, xSize, ySize, 256, 256);

        for (XenAddons.CustomButtonData button : XenAddons.instance.buttons) {
            int x = guiLeft + button.x;
            int y = guiTop + button.y;
            if (button.anchorRight) x += xSize;
            if (button.anchorBottom) y += ySize;

            XenDrawContextCompat.drawTexture(context, BUTTONS_TEXTURE, x, y,
                    (float) (button.backgroundIndex * 18), 18.0F, 18, 18, TEXTURE_WIDTH, TEXTURE_HEIGHT);
            if (button == editingButton) {
                context.fill(x, y, x + 18, y + 18, InvButtonsUi.alpha(InvButtonsUi.accent(), 0x55));
                InvButtonsUi.selection(context, x, y, 18, 18);
            }

            context.guiRenderState.up();
            if (button.itemId != null && !button.itemId.isEmpty()) {
                Identifier customTex = XenAddons.findCustomTexture(button.itemId);
                if (customTex != null) {
                    XenDrawContextCompat.drawTexture(context, customTex, x + 1, y + 1, 0, 0, 16, 16, 16, 16);
                } else {
                    ItemStack stack = button.getItemStack();
                    if (!stack.isEmpty()) {
                        XenDrawContextCompat.item(context, stack, x + 1, y + 1);
                    }
                }
            } else {
                context.centeredText(font, "?", x + 9, y + 5, 0xFFFFFFFF);
            }
        }

        if (editingButton != null && isEditorOpen) {
            updateEditorCoordinates();
            context.pose().pushMatrix();
            renderEditorPanel(context, mouseX, mouseY, delta);
            context.pose().popMatrix();
        }

        if (editingButton == null) {
            renderHints(context);
        }

        renderSnapBadge(context);

        renderIOButtons(context, mouseX, mouseY);

        if (isInfoPanelOpen) {
            renderSkullInfoPanel(context, mouseX, mouseY);
        }

        if (isSavePanelOpen) {
            context.pose().pushMatrix();
            renderSaveProfilePanel(context, mouseX, mouseY, delta);
            context.pose().popMatrix();
        }
    }

    /** Hint card, stacked under the import/export column while nothing is selected. */
    private void renderHints(GuiGraphicsExtractor context) {
        String[] hints = {
                "Click a button to select or drag it",
                "Click it again to open its editor",
                "Right click empty space to add one",
                "Backspace deletes the selection"
        };

        int cardW = 0;
        for (String hint : hints) cardW = Math.max(cardW, font.width(hint));
        cardW += 24;

        int cardH = (hints.length * (font.lineHeight + 3)) + 13;
        int cardX = IO_X;
        int cardY = ioButtonY(2) + IO_BTN_H + 8;

        InvButtonsUi.panel(context, cardX, cardY, cardW, cardH);
        int y = cardY + 8;
        for (int i = 0; i < hints.length; i++) {
            InvButtonsUi.rect(context, cardX + 10, y + 2, 3, 3, 1, i == 0 ? InvButtonsUi.accent() : InvButtonsUi.TEXT_DIM);
            context.text(font, hints[i], cardX + 18, y, i == 0 ? InvButtonsUi.TEXT : InvButtonsUi.TEXT_MUTED, false);
            y += font.lineHeight + 3;
        }
    }

    private void renderSnapBadge(GuiGraphicsExtractor context) {
        String snapText = "Grid Snap  " + (this.localGridSnap ? "ON" : "OFF") + "  [S]";
        int badgeW = font.width(snapText) + 18;
        int badgeH = 16;
        int badgeX = 10;
        int badgeY = height - badgeH - 10;

        InvButtonsUi.outline(context, badgeX, badgeY, badgeW, badgeH, InvButtonsUi.RADIUS - 1,
                this.localGridSnap ? InvButtonsUi.accent() : InvButtonsUi.BORDER_SOFT, InvButtonsUi.PANEL);
        InvButtonsUi.rect(context, badgeX + 7, badgeY + (badgeH - 5) / 2, 5, 5, 2,
                this.localGridSnap ? InvButtonsUi.accent() : InvButtonsUi.TEXT_DIM);
        context.text(font, snapText, badgeX + 16, badgeY + (badgeH - font.lineHeight) / 2 + 1,
                this.localGridSnap ? InvButtonsUi.TEXT : InvButtonsUi.TEXT_MUTED, false);
    }

    private int ioButtonY(int index) {
        return IO_Y + (index * (IO_BTN_H + IO_GAP));
    }

    private void renderIOButtons(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        String[] labels = {"Save as Profile", "Export", "Import"};
        for (int i = 0; i < labels.length; i++) {
            int y = ioButtonY(i);
            boolean hover = InvButtonsUi.hovered(mouseX, mouseY, IO_X, y, IO_BTN_W, IO_BTN_H);
            InvButtonsUi.button(context, font, IO_X, y, IO_BTN_W, IO_BTN_H, labels[i], hover,
                    i == 0 ? InvButtonsUi.Tone.ACCENT : InvButtonsUi.Tone.NEUTRAL);
        }

        if (System.currentTimeMillis() < actionStatusEndTime) {
            context.text(font, actionStatusText, IO_X + IO_BTN_W + 8, IO_Y + 5, InvButtonsUi.SUCCESS, false);
        }
    }

    private void renderSaveProfilePanel(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, InvButtonsUi.SCRIM);

        int panelX = (this.width - SAVE_PANEL_W) / 2;
        int panelY = (this.height - SAVE_PANEL_H) / 2;

        InvButtonsUi.panel(context, panelX, panelY, SAVE_PANEL_W, SAVE_PANEL_H);
        InvButtonsUi.header(context, font, panelX, panelY, SAVE_PANEL_W, 22, "Save as Profile");

        int closeX = panelX + SAVE_PANEL_W - 18;
        int closeY = panelY + 6;
        boolean hoverClose = InvButtonsUi.hovered(mouseX, mouseY, closeX, closeY, 11, 11);
        InvButtonsUi.rect(context, closeX, closeY, 11, 11, 2, hoverClose ? InvButtonsUi.DANGER : InvButtonsUi.RAISED);
        context.centeredText(font, "x", closeX + 6, closeY + 2, InvButtonsUi.TEXT);

        InvButtonsUi.label(context, font, "Profile name", panelX + 16, panelY + 32);
        InvButtonsUi.field(context, panelX + 16, panelY + 43, SAVE_PANEL_W - 32, 18, saveProfileField.isFocused());
        saveProfileField.setX(panelX + 22);
        saveProfileField.setY(panelY + 43 + ((18 - 8) / 2));
        saveProfileField.extractWidgetRenderState(context, mouseX, mouseY, delta);

        int btnW = 76;
        int btnX = panelX + (SAVE_PANEL_W - btnW) / 2;
        int btnY = panelY + SAVE_PANEL_H - 28;
        boolean hoverConfirm = InvButtonsUi.hovered(mouseX, mouseY, btnX, btnY, btnW, 18);
        InvButtonsUi.button(context, font, btnX, btnY, btnW, 18, "Save", hoverConfirm, InvButtonsUi.Tone.ACCENT);
    }

    private void handleExport() {
        try {
            Gson gson = new Gson();
            String json = gson.toJson(XenAddons.instance.buttons);
            String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            if (this.minecraft != null) {
                this.minecraft.keyboardHandler.setClipboard(encoded);
                actionStatusText = "Exported to Clipboard!";
                actionStatusEndTime = System.currentTimeMillis() + 3000;
            }
        } catch (Exception e) {
            actionStatusText = "Export Failed!";
            actionStatusEndTime = System.currentTimeMillis() + 3000;
            e.printStackTrace();
        }
    }

    private void handleImport() {
        try {
            if (this.minecraft != null) {
                String clipboard = this.minecraft.keyboardHandler.getClipboard();
                if (clipboard == null || clipboard.isEmpty()) return;

                String json = new String(Base64.getDecoder().decode(clipboard), StandardCharsets.UTF_8);
                Gson gson = new Gson();
                List<XenAddons.CustomButtonData> loaded = gson.fromJson(json, new TypeToken<List<XenAddons.CustomButtonData>>(){}.getType());

                if (loaded != null) {
                    XenAddons.instance.buttons = loaded;
                    XenAddons.save();
                    actionStatusText = "Imported Successfully!";
                    actionStatusEndTime = System.currentTimeMillis() + 3000;
                }
            }
        } catch (Exception e) {
            actionStatusText = "Invalid Clipboard!";
            actionStatusEndTime = System.currentTimeMillis() + 3000;
            e.printStackTrace();
        }
    }

    private void renderEditorPanel(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int contentX = editorLeft + PANEL_PAD;

        InvButtonsUi.panel(context, editorLeft, editorTop, PANEL_W, panelHeight());
        InvButtonsUi.header(context, font, editorLeft, editorTop, PANEL_W, PANEL_HEADER_H, "Edit Button");

        int closeX = editorLeft + PANEL_W - 18;
        int closeY = editorTop + 6;
        boolean hoverClose = InvButtonsUi.hovered(mouseX, mouseY, closeX, closeY, 11, 11);
        InvButtonsUi.rect(context, closeX, closeY, 11, 11, 2, hoverClose ? InvButtonsUi.DANGER : InvButtonsUi.RAISED);
        context.centeredText(font, "x", closeX + 6, closeY + 2, InvButtonsUi.TEXT);

        // --- Command ---
        InvButtonsUi.label(context, font, "Command", contentX, editorTop + CMD_LABEL_Y);
        InvButtonsUi.field(context, contentX, editorTop + CMD_FIELD_Y, PANEL_INNER_W, FIELD_H, commandTextField.isFocused());
        commandTextField.extractWidgetRenderState(context, mouseX, mouseY, delta);

        // --- Background swatches ---
        InvButtonsUi.label(context, font, "Background", contentX, editorTop + BG_LABEL_Y);
        int artOffset = (SWATCH_TILE - SWATCH_ART) / 2;
        for (int i = 0; i < 5; i++) {
            int bx = swatchX(contentX, i);
            int by = editorTop + BG_ROW_Y;

            boolean selected = editingButton.backgroundIndex == i;
            boolean hover = InvButtonsUi.hovered(mouseX, mouseY, bx, by, SWATCH_TILE, SWATCH_TILE);
            InvButtonsUi.outline(context, bx, by, SWATCH_TILE, SWATCH_TILE, 3,
                    selected ? InvButtonsUi.accent() : (hover ? InvButtonsUi.BORDER : InvButtonsUi.BORDER_SOFT),
                    hover ? InvButtonsUi.RAISED_HOVER : InvButtonsUi.SUNKEN);

            XenDrawContextCompat.drawTexture(context, BUTTONS_TEXTURE, bx + artOffset, by + artOffset,
                    (float) (i * 18), 0.0F, SWATCH_ART, SWATCH_ART, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }

        // --- Icon picker ---
        InvButtonsUi.label(context, font, ICON_SECTION_LABEL, contentX, editorTop + ICON_LABEL_Y);

        if (isSkullMode()) {
            int infoIconX = infoIconX(contentX);
            int infoIconY = editorTop + ICON_LABEL_Y - 1;
            if (InvButtonsUi.hovered(mouseX, mouseY, infoIconX, infoIconY, 10, 10)) {
                InvButtonsUi.rect(context, infoIconX - 1, infoIconY - 1, 12, 12, 2, InvButtonsUi.RAISED_HOVER);
            }
            XenDrawContextCompat.drawTexture(context, INFO_ICON_TEXTURE, infoIconX, infoIconY, 0.0f, 0.0f, 10, 10, 10, 10);
        }

        int chipW = chipWidth();
        for (int i = 0; i < FilterMode.values().length; i++) {
            FilterMode mode = FilterMode.values()[i];
            int bx = contentX + (i * (chipW + 2));
            int by = editorTop + CHIPS_Y;

            boolean active = currentMode == mode;
            boolean hover = InvButtonsUi.hovered(mouseX, mouseY, bx, by, chipW, CHIP_H);
            int fill = active ? InvButtonsUi.alpha(InvButtonsUi.accent(), 0xCC) : (hover ? InvButtonsUi.RAISED_HOVER : InvButtonsUi.RAISED);
            int border = active ? InvButtonsUi.accent() : InvButtonsUi.BORDER_SOFT;

            InvButtonsUi.outline(context, bx, by, chipW, CHIP_H, 2, border, fill);
            XenDrawContextCompat.item(context, mode.icon, bx + (chipW - 16) / 2, by + (CHIP_H - 16) / 2);
        }

        InvButtonsUi.field(context, contentX, editorTop + SEARCH_Y, PANEL_INNER_W, FIELD_H, iconTextField.isFocused());
        iconTextField.extractWidgetRenderState(context, mouseX, mouseY, delta);

        renderIconList(context, mouseX, mouseY, gridTop(), gridHeight());

        // --- Footer ---
        InvButtonsUi.divider(context, contentX, editorTop + footerY() - 6, PANEL_INNER_W);

        boolean hoverDelete = InvButtonsUi.hovered(mouseX, mouseY, contentX, editorTop + footerY(), DELETE_W, FIELD_H);
        InvButtonsUi.button(context, font, contentX, editorTop + footerY(), DELETE_W, FIELD_H, "Delete", hoverDelete, InvButtonsUi.Tone.DANGER);

        int doneX = contentX + DELETE_W + 6;
        int doneW = PANEL_INNER_W - DELETE_W - 6;
        boolean hoverDone = InvButtonsUi.hovered(mouseX, mouseY, doneX, editorTop + footerY(), doneW, FIELD_H);
        InvButtonsUi.button(context, font, doneX, editorTop + footerY(), doneW, FIELD_H, "Done", hoverDone, InvButtonsUi.Tone.ACCENT);
    }

    private void renderSkullInfoPanel(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int panelX = IO_X;
        int panelY = (this.height - INFO_PANEL_H) / 2;

        InvButtonsUi.panel(context, panelX, panelY, INFO_PANEL_W, INFO_PANEL_H);
        InvButtonsUi.header(context, font, panelX, panelY, INFO_PANEL_W, 22, "Skull IDs");

        int closeX = panelX + INFO_PANEL_W - 18;
        int closeY = panelY + 6;
        boolean hoverClose = InvButtonsUi.hovered(mouseX, mouseY, closeX, closeY, 11, 11);
        InvButtonsUi.rect(context, closeX, closeY, 11, 11, 2, hoverClose ? InvButtonsUi.DANGER : InvButtonsUi.RAISED);
        context.centeredText(font, "x", closeX + 6, closeY + 2, InvButtonsUi.TEXT);

        String[] steps = {
                "Open minecraft-heads.com and pick a head.",
                "Find the 'Texture URL' section.",
                "Copy the long string at the end of the URL.",
                "Paste it into the icon search above."
        };

        int textX = panelX + 12;
        int textY = panelY + 32;
        for (int i = 0; i < steps.length; i++) {
            InvButtonsUi.rect(context, textX, textY, 11, 11, 2, InvButtonsUi.alpha(InvButtonsUi.accent(), 0xAA));
            context.centeredText(font, String.valueOf(i + 1), textX + 6, textY + 2, InvButtonsUi.TEXT);

            List<FormattedCharSequence> lines = font.split(Component.literal(steps[i]), INFO_PANEL_W - 40);
            int lineY = textY + 1;
            for (FormattedCharSequence line : lines) {
                context.text(font, line, textX + 17, lineY, InvButtonsUi.TEXT_MUTED, false);
                lineY += font.lineHeight + 1;
            }
            textY = Math.max(lineY, textY + 13) + 6;
        }

        InvButtonsUi.divider(context, textX, textY, INFO_PANEL_W - 24);
        context.text(font, "Example", textX, textY + 8, InvButtonsUi.TEXT_DIM, false);
        context.text(font, "skull:a6cc4...", textX, textY + 20, InvButtonsUi.accent(), false);
    }

    private void renderIconList(GuiGraphicsExtractor context, int mouseX, int mouseY, int listY, int listH) {
        int listX = editorLeft + PANEL_PAD;

        InvButtonsUi.rect(context, listX, listY, GRID_W, listH, InvButtonsUi.RADIUS - 1, InvButtonsUi.SUNKEN);

        if (searchedIcons.isEmpty()) {
            String empty = "No match";
            context.text(font, empty, listX + (GRID_W - font.width(empty)) / 2, listY + (listH - font.lineHeight) / 2, InvButtonsUi.TEXT_DIM, false);
            return;
        }

        itemScroll.tick();
        context.enableScissor(listX, listY, listX + GRID_W, listY + listH);

        int scroll = itemScroll.getValue();
        int startIndex = (scroll / GRID_CELL) * GRID_COLS;
        int visibleCells = ((listH / GRID_CELL) + 2) * GRID_COLS;
        int endIndex = Math.min(startIndex + visibleCells, searchedIcons.size());

        String currentId = editingButton == null ? null : editingButton.itemId;
        IconResult hoveredResult = null;

        for (int i = startIndex; i < endIndex; i++) {
            IconResult result = searchedIcons.get(i);
            int col = (i - startIndex) % GRID_COLS;
            int row = (i - startIndex) / GRID_COLS;

            int cellX = listX + (col * GRID_CELL);
            int cellY = listY + (row * GRID_CELL) - (scroll % GRID_CELL);

            boolean insideList = mouseY >= listY && mouseY < listY + listH;
            boolean hover = insideList && InvButtonsUi.hovered(mouseX, mouseY, cellX, cellY, GRID_CELL, GRID_CELL);
            if (hover) hoveredResult = result;

            // One rounded fill per cell rather than a full outline: this runs for every visible
            // cell each frame, and an outline would cost three times the draw calls.
            int tile = GRID_CELL - 2;
            InvButtonsUi.rect(context, cellX + 1, cellY + 1, tile, tile, 2,
                    hover ? InvButtonsUi.RAISED_HOVER : InvButtonsUi.RAISED);
            if (currentId != null && currentId.equals(result.getConfigId())) {
                InvButtonsUi.selection(context, cellX + 1, cellY + 1, tile, tile);
            }

            result.render(context, cellX + ((GRID_CELL - 16) / 2), cellY + ((GRID_CELL - 16) / 2));
        }

        context.disableScissor();

        InvButtonsUi.scrollbar(context, listX + GRID_W + 2, listY + 2, listH - 4, gridContentHeight(), listH, scroll);

        if (hoveredResult != null) {
            context.setTooltipForNextFrame(font, Component.literal(hoveredResult.getDisplayName()), mouseX, mouseY);
        }
    }

    private void search(String query) {
        searchedIcons.clear();
        String lower = query.toLowerCase().trim();

        // Hardcoded Skulls
        if (currentMode == FilterMode.ALL || currentMode == FilterMode.SKULLS) {
            for (Map.Entry<String, String> entry : SKULL_ICONS.entrySet()) {
                if (entry.getKey().toLowerCase().contains(lower) || entry.getValue().toLowerCase().contains(lower)) {
                    ItemStack skullStack = XenAddons.CustomButtonData.getSkullStack(entry.getValue());
                    skullStack.set(DataComponents.CUSTOM_NAME, Component.literal(entry.getKey()));
                    searchedIcons.add(new ItemStackResult(skullStack));
                }
            }
            // NEW: Hypixel Skulls
            synchronized (HypixelItemManager.SKULL_ITEMS) {
                for (HypixelItemManager.HypixelItem hItem : HypixelItemManager.SKULL_ITEMS) {
                    if (hItem.name().toLowerCase().contains(lower) || hItem.configId().toLowerCase().contains(lower)) {
                        searchedIcons.add(new HypixelResult(hItem));
                    }
                }
            }
        }

        // Custom Textures
        if (currentMode == FilterMode.ALL || currentMode == FilterMode.MISC) {
            for (Map.Entry<String, Identifier> entry : XenAddons.CUSTOM_TEXTURES.entrySet()) {
                if (entry.getKey().toLowerCase().contains(lower)) {
                    searchedIcons.add(new TextureResult(entry.getKey(), entry.getValue()));
                }
            }
        }

        // Vanilla Items
        if (currentMode == FilterMode.ALL || currentMode == FilterMode.ITEMS || currentMode == FilterMode.BLOCKS) {
            for (Item item : BuiltInRegistries.ITEM) {
                boolean isBlock = item instanceof BlockItem;
                if (currentMode == FilterMode.BLOCKS && !isBlock) continue;
                if (currentMode == FilterMode.ITEMS && isBlock) continue;

                Identifier id = BuiltInRegistries.ITEM.getKey(item);
                if (id.toString().contains(lower) || new ItemStack(item).getHoverName().getString().toLowerCase().contains(lower)) {
                    searchedIcons.add(new ItemStackResult(new ItemStack(item)));
                    if (searchedIcons.size() > 500 && !lower.isEmpty()) break;
                }
            }
        }

        searchedIcons.sort(
            Comparator.comparing((IconResult r) -> {
                    String name = r.getDisplayName().toLowerCase();
                    return name.startsWith(lower) ? 0 : 1;
                })
                .thenComparingInt(r -> {
                    return (r instanceof HypixelResult) ? 1 : 0;
                })
                .thenComparing(r -> r.getDisplayName().toLowerCase())
        );

        // A pasted texture id always wins the top slot, sorting can't push it out of view.
        IconResult pasted = buildPastedSkull(query);
        if (pasted != null && searchedIcons.stream().noneMatch(r -> r.getConfigId().equals(pasted.getConfigId()))) {
            searchedIcons.add(0, pasted);
        }

        // Results changed, so the previous offset no longer points anywhere meaningful.
        itemScroll.setTarget(0);
    }

    /**
     * Turns a texture id typed into the search into a usable icon, so a skull that is not in
     * the built in list can still be assigned without a dedicated input field.
     */
    private IconResult buildPastedSkull(String query) {
        if (currentMode != FilterMode.ALL && currentMode != FilterMode.SKULLS) return null;

        String trimmed = query == null ? "" : query.trim();
        String texture = trimmed.toLowerCase(Locale.ROOT).startsWith("skull:")
                ? trimmed.substring("skull:".length()).trim()
                : trimmed;
        if (!SKULL_TEXTURE_PATTERN.matcher(texture).matches()) return null;

        String configId = "skull:" + texture;
        try {
            ItemStack stack = XenAddons.CustomButtonData.getSkullStack(configId);
            if (stack == null || stack.isEmpty()) return null;
            return new CustomSkullResult(configId, stack);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (isSavePanelOpen) {
            int panelX = (this.width - SAVE_PANEL_W) / 2;
            int panelY = (this.height - SAVE_PANEL_H) / 2;

            if (InvButtonsUi.hovered(mouseX, mouseY, panelX + SAVE_PANEL_W - 18, panelY + 6, 11, 11)) {
                isSavePanelOpen = false;
                saveProfileField.setFocused(false);
                return true;
            }

            boolean inField = InvButtonsUi.hovered(mouseX, mouseY, panelX + 16, panelY + 43, SAVE_PANEL_W - 32, 18);
            saveProfileField.setFocused(inField);
            if (inField) {
                saveProfileField.mouseClicked(click, doubled);
                return true;
            }

            int btnW = 76;
            int btnX = panelX + (SAVE_PANEL_W - btnW) / 2;
            int btnY = panelY + SAVE_PANEL_H - 28;
            if (InvButtonsUi.hovered(mouseX, mouseY, btnX, btnY, btnW, 18)) {
                confirmSaveProfile();
            }
            return true;
        }

        if (InvButtonsUi.hovered(mouseX, mouseY, IO_X, ioButtonY(0), IO_BTN_W, IO_BTN_H)) {
            isSavePanelOpen = true;
            saveProfileField.setFocused(true);
            return true;
        }

        if (InvButtonsUi.hovered(mouseX, mouseY, IO_X, ioButtonY(1), IO_BTN_W, IO_BTN_H)) {
            handleExport();
            return true;
        }

        if (InvButtonsUi.hovered(mouseX, mouseY, IO_X, ioButtonY(2), IO_BTN_W, IO_BTN_H)) {
            handleImport();
            return true;
        }

        if (isInfoPanelOpen) {
            int panelX = IO_X;
            int panelY = (this.height - INFO_PANEL_H) / 2;

            if (InvButtonsUi.hovered(mouseX, mouseY, panelX + INFO_PANEL_W - 18, panelY + 6, 11, 11)) {
                isInfoPanelOpen = false;
                return true;
            }
            if (InvButtonsUi.hovered(mouseX, mouseY, panelX, panelY, INFO_PANEL_W, INFO_PANEL_H)) {
                return true;
            }
        }

        if (editingButton != null && isEditorOpen) {
            updateEditorCoordinates();

            if (InvButtonsUi.hovered(mouseX, mouseY, editorLeft, editorTop, PANEL_W, panelHeight())) {
                int contentX = editorLeft + PANEL_PAD;

                if (InvButtonsUi.hovered(mouseX, mouseY, editorLeft + PANEL_W - 18, editorTop + 6, 11, 11)) {
                    closeEditor();
                    return true;
                }

                if (isSkullMode()
                        && InvButtonsUi.hovered(mouseX, mouseY, infoIconX(contentX), editorTop + ICON_LABEL_Y - 1, 10, 10)) {
                    isInfoPanelOpen = !isInfoPanelOpen;
                    return true;
                }

                boolean inCmd = InvButtonsUi.hovered(mouseX, mouseY, contentX, editorTop + CMD_FIELD_Y, PANEL_INNER_W, FIELD_H);
                boolean inIcon = InvButtonsUi.hovered(mouseX, mouseY, contentX, editorTop + SEARCH_Y, PANEL_INNER_W, FIELD_H);

                commandTextField.setFocused(inCmd);
                iconTextField.setFocused(inIcon);

                if (inCmd) commandTextField.mouseClicked(click, doubled);
                if (inIcon) iconTextField.mouseClicked(click, doubled);

                for (int i = 0; i < 5; i++) {
                    if (InvButtonsUi.hovered(mouseX, mouseY, swatchX(contentX, i), editorTop + BG_ROW_Y, SWATCH_TILE, SWATCH_TILE)) {
                        editingButton.backgroundIndex = i;
                        return true;
                    }
                }

                int chipW = chipWidth();
                for (int i = 0; i < FilterMode.values().length; i++) {
                    int bx = contentX + (i * (chipW + 2));
                    if (InvButtonsUi.hovered(mouseX, mouseY, bx, editorTop + CHIPS_Y, chipW, CHIP_H)) {
                        switchMode(FilterMode.values()[i]);
                        return true;
                    }
                }

                if (InvButtonsUi.hovered(mouseX, mouseY, contentX, editorTop + footerY(), DELETE_W, FIELD_H)) {
                    deleteEditingButton();
                    return true;
                }

                int doneX = contentX + DELETE_W + 6;
                if (InvButtonsUi.hovered(mouseX, mouseY, doneX, editorTop + footerY(), PANEL_INNER_W - DELETE_W - 6, FIELD_H)) {
                    closeEditor();
                    return true;
                }

                if (InvButtonsUi.hovered(mouseX, mouseY, contentX, gridTop(), GRID_W, gridHeight())) {
                    handleListClick(mouseX, mouseY);
                }
                return true;
            }
        }

        for (XenAddons.CustomButtonData btn : XenAddons.instance.buttons) {
            int x = guiLeft + btn.x;
            int y = guiTop + btn.y;
            if (btn.anchorRight) x += xSize;
            if (btn.anchorBottom) y += ySize;

            if (mouseX >= x && mouseX <= x + 18 && mouseY >= y && mouseY <= y + 18) {
                if (editingButton != btn) {
                    editingButton = btn;
                    isEditorOpen = false;
                    commandTextField.setFocused(false);
                    iconTextField.setFocused(false);
                    isInfoPanelOpen = false;
                } else {
                    if (!isEditorOpen) {
                        isEditorOpen = true;
                        if (!btn.command.startsWith("/")) {
                            btn.command = "/" + btn.command;
                        }
                        commandTextField.setValue(btn.command);
                        commandTextField.moveCursorTo(btn.command.length(), false);
                        commandTextField.setFocused(true);
                        iconTextField.setFocused(false);
                        currentMode = FilterMode.ALL;
                        search(iconTextField.getValue());
                    }
                }
                isDragging = true;
                dragOffsetX = (int)mouseX - x;
                dragOffsetY = (int)mouseY - y;
                return true;
            }
        }

        if (button == 1) {
            int relX = (int)mouseX - guiLeft;
            int relY = (int)mouseY - guiTop;
            XenAddons.CustomButtonData newBtn = new XenAddons.CustomButtonData(relX, relY, "/cmd", "minecraft:stone");
            XenAddons.instance.buttons.add(newBtn);
            editingButton = newBtn;
            isEditorOpen = true;
            commandTextField.setValue(newBtn.command);
            commandTextField.setFocused(true);
            isDragging = true;
            dragOffsetX = 9;
            dragOffsetY = 9;
            currentMode = FilterMode.ALL;
            search("");
            isInfoPanelOpen = false;
            return true;
        }

        editingButton = null;
        isDragging = false;
        isEditorOpen = false;
        isInfoPanelOpen = false;
        return super.mouseClicked(click, doubled);
    }

    private void handleListClick(double mouseX, double mouseY) {
        int listX = editorLeft + PANEL_PAD;
        int scroll = itemScroll.getValue();
        int startIndex = (scroll / GRID_CELL) * GRID_COLS;

        int col = (int) (mouseX - listX) / GRID_CELL;
        int row = ((int) mouseY - gridTop() + (scroll % GRID_CELL)) / GRID_CELL;

        if (col < 0 || col >= GRID_COLS || row < 0) return;

        int index = startIndex + (row * GRID_COLS) + col;
        if (index < 0 || index >= searchedIcons.size()) return;

        editingButton.itemId = searchedIcons.get(index).getConfigId();
    }

    private void switchMode(FilterMode mode) {
        if (currentMode == mode) return;
        currentMode = mode;
        search(iconTextField.getValue());
    }

    private void deleteEditingButton() {
        if (editingButton == null) return;
        XenAddons.instance.buttons.remove(editingButton);
        editingButton = null;
        isDragging = false;
        isInfoPanelOpen = false;
        closeEditor();
    }

    private void confirmSaveProfile() {
        String name = saveProfileField.getValue().trim();
        if (name.isEmpty()) return;

        XenAddons.saveProfile(name);
        actionStatusText = "Saved: " + name;
        actionStatusEndTime = System.currentTimeMillis() + 3000;
        isSavePanelOpen = false;
        saveProfileField.setValue("");
        saveProfileField.setFocused(false);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        isDragging = false;
        return super.mouseReleased(click);
    }

    // --- Helper to check overlap ---
    private boolean isOverlapping(int x, int y) {
        for (XenAddons.CustomButtonData btn : XenAddons.instance.buttons) {
            if (btn == editingButton) continue; // Don't check against self

            // Simple distance check (since they are fixed size)
            // If the top-left corners are within BUTTON_SIZE of each other, they overlap.
            if (Math.abs(btn.x - x) < BUTTON_SIZE && Math.abs(btn.y - y) < BUTTON_SIZE) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (isDragging && editingButton != null) {
            int newScreenX = (int)mouseX - dragOffsetX;
            int newScreenY = (int)mouseY - dragOffsetY;

            int relativeX = newScreenX - guiLeft;
            int relativeY = newScreenY - guiTop;

            if (editingButton.anchorRight) relativeX -= xSize;
            if (editingButton.anchorBottom) relativeY -= ySize;

            int proposedX = editingButton.x; // Default to current if overlap prevents move
            int proposedY = editingButton.y;

            if (this.localGridSnap) {
                boolean isOutsideX = (relativeX < 0) || (relativeX > xSize - BUTTON_SIZE);
                boolean isOutsideY = (relativeY < 0) || (relativeY > ySize - BUTTON_SIZE);

                if (!isOutsideX && !isOutsideY) {
                    // INSIDE GUI ZONE
                    if (relativeY < 80) {
                        Point bestMatch = null;
                        double closestDistSq = Double.MAX_VALUE;
                        for (Point p : INVENTORY_FIXED_SLOTS) {
                            double distSq = Math.pow(relativeX - p.x, 2) + Math.pow(relativeY - p.y, 2);
                            if (distSq < closestDistSq) {
                                closestDistSq = distSq;
                                bestMatch = p;
                            }
                        }
                        if (bestMatch != null) {
                            proposedX = bestMatch.x;
                            proposedY = bestMatch.y;
                        }
                    } else {
                        Point bestMatch = null;
                        double closestDistSq = Double.MAX_VALUE;
                        for (Point p : INVENTORY_FIXED_SLOTS) {
                            double distSq = Math.pow(relativeX - p.x, 2) + Math.pow(relativeY - p.y, 2);
                            if (distSq < closestDistSq) {
                                closestDistSq = distSq;
                                bestMatch = p;
                            }
                        }
                        if (bestMatch != null) {
                            proposedX = bestMatch.x;
                            proposedY = bestMatch.y;
                        }
                    }
                } else {
                    // OUTSIDE ZONE
                    if (relativeX < 0) {
                        int col = (relativeX + OUTER_PADDING) / OUTER_GRID_SIZE;
                        proposedX = -OUTER_PADDING - BUTTON_SIZE + (col * OUTER_GRID_SIZE);
                    } else if (relativeX > xSize - BUTTON_SIZE) {
                        int col = (relativeX - xSize + OUTER_PADDING) / OUTER_GRID_SIZE;
                        proposedX = xSize + OUTER_PADDING + (col * OUTER_GRID_SIZE);
                    } else {
                        if (isOutsideY) {
                            int col = Math.round((float)(relativeX - TOP_BOTTOM_START_X) / OUTER_GRID_SIZE);
                            proposedX = TOP_BOTTOM_START_X + (col * OUTER_GRID_SIZE);
                        }
                    }

                    if (relativeY < 0) {
                        int row = (relativeY + OUTER_PADDING) / OUTER_GRID_SIZE;
                        proposedY = -OUTER_PADDING - BUTTON_SIZE + (row * OUTER_GRID_SIZE);
                    } else if (relativeY > ySize - BUTTON_SIZE) {
                        int row = (relativeY - ySize + OUTER_PADDING) / OUTER_GRID_SIZE;
                        proposedY = ySize + OUTER_PADDING + (row * OUTER_GRID_SIZE);
                    } else {
                        if (isOutsideX) {
                            int row = Math.round((float)(relativeY - OUTER_PADDING) / OUTER_GRID_SIZE);
                            proposedY = OUTER_PADDING + (row * OUTER_GRID_SIZE);
                        }
                    }
                }
            } else {
                // Free movement
                proposedX = relativeX;
                proposedY = relativeY;

                // --- Prevent dragging into forbidden zones (Main Inventory & Left Column) ---

                // Main Inventory (Bottom)
                // Approx Bounds: x=[0, 176], y=[83, 166] (stops at GUI bottom edge)
                boolean insideBottomZone = (relativeX + BUTTON_SIZE > 0 && relativeX < 176) &&
                        (relativeY + BUTTON_SIZE > 83 && relativeY < 166);

                // Left Column (Armor/Offhand)
                // Approx Bounds: x=[0, 26], y=[7, 83]
                boolean insideLeftZone = (relativeX + BUTTON_SIZE > 0 && relativeX < 26) &&
                        (relativeY + BUTTON_SIZE > 7 && relativeY < 83);

                if (insideBottomZone || insideLeftZone) {
                    // Snap to nearest valid restricted slot
                    Point bestMatch = null;
                    double closestDistSq = Double.MAX_VALUE;
                    for (Point p : INVENTORY_FIXED_SLOTS) {
                        double distSq = Math.pow(relativeX - p.x, 2) + Math.pow(relativeY - p.y, 2);
                        if (distSq < closestDistSq) {
                            closestDistSq = distSq;
                            bestMatch = p;
                        }
                    }
                    if (bestMatch != null) {
                        proposedX = bestMatch.x;
                        proposedY = bestMatch.y;
                    }
                }
            }

            // Only update if no overlap
            if (!isOverlapping(proposedX, proposedY)) {
                editingButton.x = proposedX;
                editingButton.y = proposedY;
                updateEditorCoordinates();
            }

            return true;
        }

        if (editingButton != null && isEditorOpen && InvButtonsUi.hovered(mouseX, mouseY, editorLeft, editorTop, PANEL_W, panelHeight())) {
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (editingButton != null && isEditorOpen && InvButtonsUi.hovered(mouseX, mouseY, editorLeft, editorTop, PANEL_W, panelHeight())) {
            int maxScroll = Math.max(0, gridContentHeight() - gridHeight());
            int target = itemScroll.getTarget() + (int) (-verticalAmount * 11);
            itemScroll.setTarget(Math.max(0, Math.min(maxScroll, target)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int keyCode = input.key();
        int scanCode = input.scancode();
        int modifiers = input.modifiers();

        // Toggle Grid Snap
        boolean inputsFocused = isEditorOpen && (commandTextField.isFocused() || iconTextField.isFocused());
        if (keyCode == InputConstants.KEY_S && !inputsFocused) {
            this.localGridSnap = !this.localGridSnap;
            return true;
        }

        if (isSavePanelOpen) {
            if (keyCode == InputConstants.KEY_ESCAPE) {
                isSavePanelOpen = false;
                saveProfileField.setFocused(false);
                return true;
            }
            if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
                confirmSaveProfile();
                return true;
            }
            if (saveProfileField.keyPressed(input)) return true;
            return super.keyPressed(input);
        }

        if (editingButton != null) {
            if (isEditorOpen) {
                if (commandTextField.isFocused()) {
                    if (keyCode == InputConstants.KEY_BACKSPACE) {
                        String txt = commandTextField.getValue();
                        int cursor = commandTextField.getCursorPosition();
                        if (txt.equals("/") || (cursor <= 1 && commandTextField.getHighlighted().isEmpty())) {
                            return true;
                        }
                    }
                    return commandTextField.keyPressed(input);
                }
                if (iconTextField.isFocused()) return iconTextField.keyPressed(input);
            }
            if (keyCode == InputConstants.KEY_DELETE || keyCode == InputConstants.KEY_BACKSPACE) {
                if (!isEditorOpen || (!commandTextField.isFocused() && !iconTextField.isFocused())) {
                    deleteEditingButton();
                    return true;
                }
            }
        }

        if (keyCode == InputConstants.KEY_ESCAPE && isEditorOpen) {
            closeEditor();
            return true;
        }

        return super.keyPressed(input);
    }

    private void closeEditor() {
        isEditorOpen = false;
        commandTextField.setFocused(false);
        iconTextField.setFocused(false);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (isSavePanelOpen) {
            if (saveProfileField.charTyped(input)) return true;
            return super.charTyped(input);
        }
        if (editingButton != null && isEditorOpen) {
            if (commandTextField.isFocused() && commandTextField.charTyped(input)) {
                return true;
            }
            if (iconTextField.isFocused() && iconTextField.charTyped(input)) {
                search(iconTextField.getValue());
                return true;
            }
        }
        return super.charTyped(input);
    }

    @Override
    public void onClose() {
        XenAddons.save();
        if (parent != null) minecraft.setScreen(parent);
        else super.onClose();
    }

    private static class LerpingInteger {
        private int value, target;
        public LerpingInteger(int value, int target) { this.value = value; this.target = target; }
        public void tick() {
            if (value != target) {
                int diff = target - value;
                int change = diff / 5;
                if (change == 0) change = diff > 0 ? 1 : -1;
                value += change;
            }
        }
        public int getValue() { return value; }
        public int getTarget() { return target; }
        public void setTarget(int t) { this.target = t; }
    }
}
