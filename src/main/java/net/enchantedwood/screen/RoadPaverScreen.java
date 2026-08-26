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
public class RoadPaverScreen extends HandledScreen<RoadPaverScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/road_paver_gui.png");

    public RoadPaverScreen(RoadPaverScreenHandler handler, PlayerInventory inventory, Text title) {
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
        // Battery Slot Box at (12, 56) -> frame at (11, 55)
        drawSlotBox(context, x + 11, y + 55);

        // 2. Right: Engine Fuel Gauge Frame & Fill
        drawGaugeFrame(context, x + 148, y + 14, 16, 38);
        int fuelHeight = this.handler.getScaledFuel();
        if (fuelHeight > 0) {
            context.fillGradient(x + 149, y + 51 - fuelHeight, x + 163, y + 51, 0xFFFF6600, 0xFFCC2200);
        }
        // Fuel Slot Box at (148, 56) -> frame at (147, 55)
        drawSlotBox(context, x + 147, y + 55);

        // 3. Section Labels
        context.drawText(this.textRenderer, Text.literal("§e⚡PWR"), x + 10, y + 4, 0x555555, false);
        context.drawText(this.textRenderer, Text.literal("§6🔥GAS"), x + 146, y + 4, 0x555555, false);

        // 4. Active Paving Indicator
        if (this.handler.isPaving()) {
            context.drawText(this.textRenderer, Text.literal("§a▶ PAVING ROAD"), x + 50, y + 74, 0x55FF55, false);
        } else {
            context.drawText(this.textRenderer, Text.literal("§7⏸ IDLE"), x + 72, y + 74, 0x888888, false);
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
                    Text.literal("§7Draws 50 FE per 3-block row"),
                    Text.literal("§8Place battery packs or connect cables to charge")
            ), mouseX, mouseY);
        }

        // Fuel Tooltip (Right Gauge + Fuel Slot)
        if (mouseX >= x + 147 && mouseX <= x + 165 && mouseY >= y + 14 && mouseY <= y + 74) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§6🔥 Engine Combustion Fuel"),
                    Text.literal(String.format("§f%,d / %,d Fuel", this.handler.getFuelLevel(), this.handler.getMaxFuel())),
                    Text.literal("§7Burn time for compaction roller engine"),
                    Text.literal("§8Accepts Gasoline, Biofuel, High-Octane, or Coal")
            ), mouseX, mouseY);
        }
    }
}
