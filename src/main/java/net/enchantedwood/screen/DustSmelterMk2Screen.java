package net.enchantedwood.screen;

import net.enchantedwood.EnchantedWoodMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;

@Environment(EnvType.CLIENT)
public class DustSmelterMk2Screen extends AbstractContainerScreen<DustSmelterMk2ScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/dust_smelter_mk2_gui.png");

    public DustSmelterMk2Screen(DustSmelterMk2ScreenHandler handler, Inventory inventory, Component title) {
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

        // 1. Energy Bar at x + 18, y + 20 (width 16, height 50) using UV at u=208, v=0
        int energyHeight = this.menu.getScaledEnergy(50);
        if (energyHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 18, y + 70 - energyHeight, 208.0f, 50.0f - energyHeight, 16, energyHeight, 256, 256);
        }

        // 2. Dual Progress Arrows at x + 77, y + 23 and y + 47 (width 24, height 17)
        int cookWidth = this.menu.getScaledCookProgress(24);
        if (cookWidth > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 77, y + 23, 176.0f, 14.0f, cookWidth, 17, 256, 256);
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 77, y + 47, 176.0f, 32.0f, cookWidth, 17, 256, 256);

            // Active dual-channel smelting sparks
            if (this.menu.isCooking()) {
                long time = System.currentTimeMillis();
                int sparkX = x + 77 + cookWidth - 1;
                int sparkY1 = y + 31 + (int) ((time / 100) % 3) - 1;
                int sparkY2 = y + 55 + (int) ((time / 120) % 3) - 1;
                int sparkColor = ((time / 80) % 2 == 0) ? 0xFFFFD700 : 0xFFFF4500;
                context.fill(sparkX, sparkY1, sparkX + 2, sparkY1 + 2, sparkColor);
                context.fill(sparkX, sparkY2, sparkX + 2, sparkY2 + 2, sparkColor);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFCC60, false);
        context.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF373A40, false);
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
                    Component.literal("§7Base Usage: 60 FE/t (Dual Induction Lines)")
            ), mouseX, mouseY);
        }

        // Gear Tooltip (when gear installed)
        if (mouseX >= x + 151 && mouseX <= x + 169 && mouseY >= y + 7 && mouseY <= y + 25 && this.menu.getGearTier() != net.enchantedwood.block.custom.GearTier.NONE) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§d⚙ Installed Gear Upgrade"),
                    Component.literal(String.format("§7Speed Tier: §f%s", this.menu.getGearTier().name())),
                    Component.literal("§8Accelerates both smelting lanes simultaneously.")
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 5) {
            switch (this.hoveredSlot.index) {
                case 0, 1 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e📥 Dual Dust Input Slot"),
                        Component.literal("§7Insert metal dusts (Lane " + (this.hoveredSlot.index == 0 ? "A" : "B") + "):"),
                        Component.literal("§f• Iron, Copper, Gold, Titanium, Bauxite, Tin..."),
                        Component.literal("§7Smelts 2 items simultaneously.")
                ), mouseX, mouseY);
                case 2, 3 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§a✨ Master Ingot Output"),
                        Component.literal("§7Smelted metal ingots appear here.")
                ), mouseX, mouseY);
                case 4 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§d⚙️ Gear Upgrade Slot"),
                        Component.literal("§7Insert a Gear or Blaze Overclock Core:"),
                        Component.literal("§f• Drastically accelerates dual-channel smelting speed.")
                ), mouseX, mouseY);
            }
        }
    }
}
