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
public class ItemSalvagerScreen extends AbstractContainerScreen<ItemSalvagerScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/item_salvager_gui.png");

    public ItemSalvagerScreen(ItemSalvagerScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // 1. Energy Bar at x + 18, y + 20 (width 16, height 50)
        int energyHeight = this.menu.getScaledEnergy(50);
        if (energyHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 18, y + 70 - energyHeight, 192.0f, 50.0f - energyHeight, 16, energyHeight, 256, 256);
        }

        // 2. Progress Arrow at x + 74, y + 34 (width 24, height 17)
        int cookWidth = this.menu.getScaledCookProgress(24);
        if (cookWidth > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 74, y + 34, 176.0f, 14.0f, cookWidth, 17, 256, 256);
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
        if (mouseX >= x + 17 && mouseX <= x + 35 && mouseY >= y + 19 && mouseY <= y + 71) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§6⚡ Energy Buffer"),
                    Component.literal(String.format("§e%,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy())),
                    Component.literal("§7Usage: 40 FE/t")
            ), mouseX, mouseY);
        }

        // Gear Tooltip (when gear installed)
        if (mouseX >= x + 151 && mouseX <= x + 169 && mouseY >= y + 7 && mouseY <= y + 25 && this.menu.getGearTier() != net.enchantedwood.block.custom.GearTier.NONE) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§d⚙ Installed Gear Upgrade"),
                    Component.literal(String.format("§7Speed Tier: §f%s", this.menu.getGearTier().name()))
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 6) {
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e📥 Salvage / Deconstruction Input"),
                        Component.literal("§7Insert craftable items, tools, or blocks:"),
                        Component.literal("§f• Reclaims 100% of component ingredients")
                ), mouseX, mouseY);
                case 1, 2, 3, 4 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§a✨ Salvaged Material Output"),
                        Component.literal("§7Recovered raw materials and ingredients appear here.")
                ), mouseX, mouseY);
                case 5 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§d⚙️ Gear Upgrade Slot"),
                        Component.literal("§7Insert a Gear or Blaze Overclock Core:"),
                        Component.literal("§f• Drastically accelerates dismantling speed.")
                ), mouseX, mouseY);
            }
        }
    }
}
