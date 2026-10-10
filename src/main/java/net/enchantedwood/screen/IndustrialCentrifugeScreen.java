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
public class IndustrialCentrifugeScreen extends AbstractContainerScreen<IndustrialCentrifugeScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/industrial_centrifuge_gui.png");

    public IndustrialCentrifugeScreen(IndustrialCentrifugeScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // Energy Bar at x + 18, y + 20 (width 16, height 50)
        int energyHeight = this.menu.getScaledEnergy(50);
        if (energyHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 18, y + 70 - energyHeight, 192.0f, 50.0f - energyHeight, 16, energyHeight, 256, 256);
        }

        // Cook Progress Arrow at x + 72, y + 34 (width 24, height 17)
        int cookWidth = this.menu.getScaledCookProgress(24);
        if (cookWidth > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 72, y + 34, 176.0f, 14.0f, cookWidth, 17, 256, 256);
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
                    Component.literal("§e⚡ Energy Storage"),
                    Component.literal(String.format("§6%,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy())),
                    Component.literal("§7Base Draw: §a25 FE/t §7(Active Spin)")
            ), mouseX, mouseY);
        }

        // Gear Tooltip (when gear is installed)
        if (mouseX >= x + 151 && mouseX <= x + 169 && mouseY >= y + 7 && mouseY <= y + 25 && this.menu.getGearTier() != net.enchantedwood.block.custom.GearTier.NONE) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§d⚙ Installed Centrifuge Gear"),
                    Component.literal(String.format("§7Overclock Tier: §f%s", this.menu.getGearTier().name())),
                    Component.literal(String.format("§aSeparation Speed: §e%d ticks/item", net.enchantedwood.block.entity.IndustrialCentrifugeBlockEntity.getTierCookTime(this.menu.getGearTier())))
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 4) {
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e📥 Raw Botanical / Mineral Input"),
                        Component.literal("§7Insert unrefined organic or mineral matter:"),
                        Component.literal("§f• Slime Balls, Magma Cream, Crimson Fungus, Kelp,"),
                        Component.literal("§f• Dragon Fruit, Wasabi Root, Glow Berries, etc.")
                ), mouseX, mouseY);
                case 1 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§a✨ Purified Chemical Essence Output"),
                        Component.literal("§7Refined active medical extract appears here:"),
                        Component.literal("§f• Alkaline Base, Cryo-Thermal, Oxygenated, Nanite, Adrenal")
                ), mouseX, mouseY);
                case 2 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6🛢️ Chemical / Mineral Byproduct Output"),
                        Component.literal("§7Secondary separated matter appears here:"),
                        Component.literal("§f• Sulfur Dust, Volcanic Ash, Bone Meal, Sugar, Glowstone")
                ), mouseX, mouseY);
                case 3 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§d⚙️ Gear Overclock Slot"),
                        Component.literal("§7Insert a Gear or Blaze Overclock Core:"),
                        Component.literal("§f• Dramatically accelerates centrifugal separation speed.")
                ), mouseX, mouseY);
            }
        }
    }
}
