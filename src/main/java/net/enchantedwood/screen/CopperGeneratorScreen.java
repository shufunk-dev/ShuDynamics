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
public class CopperGeneratorScreen extends AbstractContainerScreen<CopperGeneratorScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/copper_generator_gui.png");

    public CopperGeneratorScreen(CopperGeneratorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // 1. Draw burning flame
        if (this.menu.isBurning()) {
            int fuelHeight = this.menu.getScaledFuelProgress(14);
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 81, y + 49 - fuelHeight, 176.0f, 14.0f - fuelHeight, 14, fuelHeight + 1, 256, 256);
        }

        // 2. Draw Energy Bar (height = 50px, at x + 138, y + 20)
        int energyHeight = this.menu.getScaledEnergy(50);
        if (energyHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 138, y + 70 - energyHeight, 192.0f, 50.0f - energyHeight, 16, energyHeight, 256, 256);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);


        // Energy Bar Tooltip
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        if (mouseX >= x + 137 && mouseX <= x + 155 && mouseY >= y + 19 && mouseY <= y + 71) {
            String energyText = String.format("%,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy());
            String rateText = String.format("Output: +%d FE/t", this.menu.getGenerationRate());
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§6Energy Buffer"),
                    Component.literal("§e" + energyText),
                    Component.literal("§a" + rateText)
            ), mouseX, mouseY);
        }

        // Empty Fuel Slot Tooltip
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index == 0) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§6🔥 Solid Fuel Slot"),
                    Component.literal("§7Insert combustible fuel:"),
                    Component.literal("§f• Coal, Charcoal, Coke Coal, Wood, Fire Crystal")
            ), mouseX, mouseY);
        }
    }
}
