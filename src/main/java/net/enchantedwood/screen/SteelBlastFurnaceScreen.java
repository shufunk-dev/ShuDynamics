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
public class SteelBlastFurnaceScreen extends AbstractContainerScreen<SteelBlastFurnaceScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/steel_blast_furnace_gui.png");

    public SteelBlastFurnaceScreen(SteelBlastFurnaceScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // 1. Hydrogen Gas Bar (at x + 20, y + 20, width 14, height 52)
        int h2Height = this.menu.getScaledHydrogen(52);
        if (h2Height > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 20, y + 72 - h2Height, 176.0f, 52.0f - h2Height, 14, h2Height, 256, 256);
        }

        // 2. Cook Arrow Progress (at x + 76, y + 35, width 24, height 17)
        int cookWidth = this.menu.getScaledCookProgress(24);
        if (cookWidth > 0) {
            float vOffset = this.menu.isGreenMode() ? 71.0f : 54.0f; // Green arrow when using Green Steel H2!
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 76, y + 35, 176.0f, vOffset, cookWidth, 17, 256, 256);
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

        // Hydrogen Tooltip
        if (mouseX >= x + 19 && mouseX <= x + 35 && mouseY >= y + 19 && mouseY <= y + 73) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§dHydrogen Level (H₂)"),
                    Component.literal(String.format("§f%,d / %,d mB", this.menu.getHydrogenAmount(), this.menu.getMaxHydrogen())),
                    Component.literal("§7Direct reduction: 100 mB / ingot")
            ), mouseX, mouseY);
        }

        // Energy Tooltip
        if (mouseX >= x + 151 && mouseX <= x + 167 && mouseY >= y + 19 && mouseY <= y + 73) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§e⚡ Energy Buffer"),
                    Component.literal(String.format("§6%,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy())),
                    Component.literal("§7Usage: 200 FE/t")
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 5) {
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e📥 Iron Input Slot"),
                        Component.literal("§7Insert Iron Ingots or Iron Dust.")
                ), mouseX, mouseY);
                case 1 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§8🔥 Coke Coal Fuel Slot"),
                        Component.literal("§fRequired: §aCoke Coal"),
                        Component.literal("§7Provides high-carbon blast reduction.")
                ), mouseX, mouseY);
                case 2 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§a✨ High-Grade Steel Ingot"),
                        Component.literal("§7Refined steel ingots appear here.")
                ), mouseX, mouseY);
                case 3 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§d💨 Hydrogen Canister In (Optional)"),
                        Component.literal("§fOptional: §aHydrogen Canister (H₂)"),
                        Component.literal("§7Accelerates steel reduction reactions.")
                ), mouseX, mouseY);
                case 4 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§7💨 Empty Canister Out"),
                        Component.literal("§7Depleted hydrogen canisters appear here.")
                ), mouseX, mouseY);
            }
        }
    }
}
