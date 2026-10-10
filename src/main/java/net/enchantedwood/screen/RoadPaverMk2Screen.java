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
public class RoadPaverMk2Screen extends AbstractContainerScreen<RoadPaverMk2ScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/road_paver_mk2_gui.png");

    public RoadPaverMk2Screen(RoadPaverMk2ScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    private void drawSlotBox(GuiGraphicsExtractor context, int boxX, int boxY) {
        context.fill(boxX, boxY, boxX + 18, boxY + 1, 0xFF373737);
        context.fill(boxX, boxY, boxX + 1, boxY + 18, 0xFF373737);
        context.fill(boxX + 1, boxY + 1, boxX + 17, boxY + 17, 0xFF8B8B8B);
        context.fill(boxX + 1, boxY + 17, boxX + 18, boxY + 18, 0xFFFFFFFF);
        context.fill(boxX + 17, boxY + 1, boxX + 18, boxY + 18, 0xFFFFFFFF);
    }

    private void drawGaugeFrame(GuiGraphicsExtractor context, int frameX, int frameY, int width, int height) {
        context.fill(frameX, frameY, frameX + width, frameY + 1, 0xFF373737);
        context.fill(frameX, frameY, frameX + 1, frameY + height, 0xFF373737);
        context.fill(frameX + 1, frameY + 1, frameX + width - 1, frameY + height - 1, 0xFF222222);
        context.fill(frameX + 1, frameY + height - 1, frameX + width, frameY + height, 0xFFFFFFFF);
        context.fill(frameX + width - 1, frameY + 1, frameX + width, frameY + height, 0xFFFFFFFF);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // 1. Left: Energy Gauge Frame & Fill
        drawGaugeFrame(context, x + 12, y + 14, 16, 38);
        int energyHeight = this.menu.getScaledEnergy();
        if (energyHeight > 0) {
            context.fillGradient(x + 13, y + 51 - energyHeight, x + 27, y + 51, 0xFFFFDD33, 0xFFFF9900);
        }
        drawSlotBox(context, x + 11, y + 55);

        // 2. Right: Engine Fuel Gauge Frame & Fill
        drawGaugeFrame(context, x + 148, y + 14, 16, 38);
        int fuelHeight = this.menu.getScaledFuel();
        if (fuelHeight > 0) {
            context.fillGradient(x + 149, y + 51 - fuelHeight, x + 163, y + 51, 0xFFFF6600, 0xFFCC2200);
        }
        drawSlotBox(context, x + 147, y + 55);

        // 3. Section Labels
        context.text(this.font, Component.literal("§e⚡PWR"), x + 10, y + 4, 0x555555, false);
        context.text(this.font, Component.literal("§fDECK"), x + 54, y + 6, 0x555555, false);
        context.text(this.font, Component.literal("§bPILLAR"), x + 107, y + 6, 0x555555, false);
        context.text(this.font, Component.literal("§6🔥GAS"), x + 146, y + 4, 0x555555, false);

        // 4. Active Paving Indicator
        if (this.menu.isPaving()) {
            context.text(this.font, Component.literal("§a▶ PAVING & BRIDGING"), x + 34, y + 73, 0x55FF55, false);
        } else {
            context.text(this.font, Component.literal("§7⏸ IDLE"), x + 72, y + 73, 0x888888, false);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);


        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Energy Tooltip (Left Gauge + Battery Slot)
        if (mouseX >= x + 11 && mouseX <= x + 28 && mouseY >= y + 14 && mouseY <= y + 74) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§e⚡ Electrical Guidance System"),
                    Component.literal(String.format("§f%,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy())),
                    Component.literal("§7Draws 80 FE per bridge step"),
                    Component.literal("§8Place battery packs or connect cables to charge")
            ), mouseX, mouseY);
        }

        // Fuel Tooltip (Right Gauge + Fuel Slot)
        if (mouseX >= x + 147 && mouseX <= x + 165 && mouseY >= y + 14 && mouseY <= y + 74) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§6🔥 Hydraulic Compaction Engine"),
                    Component.literal(String.format("§f%,d / %,d Fuel", this.menu.getFuelLevel(), this.menu.getMaxFuel())),
                    Component.literal("§7Powers high-tonnage road compaction roller"),
                    Component.literal("§8Accepts Gasoline, Biofuel, High-Octane, or Coal")
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 14) {
            if (this.hoveredSlot.index < 9) {
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§7Asphalt Road Deck Slot"),
                        Component.literal("§8Insert Asphalt Blocks, Slabs, Curbs, or Clay")
                ), mouseX, mouseY);
            } else if (this.hoveredSlot.index < 12) {
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§bStructural Support Pier Slot"),
                        Component.literal("§8Insert Stone Bricks, Cobble, Deepslate, or Concrete"),
                        Component.literal("§7Cast as bridge piers when crossing canyons/water")
                ), mouseX, mouseY);
            } else if (this.hoveredSlot.index == 12) {
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§eBattery Recharging Slot"),
                        Component.literal("§8Place Copper, Aluminum, Titanium, or Tungsten Battery")
                ), mouseX, mouseY);
            } else if (this.hoveredSlot.index == 13) {
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6Engine Fuel Canister Slot"),
                        Component.literal("§8Place Gasoline, Biofuel, High-Octane Canister, or Coal")
                ), mouseX, mouseY);
            }
        }
    }
}
