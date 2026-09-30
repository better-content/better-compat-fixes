package com.bettercontent.bettercontentfixes.compat.epicfight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.ForgeRegistries;

/** Inventory sub-screen for one global basic-combo selection. */
public final class StyleScreen extends Screen {
    private static final int BG = 0xFFA48657;
    private static final int PANEL = 0xFFEFE3C4;
    private static final int RED = 0xFF496951;
    private static final int WHITE = 0xFF254637;
    private static final int MUTED = 0xFF58654B;
    private static final int ROW_HEIGHT = 23;
    private static final int VISIBLE_ROWS = 5;

    private EditBox search;
    private Button equip;
    private Button useDefault;
    private String focused = "";
    private int scroll;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int listWidth;

    public StyleScreen() {
        super(Component.translatable("screen.better_content_fixes.fighting_styles"));
    }

    @Override
    protected void init() {
        panelWidth = Math.min(390, width - 16);
        panelHeight = Math.min(244, height - 16);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        listWidth = Math.max(125, Math.min(176, (panelWidth - 26) / 2));
        if (focused.isEmpty()) focused = StyleClientState.snapshot().selected();

        search = new EditBox(font, left + 10, top + 30, listWidth - 4, 18,
                Component.translatable("screen.better_content_fixes.search"));
        search.setHint(Component.translatable("screen.better_content_fixes.search"));
        search.setBordered(false);
        search.setTextColor(0xFF254637);
        search.setTextColorUneditable(0xFF58654B);
        search.setMaxLength(60);
        search.setResponder(text -> scroll = 0);
        addRenderableWidget(search);

        equip = addRenderableWidget(Button.builder(Component.translatable("screen.better_content_fixes.equip"),
                button -> {
                    if (!focused.isEmpty()) StyleNetwork.select(focused);
                }).bounds(left + listWidth + 17, top + panelHeight - 31,
                panelWidth - listWidth - 29, 20).build());
        useDefault = addRenderableWidget(Button.builder(Component.translatable("screen.better_content_fixes.default"),
                button -> StyleNetwork.select(""))
                .bounds(left + 10, top + panelHeight - 31, listWidth - 4, 20).build());
        addRenderableWidget(Button.builder(Component.literal("×"), button -> onClose())
                .bounds(left + panelWidth - 27, top + 7, 17, 17).build());
        StyleNetwork.request();
    }

    void refresh() {
        if (!focused.isEmpty() && StyleClientState.snapshot().rows().stream()
                .noneMatch(row -> row.id().equals(focused))) focused = "";
    }

    private List<StyleNetwork.Row> filtered() {
        String query = search == null ? "" : search.getValue().toLowerCase(java.util.Locale.ROOT);
        var rows = new ArrayList<>(StyleClientState.snapshot().rows());
        rows.sort(Comparator.comparing((StyleNetwork.Row row) -> !row.learned())
                .thenComparing(StyleNetwork.Row::name));
        if (!query.isEmpty()) rows.removeIf(row -> !row.name().toLowerCase(java.util.Locale.ROOT).contains(query)
                && row.sources().stream().noneMatch(source -> source.toString().contains(query)));
        return rows;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x66364838);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, BG);
        graphics.fill(left + 3, top + 3, left + panelWidth - 3, top + panelHeight - 3, PANEL);
        graphics.fill(left + 8, top + 7, left + 11, top + panelHeight - 7, RED);
        graphics.drawString(font, title, left + 18, top + 12, WHITE, false);
        graphics.fill(left + 10, top + 30, left + listWidth + 6, top + 48, 0xFFF9EFD7);
        graphics.fill(left + 10, top + 30, left + listWidth + 6, top + 31, BG);
        graphics.drawString(font, Component.translatable("screen.better_content_fixes.progress",
                StyleClientState.snapshot().rows().stream().filter(StyleNetwork.Row::learned).count(),
                StyleClientState.snapshot().rows().size()), left + 10, top + 54, MUTED, false);

        var rows = filtered();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - VISIBLE_ROWS)));
        int listY = top + 69;
        int listX = left + 10;
        if (rows.size() > VISIBLE_ROWS) {
            String range = (scroll + 1) + "-" + Math.min(rows.size(), scroll + VISIBLE_ROWS)
                    + "/" + rows.size() + (scroll == 0 ? " ↓" : scroll + VISIBLE_ROWS >= rows.size() ? " ↑" : " ↕");
            graphics.drawString(font, range, listX + listWidth - 4 - font.width(range),
                    top + 54, MUTED, false);
        }
        if (rows.isEmpty()) graphics.drawString(font,
                Component.translatable("screen.better_content_fixes.no_matches"),
                listX + 7, listY + 6, MUTED, false);
        for (int index = scroll; index < Math.min(rows.size(), scroll + VISIBLE_ROWS); index++) {
            var row = rows.get(index);
            int y = listY + (index - scroll) * ROW_HEIGHT;
            boolean selected = row.id().equals(focused);
            graphics.fill(listX, y, listX + listWidth - 4, y + ROW_HEIGHT - 2,
                    selected ? 0xFFD9C79F : 0xFFF9EFD7);
            graphics.fill(listX, y, listX + 2, y + ROW_HEIGHT - 2, selected ? RED : BG);
            String label = row.learned() ? row.name() : "◇  Undiscovered";
            graphics.drawString(font, font.plainSubstrByWidth(label, listWidth - 17),
                    listX + 7, y + 6, row.learned() ? WHITE : MUTED, false);
        }
        int detailX = left + listWidth + 15;
        int detailW = panelWidth - listWidth - 26;
        graphics.fill(detailX, top + 30, detailX + detailW, top + panelHeight - 38, 0xFFF9EFD7);
        var selected = StyleClientState.snapshot().rows().stream()
                .filter(row -> row.id().equals(focused)).findFirst().orElse(null);
        useDefault.active = !StyleClientState.snapshot().selected().isEmpty();
        equip.active = selected != null && selected.learned()
                && !focused.equals(StyleClientState.snapshot().selected());
        if (selected == null) {
            graphics.drawString(font, Component.translatable("screen.better_content_fixes.choose"),
                    detailX + 8, top + 45, MUTED, false);
        } else if (!selected.learned()) {
            graphics.drawString(font, Component.translatable("screen.better_content_fixes.locked"),
                    detailX + 8, top + 45, MUTED, false);
        } else {
            graphics.drawString(font, font.plainSubstrByWidth(selected.name(), detailW - 16),
                    detailX + 8, top + 45, WHITE, false);
            if (focused.equals(StyleClientState.snapshot().selected())) graphics.drawString(font,
                    Component.translatable("screen.better_content_fixes.active"),
                    detailX + 8, top + 60, RED, false);
            graphics.drawString(font, Component.translatable("screen.better_content_fixes.learned_from"),
                    detailX + 8, top + 82, MUTED, false);
            int sourceY = top + 97;
            int sourceSlots = Math.max(1, (panelHeight - 150) / 13 + 1);
            for (int index = 0; index < Math.min(selected.sources().size(), sourceSlots); index++) {
                if (index == sourceSlots - 1 && selected.sources().size() > sourceSlots) {
                    graphics.drawString(font, Component.translatable("screen.better_content_fixes.more_weapons",
                            selected.sources().size() - index), detailX + 8, sourceY, MUTED, false);
                    break;
                }
                var source = selected.sources().get(index);
                var item = ForgeRegistries.ITEMS.getValue(source);
                if (item != null) graphics.drawString(font,
                        font.plainSubstrByWidth(item.getDescription().getString(), detailW - 16),
                        detailX + 8, sourceY, WHITE, false);
                sourceY += 13;
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        for (var child : children()) if (child instanceof Button button && button.visible) {
            int x = button.getX(), y = button.getY(), w = button.getWidth(), h = button.getHeight();
            graphics.fill(x, y, x + w, y + h, !button.active ? 0xFF849382
                    : button.isHoveredOrFocused() ? 0xFF59755C : 0xFF405D49);
            graphics.fill(x, y, x + w, y + 2, BG);
            graphics.drawCenteredString(font, button.getMessage(), x + w / 2, y + (h - font.lineHeight) / 2, 0xFFF9EFD7);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listX = left + 10;
        int listY = top + 69;
        if (button == 0 && mouseX >= listX && mouseX < listX + listWidth - 4
                && mouseY >= listY && mouseY < listY + VISIBLE_ROWS * ROW_HEIGHT) {
            int index = scroll + ((int) mouseY - listY) / ROW_HEIGHT;
            var rows = filtered();
            if (index >= 0 && index < rows.size()) {
                focused = rows.get(index).id();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = Math.max(0, Math.min(Math.max(0, filtered().size() - VISIBLE_ROWS),
                scroll - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public void onClose() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player != null) minecraft.setScreen(new InventoryScreen(minecraft.player));
        else minecraft.setScreen(null);
    }
}
