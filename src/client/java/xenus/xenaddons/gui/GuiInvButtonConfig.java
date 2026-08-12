package xenus.xenaddons.gui;

import xenus.xenaddons.XenAddons;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import xenus.xenaddons.compat.XenDrawContextCompat;
import xenus.xenaddons.compat.XenSoundCompat;

import java.util.ArrayList;
import java.util.List;

public class GuiInvButtonConfig extends Screen {

    private static final Identifier TRASH_ICON = Identifier.fromNamespaceAndPath("xenaddons", "textures/gui/icons/delete_trashcan.png");

    private static final int PANEL_W = 272;
    private static final int PANEL_H = 196;
    private static final int PAD = 12;
    private static final int HEADER_H = 24;

    private static final int TABS_Y = 34;
    private static final int TABS_H = 20;
    private static final int CONTENT_Y = 64;

    private static final int ROW_H = 24;

    private static final int FIELD_Y = 76;
    private static final int FIELD_H = 20;
    private static final int SAVE_W = 66;
    private static final int LIST_Y = 116;
    private static final int LIST_H = 62;
    private static final int PROFILE_ROW_H = 20;
    private static final int PROFILE_ROW_GAP = 2;
    private static final int TRASH_W = 20;

    private final Screen parent;
    private Tab currentTab = Tab.GENERAL;

    private int boxX, boxY;

    private final List<Toggle> toggles = new ArrayList<>();
    private final List<String> profiles = new ArrayList<>();
    private EditBox profileNameField;

    private final LerpingInteger profileScroll = new LerpingInteger(0, 0);

    private String statusText = "";
    private int statusColor = InvButtonsUi.SUCCESS;
    private long statusEndTime = 0;

    public GuiInvButtonConfig(Screen parent) {
        super(Component.literal("Configuration"));
        this.parent = parent;
    }

    private enum Tab {
        GENERAL("General"),
        PROFILES("Profiles");

        final String label;
        Tab(String label) { this.label = label; }
    }

    protected void init() {
        boxX = (this.width - PANEL_W) / 2;
        boxY = (this.height - PANEL_H) / 2;

        toggles.clear();
        toggles.add(new Toggle("Inventory Buttons", "Master switch for every button",
                () -> XenAddons.instance.enabled, v -> XenAddons.instance.enabled = v));
        toggles.add(new Toggle("Show Tooltips", "Reveal the command on hover",
                () -> XenAddons.instance.showTooltips, v -> XenAddons.instance.showTooltips = v));
        toggles.add(new Toggle("Hide in Creative", "Keep the creative tabs clear",
                () -> XenAddons.instance.hideInCreative, v -> XenAddons.instance.hideInCreative = v));
        toggles.add(new Toggle("Grid Snap Default", "Start the editor with snapping on",
                () -> XenAddons.instance.gridSnap, v -> XenAddons.instance.gridSnap = v));
        toggles.add(new Toggle("Hide Recipe Book", "Remove the vanilla recipe toggle",
                () -> XenAddons.instance.hideRecipeBook, v -> XenAddons.instance.hideRecipeBook = v));

        int fieldW = PANEL_W - (PAD * 2) - SAVE_W - 6;
        this.profileNameField = new EditBox(this.font, boxX + PAD + 7, boxY + FIELD_Y + (FIELD_H - 8) / 2, fieldW - 14, 10, Component.literal("Profile Name"));
        this.profileNameField.setMaxLength(32);
        this.profileNameField.setBordered(false);
        this.profileNameField.setTextColor(InvButtonsUi.TEXT);
        this.profileNameField.setHint(Component.literal("New profile name"));

        refreshProfileList();
    }

    private void refreshProfileList() {
        profiles.clear();
        profiles.addAll(XenAddons.getProfileNames());
        clampScroll();
    }

    private void setStatus(String msg, int color) {
        this.statusText = msg;
        this.statusColor = color;
        this.statusEndTime = System.currentTimeMillis() + 3000;
    }

    // --- Layout helpers, shared by rendering and hit testing ---

    private int tabX(int index) {
        return boxX + PAD + (index * ((PANEL_W - (PAD * 2)) / Tab.values().length));
    }

    private int tabWidth() {
        return (PANEL_W - (PAD * 2)) / Tab.values().length;
    }

    private int rowY(int index) {
        return boxY + CONTENT_Y + (index * ROW_H);
    }

    private int contentWidth() {
        return PANEL_W - (PAD * 2);
    }

    private int fieldWidth() {
        return contentWidth() - SAVE_W - 6;
    }

    private int saveX() {
        return boxX + PAD + fieldWidth() + 6;
    }

    private int profileContentHeight() {
        return profiles.size() * (PROFILE_ROW_H + PROFILE_ROW_GAP);
    }

    private void clampScroll() {
        int max = Math.max(0, profileContentHeight() - LIST_H);
        if (profileScroll.getTarget() > max) profileScroll.setTarget(max);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        context.fill(0, 0, this.width, this.height, InvButtonsUi.SCRIM);

        InvButtonsUi.panel(context, boxX, boxY, PANEL_W, PANEL_H);
        InvButtonsUi.header(context, font, boxX, boxY, PANEL_W, HEADER_H, "Inventory Buttons");

        String hint = "ESC to close";
        context.text(font, hint, boxX + PANEL_W - 10 - font.width(hint), boxY + (HEADER_H - font.lineHeight) / 2 + 1, InvButtonsUi.TEXT_DIM, false);

        renderTabs(context, mouseX, mouseY);

        if (currentTab == Tab.GENERAL) {
            renderGeneral(context, mouseX, mouseY);
            return;
        }

        renderProfiles(context, mouseX, mouseY, delta);
        if (System.currentTimeMillis() < statusEndTime) {
            context.text(font, statusText, boxX + PAD, boxY + PANEL_H - 14, statusColor, false);
        }
    }

    private void renderTabs(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int tabW = tabWidth();
        InvButtonsUi.rect(context, boxX + PAD, boxY + TABS_Y, contentWidth(), TABS_H, InvButtonsUi.RADIUS - 1, InvButtonsUi.SUNKEN);

        for (int i = 0; i < Tab.values().length; i++) {
            Tab tab = Tab.values()[i];
            int x = tabX(i);
            boolean selected = currentTab == tab;
            boolean hover = InvButtonsUi.hovered(mouseX, mouseY, x, boxY + TABS_Y, tabW, TABS_H);

            if (selected) {
                InvButtonsUi.rect(context, x + 2, boxY + TABS_Y + 2, tabW - 4, TABS_H - 4, InvButtonsUi.RADIUS - 2, InvButtonsUi.alpha(InvButtonsUi.accent(), 0xE0));
            } else if (hover) {
                InvButtonsUi.rect(context, x + 2, boxY + TABS_Y + 2, tabW - 4, TABS_H - 4, InvButtonsUi.RADIUS - 2, InvButtonsUi.RAISED);
            }

            int color = selected ? InvButtonsUi.TEXT : (hover ? InvButtonsUi.TEXT : InvButtonsUi.TEXT_MUTED);
            context.text(font, tab.label, x + (tabW - font.width(tab.label)) / 2, boxY + TABS_Y + (TABS_H - font.lineHeight) / 2 + 1, color, false);
        }
    }

    private void renderGeneral(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        for (int i = 0; i < toggles.size(); i++) {
            Toggle toggle = toggles.get(i);
            int y = rowY(i);
            boolean hover = InvButtonsUi.hovered(mouseX, mouseY, boxX + PAD, y, contentWidth(), ROW_H);

            if (hover) {
                InvButtonsUi.rect(context, boxX + PAD, y, contentWidth(), ROW_H, InvButtonsUi.RADIUS - 1, InvButtonsUi.alpha(0xFFFFFFFF, 0x0C));
            }

            context.text(font, toggle.label, boxX + PAD + 8, y + 4, InvButtonsUi.TEXT, false);
            context.text(font, toggle.description, boxX + PAD + 8, y + 14, InvButtonsUi.TEXT_DIM, false);

            int toggleX = boxX + PAD + contentWidth() - 8 - InvButtonsUi.TOGGLE_W;
            InvButtonsUi.toggle(context, toggleX, y + (ROW_H - InvButtonsUi.TOGGLE_H) / 2, toggle.getter.get(), hover);
        }
    }

    private void renderProfiles(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        InvButtonsUi.label(context, font, "Save current layout", boxX + PAD, boxY + CONTENT_Y);

        boolean focused = profileNameField.isFocused();
        InvButtonsUi.field(context, boxX + PAD, boxY + FIELD_Y, fieldWidth(), FIELD_H, focused);
        profileNameField.extractWidgetRenderState(context, mouseX, mouseY, delta);

        boolean hoverSave = InvButtonsUi.hovered(mouseX, mouseY, saveX(), boxY + FIELD_Y, SAVE_W, FIELD_H);
        InvButtonsUi.button(context, font, saveX(), boxY + FIELD_Y, SAVE_W, FIELD_H, "Save", hoverSave, InvButtonsUi.Tone.ACCENT);

        InvButtonsUi.label(context, font, "Saved profiles", boxX + PAD, boxY + LIST_Y - 12);

        int listX = boxX + PAD;
        int listY = boxY + LIST_Y;
        int listW = contentWidth();
        InvButtonsUi.rect(context, listX, listY, listW, LIST_H, InvButtonsUi.RADIUS - 1, InvButtonsUi.SUNKEN);

        if (profiles.isEmpty()) {
            String empty = "No profile saved yet";
            context.text(font, empty, listX + (listW - font.width(empty)) / 2, listY + (LIST_H - font.lineHeight) / 2, InvButtonsUi.TEXT_DIM, false);
            return;
        }

        profileScroll.tick();
        context.enableScissor(listX, listY, listX + listW, listY + LIST_H);

        int rowW = listW - 8 - TRASH_W - 4;
        for (int i = 0; i < profiles.size(); i++) {
            int rowY = profileRowY(i);
            if (rowY + PROFILE_ROW_H < listY || rowY > listY + LIST_H) continue;

            boolean inList = mouseY >= listY && mouseY < listY + LIST_H;
            boolean hoverRow = inList && InvButtonsUi.hovered(mouseX, mouseY, listX + 4, rowY, rowW, PROFILE_ROW_H);
            boolean hoverTrash = inList && InvButtonsUi.hovered(mouseX, mouseY, listX + 4 + rowW + 4, rowY, TRASH_W, PROFILE_ROW_H);

            InvButtonsUi.rect(context, listX + 4, rowY, rowW, PROFILE_ROW_H, InvButtonsUi.RADIUS - 2,
                    hoverRow ? InvButtonsUi.RAISED_HOVER : InvButtonsUi.RAISED);
            context.text(font, profiles.get(i), listX + 12, rowY + (PROFILE_ROW_H - font.lineHeight) / 2 + 1, InvButtonsUi.TEXT, false);

            if (hoverRow) {
                String load = "Load";
                context.text(font, load, listX + 4 + rowW - 8 - font.width(load), rowY + (PROFILE_ROW_H - font.lineHeight) / 2 + 1, InvButtonsUi.accent(), false);
            }

            InvButtonsUi.rect(context, listX + 4 + rowW + 4, rowY, TRASH_W, PROFILE_ROW_H, InvButtonsUi.RADIUS - 2,
                    hoverTrash ? InvButtonsUi.DANGER : InvButtonsUi.RAISED);
            XenDrawContextCompat.drawTexture(context, TRASH_ICON, listX + 4 + rowW + 6, rowY + 2, 0.0f, 0.0f, 16, 16, 16, 16);
        }

        context.disableScissor();

        InvButtonsUi.scrollbar(context, listX + listW - 5, listY + 2, LIST_H - 4, profileContentHeight(), LIST_H, profileScroll.getValue());
    }

    private int profileRowY(int index) {
        return boxY + LIST_Y + 4 + (index * (PROFILE_ROW_H + PROFILE_ROW_GAP)) - profileScroll.getValue();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (click.button() != 0) return super.mouseClicked(click, doubled);

        int tabW = tabWidth();
        for (int i = 0; i < Tab.values().length; i++) {
            if (InvButtonsUi.hovered(mouseX, mouseY, tabX(i), boxY + TABS_Y, tabW, TABS_H)) {
                currentTab = Tab.values()[i];
                profileNameField.setFocused(false);
                playClick();
                return true;
            }
        }

        if (currentTab == Tab.GENERAL) {
            for (int i = 0; i < toggles.size(); i++) {
                if (InvButtonsUi.hovered(mouseX, mouseY, boxX + PAD, rowY(i), contentWidth(), ROW_H)) {
                    Toggle toggle = toggles.get(i);
                    toggle.setter.accept(!toggle.getter.get());
                    playClick();
                    return true;
                }
            }
            return super.mouseClicked(click, doubled);
        }

        boolean inField = InvButtonsUi.hovered(mouseX, mouseY, boxX + PAD, boxY + FIELD_Y, fieldWidth(), FIELD_H);
        profileNameField.setFocused(inField);
        if (inField) {
            profileNameField.mouseClicked(click, doubled);
            return true;
        }

        if (InvButtonsUi.hovered(mouseX, mouseY, saveX(), boxY + FIELD_Y, SAVE_W, FIELD_H)) {
            saveCurrentProfile();
            playClick();
            return true;
        }

        int listX = boxX + PAD;
        int listY = boxY + LIST_Y;
        int rowW = contentWidth() - 8 - TRASH_W - 4;
        if (mouseY >= listY && mouseY < listY + LIST_H) {
            for (int i = 0; i < profiles.size(); i++) {
                int rowY = profileRowY(i);
                String name = profiles.get(i);

                if (InvButtonsUi.hovered(mouseX, mouseY, listX + 4, rowY, rowW, PROFILE_ROW_H)) {
                    XenAddons.loadProfile(name);
                    setStatus("Loaded " + name, InvButtonsUi.SUCCESS);
                    playClick();
                    return true;
                }

                if (InvButtonsUi.hovered(mouseX, mouseY, listX + 4 + rowW + 4, rowY, TRASH_W, PROFILE_ROW_H)) {
                    XenAddons.deleteProfile(name);
                    setStatus("Deleted " + name, InvButtonsUi.DANGER);
                    refreshProfileList();
                    playClick();
                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    private void saveCurrentProfile() {
        String name = profileNameField.getValue().trim();
        if (name.isEmpty()) {
            setStatus("Name the profile first", InvButtonsUi.DANGER);
            return;
        }

        XenAddons.saveProfile(name);
        setStatus("Saved " + name, InvButtonsUi.SUCCESS);
        refreshProfileList();
        profileNameField.setValue("");
    }

    private void playClick() {
        if (this.minecraft != null) XenSoundCompat.playButtonClick(this.minecraft);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (currentTab == Tab.PROFILES) {
            int max = Math.max(0, profileContentHeight() - LIST_H);
            int target = profileScroll.getTarget() + (int) (-verticalAmount * 10);
            profileScroll.setTarget(Math.max(0, Math.min(max, target)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int keyCode = input.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && !profileNameField.isFocused()) {
            this.onClose();
            return true;
        }
        if (currentTab == Tab.PROFILES && profileNameField.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                saveCurrentProfile();
                return true;
            }
            if (profileNameField.keyPressed(input)) return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (currentTab == Tab.PROFILES && profileNameField.isFocused()) {
            if (profileNameField.charTyped(input)) return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void onClose() {
        XenAddons.save();
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    interface BooleanSupplier { boolean get(); }
    interface BooleanConsumer { void accept(boolean val); }

    private record Toggle(String label, String description, BooleanSupplier getter, BooleanConsumer setter) {}

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
