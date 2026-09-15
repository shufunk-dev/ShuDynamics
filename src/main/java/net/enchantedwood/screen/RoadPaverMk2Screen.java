package net.enchantedwood.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;

import java.util.List;

@Environment(EnvType.CLIENT)
public class RoadPaverMk2Screen extends HandledScreen<RoadPaverMk2ScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/road_paver_mk2_gui.png");

    public RoadPaverMk2Screen(RoadPaverMk2ScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    private void drawSlotBox(DrawContext context, int boxX, int boxY) {
        context.fill(boxX, boxY, boxX + 18, boxY + 1, 0xFF373737);
        context.fill(boxX, boxY, boxX + 1, boxY + 18, 0xFF373737);
        context.fill(boxX + 1, boxY + 1, boxX + 17, boxY + 17, 0xFF8B8B8B);
        context.fill(boxX + 1, boxY + 17, boxX + 18, boxY + 18, 0xFFFFFFFF);
        context.fill(boxX + 17, boxY + 1, boxX + 18, boxY + 18, 0xFFFFFFFF);
    }

    private void drawGaugeFrame(DrawContext context, int frameX, int frameY, int width, int height) {
        context.fill(frameX, frameY, frameX + width, frameY + 1, 0xFF373737);
        context.fill(frameX, frameY, frameX + 1, frameY + height, 0xFF373737);
        context.fill(frameX + 1, frameY + 1, frameX + width - 1, frameY + height - 1, 0xFF222222);
        context.fill(frameX + 1, frameY + height - 1, frameX + width, frameY + height, 0xFFFFFFFF);
        context.fill(frameX + width - 1, frameY + 1, frameX + width, frameY + height, 0xFFFFFFFF);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // 1. Left: Energy Gauge Frame & Fill
        drawGaugeFrame(context, x + 12, y + 14, 16, 38);
        int energyHeight = this.handler.getScaledEnergy();
        if (energyHeight > 0) {
            context.fillGradient(x + 13, y + 51 - energyHeight, x + 27, y + 51, 0xFFFFDD33, 0xFFFF9900);
        }
        drawSlotBox(context, x + 11, y + 55);

        // 2. Right: Engine Fuel Gauge Frame & Fill
        drawGaugeFrame(context, x + 148, y + 14, 16, 38);
        int fuelHeight = this.handler.getScaledFuel();
        if (fuelHeight > 0) {
            context.fillGradient(x + 149, y + 51 - fuelHeight, x + 163, y + 51, 0xFFFF6600, 0xFFCC2200);
        }
        drawSlotBox(context, x + 147, y + 55);

        // 3. Section Labels
        context.drawText(this.textRenderer, Text.literal("§e⚡PWR"), x + 10, y + 4, 0x555555, false);
        context.drawText(this.textRenderer, Text.literal("§fDECK"), x + 54, y + 6, 0x555555, false);
        context.drawText(this.textRenderer, Text.literal("§bPILLAR"), x + 107, y + 6, 0x555555, false);
        context.drawText(this.textRenderer, Text.literal("§6🔥GAS"), x + 146, y + 4, 0x555555, false);

        // 4. Active Paving Indicator
        if (this.handler.isPaving()) {
            context.drawText(this.textRenderer, Text.literal("§a▶ PAVING & BRIDGING"), x + 34, y + 73, 0x55FF55, false);
        } else {
            context.drawText(this.textRenderer, Text.literal("§7⏸ IDLE"), x + 72, y + 73, 0x888888, false);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Energy Tooltip (Left Gauge + Battery Slot)
        if (mouseX >= x + 11 && mouseX <= x + 28 && mouseY >= y + 14 && mouseY <= y + 74) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§e⚡ Electrical Guidance System"),
                    Text.literal(String.format("§f%,d / %,d FE", this.handler.getEnergy(), this.handler.getMaxEnergy())),
                    Text.literal("§7Draws 80 FE per bridge step"),
                    Text.literal("§8Place battery packs or connect cables to charge")
            ), mouseX, mouseY);
        }

        // Fuel Tooltip (Right Gauge + Fuel Slot)
        if (mouseX >= x + 147 && mouseX <= x + 165 && mouseY >= y + 14 && mouseY <= y + 74) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§6🔥 Hydraulic Compaction Engine"),
                    Text.literal(String.format("§f%,d / %,d Fuel", this.handler.getFuelLevel(), this.handler.getMaxFuel())),
                    Text.literal("§7Powers high-tonnage road compaction roller"),
                    Text.literal("§8Accepts Gasoline, Biofuel, High-Octane, or Coal")
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.focusedSlot != null && !this.focusedSlot.hasStack() && this.focusedSlot.id < 14) {
            if (this.focusedSlot.id < 9) {
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§7Asphalt Road Deck Slot"),
                        Text.literal("§8Insert Asphalt Blocks, Slabs, Curbs, or Clay")
                ), mouseX, mouseY);
            } else if (this.focusedSlot.id < 12) {
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§bStructural Support Pier Slot"),
                        Text.literal("§8Insert Stone Bricks, Cobble, Deepslate, or Concrete"),
                        Text.literal("§7Cast as bridge piers when crossing canyons/water")
                ), mouseX, mouseY);
            } else if (this.focusedSlot.id == 12) {
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§eBattery Recharging Slot"),
                        Text.literal("§8Place Copper, Aluminum, Titanium, or Tungsten Battery")
                ), mouseX, mouseY);
            } else if (this.focusedSlot.id == 13) {
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§6Engine Fuel Canister Slot"),
                        Text.literal("§8Place Gasoline, Biofuel, High-Octane Canister, or Coal")
                ), mouseX, mouseY);
            }
        }
    }
}
