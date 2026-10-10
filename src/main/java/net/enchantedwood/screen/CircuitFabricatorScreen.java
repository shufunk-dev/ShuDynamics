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
public class CircuitFabricatorScreen extends AbstractContainerScreen<CircuitFabricatorScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/circuit_fabricator_gui.png");

    public CircuitFabricatorScreen(CircuitFabricatorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // 1. Energy Bar at x + 10, y + 18 (width 12, height 50)
        int energyHeight = this.menu.getScaledEnergy(50);
        if (energyHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 10, y + 68 - energyHeight, 176.0f, 50.0f - energyHeight, 12, energyHeight, 256, 256);
        }

        // 2. Fabrication Laser / Progress Arrow at x + 88, y + 34 (width 24, height 17)
        int cookWidth = this.menu.getScaledCookProgress(24);
        if (cookWidth > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 88, y + 34, 188.0f, 0.0f, cookWidth, 17, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        context.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);


        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Energy Bar Tooltip
        if (mouseX >= x + 9 && mouseX <= x + 23 && mouseY >= y + 17 && mouseY <= y + 69) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§6⚡ Energy Buffer"),
                    Component.literal(String.format("§e%,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy())),
                    Component.literal("§7Draw: 35 FE/t")
            ), mouseX, mouseY);
        }

        // Progress Tooltip
        if (mouseX >= x + 87 && mouseX <= x + 113 && mouseY >= y + 33 && mouseY <= y + 52) {
            int cookProgress = this.menu.getScaledCookProgress(100);
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§b⚡ Circuit Fabrication Laser"),
                    Component.literal(String.format("§7Progress: §f%d%%", cookProgress)),
                    Component.literal("§8Aligns components and sinters microscopic silicon traces.")
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 6) {
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§b💿 Semiconductor Substrate"),
                        Component.literal("§7Insert: §fSilicon Wafer §7(Basic/Metallurgy),"),
                        Component.literal("§7        §fBasic Chip §7(for Advanced),"),
                        Component.literal("§7        §fAdvanced Chip §7(for Quantum)"),
                        Component.literal("§8Foundation silicon wafer etched by the precision laser.")
                ), mouseX, mouseY);
                case 1 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e⚡ Conductor Component"),
                        Component.literal("§7Insert: §fCopper Ingot, Diamond, Netherite Dust, or Gold Ingot"),
                        Component.literal("§8Conductive trace element for circuit pathways.")
                ), mouseX, mouseY);
                case 2 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6✨ Logic & Semiconductor Gate"),
                        Component.literal("§7Insert: §fGold Nugget, Glowstone Dust, Blaze Powder, or Redstone"),
                        Component.literal("§8Transistor gate and logic frequency modulator.")
                ), mouseX, mouseY);
                case 3 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§a🔮 Signal & Dielectric Enhancer"),
                        Component.literal("§7Insert: §fRedstone, Lapis Lazuli, Enchanted Dust, or Zirconia Nodule"),
                        Component.literal("§8Stabilizing dielectric flux for micro-architecture.")
                ), mouseX, mouseY);
                case 4 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§d💾 Fabricated Microchip / Processor"),
                        Component.literal("§7Outputs finished microcontrollers:"),
                        Component.literal("§f• Basic, Advanced, Quantum Computer Chips"),
                        Component.literal("§6• Metallurgy Controller Chip"),
                        Component.literal("§8Laser-sintered integrated circuits.")
                ), mouseX, mouseY);
                case 5 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6⚙ Overclock & Gear Socket"),
                        Component.literal("§7Accepts: §fCopper..Diamond Gears §7or §6Blaze Overclock Core"),
                        Component.literal("§8Accelerates laser fabrication speed up to 4.0×.")
                ), mouseX, mouseY);
            }
        }
    }
}
