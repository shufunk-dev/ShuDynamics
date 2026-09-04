package net.enchantedwood.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public class EnchantedStorageTerminalScreen extends HandledScreen<EnchantedStorageTerminalScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/enchanted_chest_gui.png");
    private TextFieldWidget searchBox;
    private ButtonWidget prevButton;
    private ButtonWidget nextButton;

    public EnchantedStorageTerminalScreen(EnchantedStorageTerminalScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 222;
        this.titleX = 8;
        this.titleY = 6;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        this.searchBox = new TextFieldWidget(this.textRenderer, x + 98, y + 4, 70, 11, Text.literal("Search..."));
        this.searchBox.setMaxLength(30);
        this.searchBox.setDrawsBackground(true);
        this.searchBox.setFocusUnlocked(true);
        this.searchBox.setPlaceholder(Text.literal("Search...").formatted(net.minecraft.util.Formatting.DARK_GRAY));
        this.searchBox.setText(this.handler.getSearchQuery());
        this.searchBox.setChangedListener(query -> {
            this.handler.setSearchFilter(query);
            if (net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(net.enchantedwood.network.SetStorageTerminalSearchPayload.ID)) {
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new net.enchantedwood.network.SetStorageTerminalSearchPayload(query));
            }
        });
        this.addDrawableChild(this.searchBox);

        // Previous Page Button
        this.prevButton = ButtonWidget.builder(Text.literal("◀"), button -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 0);
            }
        }).dimensions(x + 48, y + 3, 14, 12).build();

        // Next Page Button
        this.nextButton = ButtonWidget.builder(Text.literal("▶"), button -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 1);
            }
        }).dimensions(x + 80, y + 3, 14, 12).build();

        this.addDrawableChild(this.prevButton);
        this.addDrawableChild(this.nextButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount > 0) {
            // Scroll Up -> Previous Page
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 0);
                return true;
            }
        } else if (verticalAmount < 0) {
            // Scroll Down -> Next Page
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 1);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);

        // Network telemetry in header
        int totalCap = this.handler.getTotalCapacity();
        boolean online = this.handler.isOnline();

        if (!online) {
            context.drawText(this.textRenderer, Text.literal("●").formatted(net.minecraft.util.Formatting.RED), 38, 6, 0xFF5555, false);
        } else if (totalCap <= 0) {
            context.drawText(this.textRenderer, Text.literal("●").formatted(net.minecraft.util.Formatting.GOLD), 38, 6, 0xFFAA00, false);
        } else {
            context.drawText(this.textRenderer, Text.literal("●").formatted(net.minecraft.util.Formatting.GREEN), 38, 6, 0x55FF55, false);
        }

        // Page Indicator between ◀ and ▶ buttons
        int curPage = this.handler.getCurrentPage() + 1;
        int totalPages = this.handler.getTotalPages();
        String pageStr = curPage + "/" + totalPages;
        int strWidth = this.textRenderer.getWidth(pageStr);
        context.drawText(this.textRenderer, Text.literal(pageStr).formatted(net.minecraft.util.Formatting.DARK_GRAY), 71 - (strWidth / 2), 6, 0x3F3F3F, false);
    }

    public static String formatCount(int count) {
        if (count <= 1) return "";
        if (count < 10000) return String.valueOf(count); // Shows exact count (e.g. 1408, 9999)
        if (count < 1000000) return (count / 1000) + "k";
        if (count < 10000000) return String.format(Locale.ROOT, "%.1fM", count / 1000000.0);
        return (count / 1000000) + "M";
    }

    @Override
    protected void drawSlot(DrawContext context, Slot slot, int mouseX, int mouseY) {
        if (slot.id < 54) {
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                int x = slot.x;
                int y = slot.y;
                context.drawItem(stack, x, y);
                String countText = formatCount(stack.getCount());
                context.drawStackOverlay(this.textRenderer, stack, x, y, countText);
                return;
            }
        }
        super.drawSlot(context, slot, mouseX, mouseY);
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext context, int x, int y) {
        if (this.focusedSlot != null && this.focusedSlot.hasStack() && this.focusedSlot.id < 54) {
            ItemStack stack = this.focusedSlot.getStack();
            List<Text> tooltip = new ArrayList<>(getTooltipFromItem(stack));
            tooltip.add(Text.literal("§6📦 Stored in Network: §e" + String.format(Locale.ROOT, "%,d", stack.getCount())));
            context.drawTooltip(this.textRenderer, tooltip, stack.getTooltipData(), x, y);
            return;
        }
        super.drawMouseoverTooltip(context, x, y);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Hover Tooltip for Network Status (x + 34 .. 48, y + 4 .. 16)
        if (mouseX >= x + 34 && mouseX <= x + 48 && mouseY >= y + 4 && mouseY <= y + 16) {
            int totalCap = this.handler.getTotalCapacity();
            int stored = this.handler.getStoredCount();
            boolean online = this.handler.isOnline();

            if (!online) {
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§c⚡ Storage Network: OFFLINE"),
                        Text.literal("§7Connect power to the Enchanted Storage Controller.")
                ), mouseX, mouseY);
            } else if (totalCap <= 0) {
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§6💾 Digital Storage Network"),
                        Text.literal("§eStatus: §6NO DRIVES INSTALLED"),
                        Text.literal("§7Install 1k, 4k, 16k, or 64k Storage Crystals"),
                        Text.literal("§7in a nearby Drive Bay to store items.")
                ), mouseX, mouseY);
            } else {
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§6💾 Digital Storage Network"),
                        Text.literal("§eItems Stored: §f" + String.format(Locale.ROOT, "%,d / %,d", stored, totalCap)),
                        Text.literal("§aStatus: ONLINE"),
                        Text.literal("§7Use Mouse Wheel or ◀ ▶ buttons to cycle pages.")
                ), mouseX, mouseY);
            }
        }
    }
}
