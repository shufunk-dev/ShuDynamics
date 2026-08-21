package net.enchantedwood.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;

@Environment(EnvType.CLIENT)
public class EnchantedChestScreen extends HandledScreen<EnchantedChestScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/enchanted_chest_gui.png");
    private ButtonWidget upButton;
    private ButtonWidget downButton;
    private ButtonWidget sortButton;

    public EnchantedChestScreen(EnchantedChestScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 194;
        this.backgroundHeight = 222;
        this.titleX = 8;
        this.titleY = 5;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Up Scroll Button
        this.upButton = ButtonWidget.builder(Text.literal("▲"), button -> {
            int newRow = Math.max(0, this.handler.getScrollRow() - 1);
            this.handler.setScrollRow(newRow);
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 100 + newRow);
            }
        }).dimensions(x + 174, y + 4, 12, 11).build();
        this.addDrawableChild(this.upButton);

        // Down Scroll Button
        this.downButton = ButtonWidget.builder(Text.literal("▼"), button -> {
            int newRow = Math.min(this.handler.getMaxScrollRows(), this.handler.getScrollRow() + 1);
            this.handler.setScrollRow(newRow);
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 100 + newRow);
            }
        }).dimensions(x + 174, y + 126, 12, 11).build();
        this.addDrawableChild(this.downButton);

        // Sort Button
        this.sortButton = ButtonWidget.builder(Text.literal("Sort"), button -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 2);
            }
        }).dimensions(x + 138, y + 3, 32, 11).build();
        this.addDrawableChild(this.sortButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScrollRows = this.handler.getMaxScrollRows();
        if (maxScrollRows > 0) {
            int currentScroll = this.handler.getScrollRow();
            int newScroll = currentScroll;
            if (verticalAmount < 0) {
                newScroll = Math.min(currentScroll + 1, maxScrollRows);
            } else if (verticalAmount > 0) {
                newScroll = Math.max(currentScroll - 1, 0);
            }
            if (newScroll != currentScroll) {
                this.handler.setScrollRow(newScroll);
                if (this.client != null && this.client.interactionManager != null) {
                    this.client.interactionManager.clickButton(this.handler.syncId, 100 + newScroll);
                }
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0x404040, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0x404040, false);

        int maxSlots = this.handler.getMaxSlots();
        int scrollRow = this.handler.getScrollRow() + 1;
        int totalRows = (int) Math.ceil((double) maxSlots / 9.0);

        String capacityInfo = maxSlots + " Slots (Row " + scrollRow + "/" + totalRows + ")";
        context.drawText(this.textRenderer, capacityInfo, 7, 128, 0x404040, false);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // Render Scrollbar Thumb Widget
        int maxScrollRows = this.handler.getMaxScrollRows();
        int thumbY = y + 17;
        if (maxScrollRows > 0) {
            float progress = (float) this.handler.getScrollRow() / (float) maxScrollRows;
            thumbY += (int) (progress * (108 - 15));
        }

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 174, thumbY, 196.0f, 0.0f, 12, 15, 256, 256);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.upButton != null && this.downButton != null) {
            boolean canScroll = this.handler.getMaxScrollRows() > 0;
            this.upButton.visible = canScroll;
            this.downButton.visible = canScroll;
            this.upButton.active = this.handler.getScrollRow() > 0;
            this.downButton.active = this.handler.getScrollRow() < this.handler.getMaxScrollRows();
        }

        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
