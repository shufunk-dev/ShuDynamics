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
public class TungstenBatteryScreen extends AbstractContainerScreen<TungstenBatteryScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/tungsten_battery_gui.png");

    public TungstenBatteryScreen(TungstenBatteryScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // Horizontal Battery Meter (width = 100, height = 18 at x + 38, y + 36)
        int energyWidth = this.menu.getScaledEnergy(100);
        if (energyWidth > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 38, y + 36, 0.0f, 166.0f, energyWidth, 18, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);

        double percent = (double) this.menu.getEnergy() / (double) this.menu.getMaxEnergy() * 100.0;
        String energyStr = String.format("%,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy());
        int energyStrWidth = this.font.width(energyStr);
        context.text(this.font, energyStr, (this.imageWidth - energyStrWidth) / 2, 24, 0x117755, false);

        String rateStr = String.format("Max I/O: %,d FE/t (%.1f%%)", this.menu.getMaxTransfer(), percent);
        int rateStrWidth = this.font.width(rateStr);
        context.text(this.font, rateStr, (this.imageWidth - rateStrWidth) / 2, 58, 0x444444, false);

        // Slot indicators
        context.text(this.font, Component.literal("§6IN"), 19, 23, 0x555555, false);
        context.text(this.font, Component.literal("§bOUT"), 144, 23, 0x555555, false);

        context.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);


        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Battery Gauge Tooltip
        if (mouseX >= x + 37 && mouseX <= x + 139 && mouseY >= y + 35 && mouseY <= y + 55) {
            double percent = (double) this.menu.getEnergy() / (double) this.menu.getMaxEnergy() * 100.0;
            String energyText = String.format("%,d / %,d FE (%.1f%%)", this.menu.getEnergy(), this.menu.getMaxEnergy(), percent);
            String rateText = String.format("Max Transfer: %,d FE/t", this.menu.getMaxTransfer());
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§2⚡ Tier 4 Tungsten Heavy Battery"),
                    Component.literal("§a" + energyText),
                    Component.literal("§7" + rateText)
            ), mouseX, mouseY);
        }

        // Discharge Slot Tooltip
        if (mouseX >= x + 15 && mouseX <= x + 33 && mouseY >= y + 34 && mouseY <= y + 52) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§6📥 Discharge Slot (IN)"),
                    Component.literal("§7Place any battery pack here to drain power into this cell.")
            ), mouseX, mouseY);
        }

        // Charge Slot Tooltip
        if (mouseX >= x + 143 && mouseX <= x + 161 && mouseY >= y + 34 && mouseY <= y + 52) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§b⚡ Charge Slot (OUT)"),
                    Component.literal("§7Place any battery pack or tool here to rapidly recharge it.")
            ), mouseX, mouseY);
        }
    }
}
