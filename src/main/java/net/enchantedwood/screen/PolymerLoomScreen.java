package net.enchantedwood.screen;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.entity.PolymerLoomBlockEntity;
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
public class PolymerLoomScreen extends AbstractContainerScreen<PolymerLoomScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/polymer_loom_gui.png");

    public PolymerLoomScreen(PolymerLoomScreenHandler handler, Inventory inventory, Component title) {
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

        // Cook Progress Arrow at x + 76, y + 35 (width 24, height 17)
        int cookWidth = this.menu.getScaledCookProgress(24);
        if (cookWidth > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 76, y + 35, 176.0f, 14.0f, cookWidth, 17, 256, 256);
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
                    Component.literal("§7Draw: §a40 FE/t §7(Active Weaving)")
            ), mouseX, mouseY);
        }

        // Gear Tooltip (when gear is installed)
        if (mouseX >= x + 151 && mouseX <= x + 169 && mouseY >= y + 7 && mouseY <= y + 25 && this.menu.getGearTier() != GearTier.NONE) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§d⚙ Installed Loom Gear"),
                    Component.literal(String.format("§7Overclock Tier: §f%s", this.menu.getGearTier().name())),
                    Component.literal(String.format("§aWeaving Speed: §e%d ticks/item", PolymerLoomBlockEntity.getTierCookTime(this.menu.getGearTier())))
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 5) {
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e🧶 Raw Polymer / Textile Feed"),
                        Component.literal("§7Insert: §fRubber §7or §fSterile Polymer Fabric"),
                        Component.literal("§8Polymer base for spinning lint-free textiles.")
                ), mouseX, mouseY);
                case 1 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§b🧵 Reinforcing Fibers / Glass"),
                        Component.literal("§7Insert: §fString, White Wool, Glass Pane, Aluminum Ingot"),
                        Component.literal("§8Tensile threads woven into the polymer matrix.")
                ), mouseX, mouseY);
                case 2 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6⚙ Additive / Hermetic Sealant"),
                        Component.literal("§7Insert: §fSilicon, Silicon Wafer, Titanium Nugget, Rubber"),
                        Component.literal("§8Anti-static semiconductor / hermetic bounding agents.")
                ), mouseX, mouseY);
                case 3 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§d✨ Finished Cleanroom Product"),
                        Component.literal("§7Outputs:"),
                        Component.literal("§f• Sterile Polymer Fabric (4×)"),
                        Component.literal("§f• Cleanroom Hood, Smock, Trousers, Booties")
                ), mouseX, mouseY);
                case 4 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§5⚙ Loom Overclock Gear Slot"),
                        Component.literal("§7Insert: §fMachine Speed Gear (Iron to Netherite)"),
                        Component.literal("§8Dramatically accelerates spinning & weaving speed.")
                ), mouseX, mouseY);
            }
        }
    }
}
