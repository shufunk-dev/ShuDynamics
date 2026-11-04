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
public class FuelRefineryScreen extends AbstractContainerScreen<FuelRefineryScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/fuel_refinery_gui.png");

    public FuelRefineryScreen(FuelRefineryScreenHandler handler, Inventory inventory, Component title) {
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

        // 1. Energy Bar (x + 13, y + 15, width 12, height 36) using UV at u=176, v=0
        int energyHeight = this.menu.getScaledEnergy();
        if (energyHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 13, y + 51 - energyHeight, 176.0f, 36.0f - energyHeight, 12, energyHeight, 256, 256);
        }

        // 2. Refining Progress Arrow (x + 74, y + 34, width 24, height 17) using UV at u=176, v=36
        int progressWidth = this.menu.getScaledProgress();
        if (progressWidth > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 74, y + 34, 176.0f, 36.0f, progressWidth, 17, 256, 256);

            // Active petrochemical distillation bubble sparks
            if (this.menu.isRefining()) {
                long time = System.currentTimeMillis();
                int sparkX = x + 74 + progressWidth - 1;
                int sparkY = y + 42 + (int) ((time / 90) % 3) - 1;
                int sparkColor = ((time / 70) % 2 == 0) ? 0xFF00E5FF : 0xFFFFD700;
                context.fill(sparkX, sparkY, sparkX + 2, sparkY + 2, sparkColor);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFD700, false);
        context.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF373A40, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Energy Tooltip
        if (mouseX >= x + 12 && mouseX <= x + 26 && mouseY >= y + 14 && mouseY <= y + 52) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§e⚡ Energy Storage"),
                    Component.literal(String.format("§f%,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy())),
                    Component.literal("§7Consumes 20 FE/t while refining")
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 5) {
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e📥 Feedstock Input"),
                        Component.literal("§7Insert biological or petroleum base:"),
                        Component.literal("§f• Crude Oil Sludge, Corn, Wheat, Sugar Cane, Potato")
                ), mouseX, mouseY);
                case 1 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§7🛢️ Empty Canister Input"),
                        Component.literal("§7Insert Empty Gas Canisters to bottle refined fuels.")
                ), mouseX, mouseY);
                case 2 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§a✨ Refined Fuel Output"),
                        Component.literal("§7Gasoline Canisters or Biofuel appear here.")
                ), mouseX, mouseY);
                case 3 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6🛢️ Mineral Tar Byproduct"),
                        Component.literal("§7Recovered petroleum tar appears here.")
                ), mouseX, mouseY);
                case 4 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e⚡ Battery Charge Socket"),
                        Component.literal("§7Insert an electric battery to recharge this refinery off-grid.")
                ), mouseX, mouseY);
            }
        }
    }
}
