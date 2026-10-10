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
public class AluminumRefinerScreen extends AbstractContainerScreen<AluminumRefinerScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/aluminum_refiner_gui.png");

    public AluminumRefinerScreen(AluminumRefinerScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // 1. Oxygen Bar (at x + 24, y + 20, width 14, height 52)
        int o2Height = this.menu.getScaledOxygen(52);
        if (o2Height > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 24, y + 72 - o2Height, 176.0f, 52.0f - o2Height, 14, o2Height, 256, 256);
        }

        // 2. Cook Arrow Progress (at x + 79, y + 34, width 24, height 17)
        int cookWidth = this.menu.getScaledCookProgress(24);
        if (cookWidth > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 79, y + 34, 176.0f, 54.0f, cookWidth, 17, 256, 256);
        }

        // 3. Energy Bar (at x + 152, y + 20, width 14, height 52)
        int energyHeight = this.menu.getScaledEnergy(52);
        if (energyHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 152, y + 72 - energyHeight, 190.0f, 52.0f - energyHeight, 14, energyHeight, 256, 256);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);


        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Oxygen Tooltip
        if (mouseX >= x + 23 && mouseX <= x + 39 && mouseY >= y + 19 && mouseY <= y + 73) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§bOxygen Level (O₂)"),
                    Component.literal(String.format("§f%,d / %,d mB", this.menu.getOxygenAmount(), this.menu.getMaxOxygen())),
                    Component.literal("§7Draws 100 mB per ingot")
            ), mouseX, mouseY);
        }

        // Energy Tooltip
        if (mouseX >= x + 151 && mouseX <= x + 167 && mouseY >= y + 19 && mouseY <= y + 73) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§e⚡ Energy Buffer"),
                    Component.literal(String.format("§6%,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy())),
                    Component.literal("§7Usage: 100 FE/t")
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 4) {
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e📥 Bauxite Ore Input"),
                        Component.literal("§7Insert Raw Bauxite or Bauxite Dust:"),
                        Component.literal("§7Smelted using pure pressurized oxygen.")
                ), mouseX, mouseY);
                case 1 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§b💨 Oxygen Canister In"),
                        Component.literal("§fRequired: §aOxygen Canister (O₂)"),
                        Component.literal("§7Provides pure oxygen for bauxite Bayer reduction.")
                ), mouseX, mouseY);
                case 2 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§7💨 Empty Canister Out"),
                        Component.literal("§7Depleted canisters appear here for refilling.")
                ), mouseX, mouseY);
                case 3 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§a✨ Pure Aluminum Ingot"),
                        Component.literal("§7Refined aluminum ingots appear here.")
                ), mouseX, mouseY);
            }
        }
    }
}
