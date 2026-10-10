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
public class RoadPaverScreen extends AbstractContainerScreen<RoadPaverScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/road_paver_gui.png");

    public RoadPaverScreen(RoadPaverScreenHandler handler, Inventory inventory, Component title) {
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
        // Battery Slot Box at (12, 56) -> frame at (11, 55)
        drawSlotBox(context, x + 11, y + 55);

        // 2. Right: Engine Fuel Gauge Frame & Fill
        drawGaugeFrame(context, x + 148, y + 14, 16, 38);
        int fuelHeight = this.menu.getScaledFuel();
        if (fuelHeight > 0) {
            context.fillGradient(x + 149, y + 51 - fuelHeight, x + 163, y + 51, 0xFFFF6600, 0xFFCC2200);
        }
        // Fuel Slot Box at (148, 56) -> frame at (147, 55)
        drawSlotBox(context, x + 147, y + 55);

        // 3. Section Labels
        context.text(this.font, Component.literal("§e⚡PWR"), x + 10, y + 4, 0x555555, false);
        context.text(this.font, Component.literal("§6🔥GAS"), x + 146, y + 4, 0x555555, false);

        // 4. Active Paving Indicator
        if (this.menu.isPaving()) {
            context.text(this.font, Component.literal("§a▶ PAVING ROAD"), x + 50, y + 74, 0x55FF55, false);
        } else {
            context.text(this.font, Component.literal("§7⏸ IDLE"), x + 72, y + 74, 0x888888, false);
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
                    Component.literal("§7Draws 50 FE per 3-block row"),
                    Component.literal("§8Place battery packs or connect cables to charge")
            ), mouseX, mouseY);
        }

        // Fuel Tooltip (Right Gauge + Fuel Slot)
        if (mouseX >= x + 147 && mouseX <= x + 165 && mouseY >= y + 14 && mouseY <= y + 74) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§6🔥 Engine Combustion Fuel"),
                    Component.literal(String.format("§f%,d / %,d Fuel", this.menu.getFuelLevel(), this.menu.getMaxFuel())),
                    Component.literal("§7Burn time for compaction roller engine"),
                    Component.literal("§8Accepts Gasoline, Biofuel, High-Octane, or Coal")
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 11) {
            if (this.hoveredSlot.index < 9) {
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e🛣️ Road Material Hopper (Slot " + (this.hoveredSlot.index + 1) + "/9)"),
                        Component.literal("§7Insert paving materials:"),
                        Component.literal("§f• Asphalt Blocks, Asphalt Slabs, Concrete Curbs, Clay")
                ), mouseX, mouseY);
            } else if (this.hoveredSlot.index == 9) {
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e🔋 Battery Charging Slot"),
                        Component.literal("§7Insert portable batteries to power the electrical guidance system.")
                ), mouseX, mouseY);
            } else if (this.hoveredSlot.index == 10) {
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6⛽ Engine Fuel Slot"),
                        Component.literal("§7Insert combustible engine fuel:"),
                        Component.literal("§f• Gasoline Canister, Biofuel Canister, High-Octane, Coal")
                ), mouseX, mouseY);
            }
        }
    }
}
