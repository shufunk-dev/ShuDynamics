package net.enchantedwood.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.enchantedwood.EnchantedWoodMod;

import java.util.List;

@Environment(EnvType.CLIENT)
public class CokeOvenScreen extends AbstractContainerScreen<CokeOvenScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/coke_oven_gui.png");

    public CokeOvenScreen(CokeOvenScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.titleLabelX = 8;
        this.titleLabelY = 7;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // 1. Pyrolysis Retort Temperature / Heat Gauge at (20, 20, width 14, height 50) using UV at u=208, v=0
        if (this.menu.isCooking()) {
            int heatHeight = this.menu.getScaledCookProgress(50);
            if (heatHeight > 0) {
                context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 20, y + 70 - heatHeight, 208.0f, 50.0f - heatHeight, 14, heatHeight, 256, 256);
            }
        }

        // 2. Cook Progress Arrow at (79, 34, width 24, height 17) using UV at u=176, v=14
        int cookWidth = this.menu.getScaledCookProgress(24);
        if (cookWidth > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 79, y + 34, 176.0f, 14.0f, cookWidth, 17, 256, 256);

            // Active pyrolysis ember flecks at the pouring tip
            if (this.menu.isCooking()) {
                long time = System.currentTimeMillis();
                int sparkX = x + 79 + cookWidth - 1;
                int sparkY = y + 42 + (int) ((time / 120) % 3) - 1;
                int sparkColor = ((time / 90) % 2 == 0) ? 0xFFFFD700 : 0xFFFF5500;
                context.fill(sparkX, sparkY, sparkX + 2, sparkY + 2, sparkColor);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFE8E0D0, false);
        context.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF373A40, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Retort Heat Gauge Tooltip
        if (mouseX >= x + 19 && mouseX <= x + 35 && mouseY >= y + 19 && mouseY <= y + 71) {
            boolean active = this.menu.isCooking();
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§6♨ Pyrolysis Retort Kiln"),
                    Component.literal(active ? "§aStatus: §e650°C (Active Pyrolysis)" : "§7Status: §8Standby (Idle)"),
                    Component.literal("§7Bakes coal/logs in an oxygen-deprived"),
                    Component.literal("§7chamber to yield high-density Coke Coal & Tar.")
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 3) {
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e📥 Raw Carbon Input"),
                        Component.literal("§7Insert Coal, Charcoal, or Wood Logs:"),
                        Component.literal("§8Bakes carbon in oxygen-free pyrolysis.")
                ), mouseX, mouseY);
                case 1 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§8🔥 Coke Coal Output"),
                        Component.literal("§7High-efficiency industrial blast fuel appears here.")
                ), mouseX, mouseY);
                case 2 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6🛢️ Mineral Tar Byproduct"),
                        Component.literal("§7Recovered petroleum tar from coal pyrolysis.")
                ), mouseX, mouseY);
            }
        }
    }
}
