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
public class GeothermalGeneratorScreen extends AbstractContainerScreen<GeothermalGeneratorScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/geothermal_generator_gui.png");

    public GeothermalGeneratorScreen(GeothermalGeneratorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // 1. Lava Tank (width = 16, height = 50, at x + 18, y + 20)
        int lavaHeight = this.menu.getScaledLava(50);
        if (lavaHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 18, y + 70 - lavaHeight, 212.0f, 50.0f - lavaHeight, 16, lavaHeight, 256, 256);
        }

        // 2. Burning Flame (at x + 76, y + 36)
        if (this.menu.isBurning()) {
            int fuelHeight = this.menu.getScaledFuelProgress(14);
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 76, y + 50 - fuelHeight, 176.0f, 14.0f - fuelHeight, 14, fuelHeight + 1, 256, 256);
        }

        // 3. Energy Bar (width = 16, height = 50, at x + 121, y + 20)
        int energyHeight = this.menu.getScaledEnergy(50);
        if (energyHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 121, y + 70 - energyHeight, 192.0f, 50.0f - energyHeight, 16, energyHeight, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractLabels(context, mouseX, mouseY);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);


        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Lava Tank Tooltip
        if (mouseX >= x + 17 && mouseX <= x + 35 && mouseY >= y + 19 && mouseY <= y + 71) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§c🌋 Lava Buffer Tank"),
                    Component.literal(String.format("§6%,d / 10,000 mB", this.menu.getLavaAmount())),
                    Component.literal("§7Accepts Lava Buckets, Magma Blocks, Fire Crystals, or Pumps")
            ), mouseX, mouseY);
        }

        // Energy Bar Tooltip
        if (mouseX >= x + 120 && mouseX <= x + 138 && mouseY >= y + 19 && mouseY <= y + 71) {
            int rate = Math.round(750 * this.menu.getGearMultiplier());
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§6⚡ Energy Buffer"),
                    Component.literal(String.format("§e%,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy())),
                    Component.literal(String.format("§aGeneration: +%d FE/t (%s Gear)", rate, this.menu.getGearTier().name()))
            ), mouseX, mouseY);
        }

        // Gear Slot Tooltip (when installed)
        if (mouseX >= x + 151 && mouseX <= x + 169 && mouseY >= y + 7 && mouseY <= y + 25 && this.menu.getGearTier() != net.enchantedwood.block.custom.GearTier.NONE) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§d⚙ Installed Gear Upgrade"),
                    Component.literal(String.format("§7Current: §f%s (x%.2f Multiplier)", this.menu.getGearTier().name(), this.menu.getGearMultiplier()))
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 3) {
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e📥 Thermal Fuel Input"),
                        Component.literal("§7Insert thermal power sources:"),
                        Component.literal("§f• Lava Buckets, Magma Blocks, Fire Crystals")
                ), mouseX, mouseY);
                case 1 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§b🪣 Empty Bucket Output"),
                        Component.literal("§7Emptied lava containers appear here.")
                ), mouseX, mouseY);
                case 2 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§d⚙️ Gear Upgrade Slot"),
                        Component.literal("§7Insert a Gear or Blaze Overclock Core:"),
                        Component.literal("§f• Dramatically boosts geothermal FE/t generation.")
                ), mouseX, mouseY);
            }
        }
    }
}
