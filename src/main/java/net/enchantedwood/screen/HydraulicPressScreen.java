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
public class HydraulicPressScreen extends AbstractContainerScreen<HydraulicPressScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/hydraulic_press_gui.png");

    public HydraulicPressScreen(HydraulicPressScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // 1. Progress Arrow
        int progress = this.menu.getScaledProgress();
        if (progress > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 79, y + 34, 176.0f, 14.0f, progress, 16, 256, 256);
        }

        // 2. Energy Bar
        int energyHeight = this.menu.getScaledEnergy();
        if (energyHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 152, y + 72 - energyHeight, 190.0f, 52.0f - energyHeight, 14, energyHeight, 256, 256);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);


        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Energy Tooltip
        if (mouseX >= x + 151 && mouseX <= x + 167 && mouseY >= y + 19 && mouseY <= y + 73) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§e⚡ Energy Storage"),
                    Component.literal(String.format("§f%,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy())),
                    Component.literal("§7Draws 40 FE/t during plate stamping.")
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 3) {
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e📥 Stamping Input Slot"),
                        Component.literal("§7Insert metal ingots or blocks to stamp:"),
                        Component.literal("§f• Tungsten, Cobalt, Ardite, Manyullyn, Iron")
                ), mouseX, mouseY);
                case 1 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6⚙️ Gear Upgrade Slot"),
                        Component.literal("§7Insert a Gear or Blaze Overclock Core:"),
                        Component.literal("§f• Dramatically speeds up plate stamping.")
                ), mouseX, mouseY);
                case 2 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§a✨ Stamped Plate Output"),
                        Component.literal("§7Finished metal plates appear here.")
                ), mouseX, mouseY);
            }
        }
    }
}
