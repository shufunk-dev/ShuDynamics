package net.enchantedwood.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;

import java.util.List;

@Environment(EnvType.CLIENT)
public class EnchantedStorageTerminalScreen extends HandledScreen<EnchantedStorageTerminalScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/enchanted_chest_gui.png");
    private TextFieldWidget searchBox;

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

        this.searchBox = new TextFieldWidget(this.textRenderer, x + 90, y + 4, 78, 11, Text.literal("Search..."));
        this.searchBox.setMaxLength(25);
        this.searchBox.setDrawsBackground(true);
        this.searchBox.setFocusUnlocked(true);
        this.addSelectableChild(this.searchBox);
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
            context.drawText(this.textRenderer, Text.literal("●").formatted(net.minecraft.util.Formatting.RED), 76, 6, 0xFF5555, false);
        } else if (totalCap <= 0) {
            context.drawText(this.textRenderer, Text.literal("●").formatted(net.minecraft.util.Formatting.GOLD), 76, 6, 0xFFAA00, false);
        } else {
            context.drawText(this.textRenderer, Text.literal("●").formatted(net.minecraft.util.Formatting.GREEN), 76, 6, 0x55FF55, false);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.searchBox.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Hover Tooltip for Network Capacity & Online status (x + 70 .. 88, y + 4 .. 16)
        if (mouseX >= x + 70 && mouseX <= x + 88 && mouseY >= y + 4 && mouseY <= y + 16) {
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
                        Text.literal("§eItems Stored: §f" + String.format("%,d / %,d", stored, totalCap)),
                        Text.literal("§aStatus: ONLINE")
                ), mouseX, mouseY);
            }
        }
    }
}
