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
public class MagmaCrucibleScreen extends AbstractContainerScreen<MagmaCrucibleScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/magma_crucible_gui.png");

    public MagmaCrucibleScreen(MagmaCrucibleScreenHandler handler, Inventory inventory, Component title) {
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

        // 2. Melt Progress Arrow at x + 66, y + 34 (width 24, height 17) using UV at u=176, v=14
        int cookWidth = this.menu.getScaledCookProgress(24);
        if (cookWidth > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 66, y + 34, 176.0f, 14.0f, cookWidth, 17, 256, 256);

            // Active smelting molten thermal flecks at the pouring tip
            if (this.menu.isCooking()) {
                long time = System.currentTimeMillis();
                int sparkX = x + 66 + cookWidth - 1;
                int sparkY = y + 42 + (int) ((time / 100) % 3) - 1;
                int sparkColor = ((time / 80) % 2 == 0) ? 0xFFFFD700 : 0xFFFF4500;
                context.fill(sparkX, sparkY, sparkX + 2, sparkY + 2, sparkColor);
            }
        }

        // 3. Lava Tank at x + 96, y + 20 (width 16, height 50) using UV at u=226, v=0
        int lavaHeight = this.menu.getScaledLava(50);
        if (lavaHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 96, y + 70 - lavaHeight, 226.0f, 50.0f - lavaHeight, 16, lavaHeight, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFAA00, false);
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
                    Component.literal("§6⚡ Induction Energy Buffer"),
                    Component.literal(String.format("§e%,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy())),
                    Component.literal("§7Base Draw: 35 FE/t")
            ), mouseX, mouseY);
        }

        // Lava Tank Tooltip
        if (mouseX >= x + 95 && mouseX <= x + 113 && mouseY >= y + 19 && mouseY <= y + 71) {
            int lava = this.menu.getLavaAmount();
            int buckets = lava / 1000;
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§c🌋 Lava Buffer Tank"),
                    Component.literal(String.format("§6%,d / 10,000 mB §7(%d Buckets)", lava, buckets)),
                    Component.literal("§7Melts Basalt, Blackstone, Magma, and Netherrack.")
            ), mouseX, mouseY);
        }

        // Gear Tooltip (when gear installed)
        if (mouseX >= x + 151 && mouseX <= x + 169 && mouseY >= y + 7 && mouseY <= y + 25 && this.menu.getGearTier() != net.enchantedwood.block.custom.GearTier.NONE) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§d⚙ Installed Gear Upgrade"),
                    Component.literal(String.format("§7Speed Tier: §f%s", this.menu.getGearTier().name())),
                    Component.literal("§8Dramatically accelerates thermal liquefaction.")
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 5) {
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e📥 Geological Melting Input"),
                        Component.literal("§7Insert volcanic rock or stone:"),
                        Component.literal("§f• Basalt, Blackstone, Netherrack, Magma Block"),
                        Component.literal("§7Melts stone into fluid lava.")
                ), mouseX, mouseY);
                case 1 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6💎 Mineral Byproduct Output"),
                        Component.literal("§7Precious slag, sulfur, or minerals appear here.")
                ), mouseX, mouseY);
                case 2 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§b🪣 Empty Bucket Input"),
                        Component.literal("§7Place empty Iron or Copper Buckets to"),
                        Component.literal("§7automatically bottle melted lava from the internal tank.")
                ), mouseX, mouseY);
                case 3 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§c🔥 Lava Bucket Output"),
                        Component.literal("§7Filled lava buckets are extracted here.")
                ), mouseX, mouseY);
                case 4 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§d⚙️ Gear Upgrade Slot"),
                        Component.literal("§7Insert a Gear or Blaze Overclock Core:"),
                        Component.literal("§f• Dramatically increases melting temperature & speed.")
                ), mouseX, mouseY);
            }
        }
    }
}
