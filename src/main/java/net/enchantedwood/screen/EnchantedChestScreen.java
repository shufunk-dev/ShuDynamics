package net.enchantedwood.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.enchantedwood.EnchantedWoodMod;

@Environment(EnvType.CLIENT)
public class EnchantedChestScreen extends AbstractContainerScreen<EnchantedChestScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/enchanted_chest_gui.png");
    private Button upButton;
    private Button downButton;
    private Button sortButton;

    public EnchantedChestScreen(EnchantedChestScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 194, 222);
        this.titleLabelX = 8;
        this.titleLabelY = 5;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Up Scroll Button
        this.upButton = Button.builder(Component.literal("▲"), button -> {
            int newRow = Math.max(0, this.menu.getScrollRow() - 1);
            this.menu.setScrollRow(newRow);
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 100 + newRow);
            }
        }).bounds(x + 174, y + 4, 12, 11).build();
        this.addRenderableWidget(this.upButton);

        // Down Scroll Button
        this.downButton = Button.builder(Component.literal("▼"), button -> {
            int newRow = Math.min(this.menu.getMaxScrollRows(), this.menu.getScrollRow() + 1);
            this.menu.setScrollRow(newRow);
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 100 + newRow);
            }
        }).bounds(x + 174, y + 126, 12, 11).build();
        this.addRenderableWidget(this.downButton);

        // Sort Button
        this.sortButton = Button.builder(Component.literal("Sort"), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 2);
            }
        }).bounds(x + 138, y + 3, 32, 11).build();
        this.addRenderableWidget(this.sortButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScrollRows = this.menu.getMaxScrollRows();
        if (maxScrollRows > 0) {
            int currentScroll = this.menu.getScrollRow();
            int newScroll = currentScroll;
            if (verticalAmount < 0) {
                newScroll = Math.min(currentScroll + 1, maxScrollRows);
            } else if (verticalAmount > 0) {
                newScroll = Math.max(currentScroll - 1, 0);
            }
            if (newScroll != currentScroll) {
                this.menu.setScrollRow(newScroll);
                if (this.minecraft != null && this.minecraft.gameMode != null) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 100 + newScroll);
                }
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        context.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        int maxSlots = this.menu.getMaxSlots();
        int scrollRow = this.menu.getScrollRow() + 1;
        int totalRows = (int) Math.ceil((double) maxSlots / 9.0);

        String capacityInfo = maxSlots + " Slots (Row " + scrollRow + "/" + totalRows + ")";
        context.text(this.font, capacityInfo, 7, 128, 0x404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // Render Scrollbar Thumb Widget
        int maxScrollRows = this.menu.getMaxScrollRows();
        int thumbY = y + 17;
        if (maxScrollRows > 0) {
            float progress = (float) this.menu.getScrollRow() / (float) maxScrollRows;
            thumbY += (int) (progress * (108 - 15));
        }

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 174, thumbY, 196.0f, 0.0f, 12, 15, 256, 256);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);

        if (this.upButton != null && this.downButton != null) {
            boolean canScroll = this.menu.getMaxScrollRows() > 0;
            this.upButton.visible = canScroll;
            this.downButton.visible = canScroll;
            this.upButton.active = this.menu.getScrollRow() > 0;
            this.downButton.active = this.menu.getScrollRow() < this.menu.getMaxScrollRows();
        }
    }
}
