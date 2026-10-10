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
public class HarmonicRecordPressScreen extends AbstractContainerScreen<HarmonicRecordPressScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/harmonic_record_press_gui.png");

    public HarmonicRecordPressScreen(HarmonicRecordPressScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // 1. Progress Needle / Record Cutter Arrow
        int progress = this.menu.getScaledProgress();
        if (progress > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 76, y + 35, 176.0f, 14.0f, progress, 16, 256, 256);
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
                    Component.literal("§7Draws 40 FE/t during record pressing.")
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 4) {
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e💿 Blank Vinyl Slot"),
                        Component.literal("§7Insert a §fBlank Vinyl Disc§7 to cut."),
                        Component.literal("§8Crafted with Rubber + Resin + Iron Nugget.")
                ), mouseX, mouseY);
                case 1 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§d🔮 Acoustic Catalyst Slot"),
                        Component.literal("§7Insert a thematic catalyst to imprint:"),
                        Component.literal("§5• Crying Obsidian §8(Rip the Sky Wide)"),
                        Component.literal("§c• Fire Crystal §8(Rift of the Colossus)"),
                        Component.literal("§6• High-Octane Fuel §8(Highway Overdrive)"),
                        Component.literal("§b• Silicon Wafer §8(Sterile Protocol)"),
                        Component.literal("§a• Storage Crystal §8(Subroutine 64k)"),
                        Component.literal("§d• Master Rainbow Roll §8(Haven Bloom)"),
                        Component.literal("§4• Manyullyn Ingot §8(Heart of the Crucible)"),
                        Component.literal("§9• Hydrogen Canister §8(Stratosphere Break)"),
                        Component.literal("§3• Diving Mask / Nautilus §8(Abyssal Pressure)"),
                        Component.literal("§e• Sulfur Dust §8(Anoxic Echoes)"),
                        Component.literal("§7• Any Music Disc §8(Duplicates the record!)")
                ), mouseX, mouseY);
                case 2 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§a📦 Pressed Music Disc Output"),
                        Component.literal("§7The finished record is placed here.")
                ), mouseX, mouseY);
                case 3 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6⚙ Overclocking Gear Socket"),
                        Component.literal("§7Insert mechanical or enchanted gears"),
                        Component.literal("§7to accelerate pressing speed up to 8x.")
                ), mouseX, mouseY);
            }
        }
    }
}
