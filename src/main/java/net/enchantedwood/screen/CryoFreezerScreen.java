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
import net.enchantedwood.block.custom.GearTier;

import java.util.List;

@Environment(EnvType.CLIENT)
public class CryoFreezerScreen extends AbstractContainerScreen<CryoFreezerScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/cryo_freezer_gui.png");

    public CryoFreezerScreen(CryoFreezerScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // 1. Energy Gauge at x + 16, y + 20 (width 14, height 50)
        int energyHeight = this.menu.getScaledEnergy(50);
        if (energyHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 16, y + 70 - energyHeight, 192.0f, 50.0f - energyHeight, 14, energyHeight, 256, 256);
        }

        // 2. Water Reservoir at x + 42, y + 20 (width 18, height 50)
        int waterHeight = this.menu.getScaledWater(50);
        if (waterHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 42, y + 70 - waterHeight, 212.0f, 50.0f - waterHeight, 18, waterHeight, 256, 256);
        }

        // 3. Freezing Progress Indicator at x + 120, y + 35 (width 22, height 16)
        int freezeWidth = this.menu.getScaledProgress(22);
        if (freezeWidth > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 120, y + 35, 176.0f, 14.0f, freezeWidth, 16, 256, 256);
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

        // Energy Bar Tooltip (x: 15..30, y: 19..70)
        if (mouseX >= x + 15 && mouseX <= x + 30 && mouseY >= y + 19 && mouseY <= y + 70) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§b⚡ Energy Buffer"),
                    Component.literal(String.format("§e%,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy())),
                    Component.literal("§7Usage: 25 FE/t (reduced by gears)")
            ), mouseX, mouseY);
        }

        // Water Tank Tooltip (x: 41..60, y: 19..70)
        if (mouseX >= x + 41 && mouseX <= x + 60 && mouseY >= y + 19 && mouseY <= y + 70) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§9💧 Internal Water Reservoir"),
                    Component.literal(String.format("§b%,d / 10,000 mB", this.menu.getWaterAmount())),
                    Component.literal("§7Feeds cryogenic freezing coils. Connects to Water Pump & pipes.")
            ), mouseX, mouseY);
        }

        // Freezing Progress Tooltip (x: 119..142, y: 34..51)
        if (mouseX >= x + 119 && mouseX <= x + 142 && mouseY >= y + 34 && mouseY <= y + 51) {
            int pct = (this.menu.getFreezeProgress() * 100) / Math.max(1, this.menu.getTotalFreezeTime());
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§b❄ Cryogenic Freezing"),
                    Component.literal(String.format("§7Progress: §f%d%%", pct))
            ), mouseX, mouseY);
        }

        // Gear Tooltip (when gear installed, x: 151..169, y: 7..25)
        if (this.menu.getActiveGearTier() != GearTier.NONE) {
            if (mouseX >= x + 151 && mouseX <= x + 169 && mouseY >= y + 7 && mouseY <= y + 25) {
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§d⚙ Installed Gear Upgrade"),
                        Component.literal(String.format("§7Tier: §f%s", this.menu.getActiveGearTier().name())),
                        Component.literal(String.format("§7Freezing Time: §b%d ticks", this.menu.getTotalFreezeTime()))
                ), mouseX, mouseY);
            }
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 5) {
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§b🪣 Water Bucket Input"),
                        Component.literal("§7Insert Water Buckets to replenish the internal tank.")
                ), mouseX, mouseY);
                case 1 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§7🪣 Bucket Return"),
                        Component.literal("§7Empty buckets returned here.")
                ), mouseX, mouseY);
                case 2 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§f❄ Solid Material Input (Optional)"),
                        Component.literal("§7Leave empty for Water -> Ice,"),
                        Component.literal("§7or insert Ice (for Packed Ice) or Packed Ice (for Blue Ice).")
                ), mouseX, mouseY);
                case 3 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§b🧊 Frozen Product Output"),
                        Component.literal("§7Manufactured Ice, Packed Ice, or Blue Ice appears here.")
                ), mouseX, mouseY);
                case 4 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§d⚙️ Gear Upgrade Slot"),
                        Component.literal("§7Insert an alloy Gear to overclock freezing speed.")
                ), mouseX, mouseY);
            }
        }
    }
}
