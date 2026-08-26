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

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // 1. Energy Bar (x + 13, y + 15, width 12, height 36)
        int energyHeight = this.handler.getScaledEnergy();
        if (energyHeight > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 13, y + 51 - energyHeight, 176.0f, 36.0f - energyHeight, 12, energyHeight, 256, 256);
        }

        // 2. Fuel Gauge Bar (x + 149, y + 15, width 14, height 36)
        int fuelHeight = this.handler.getScaledFuel();
        if (fuelHeight > 0) {
            context.fillGradient(x + 149, y + 51 - fuelHeight, x + 163, y + 51, 0xFFFF8800, 0xFFCC3300);
        }

        // 3. Section Labels
        context.drawText(this.textRenderer, Text.literal("§7Bat"), x + 10, y + 43, 0x888888, false);
        context.drawText(this.textRenderer, Text.literal("§7Gas"), x + 148, y + 43, 0x888888, false);

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

        // Energy Tooltip (Left)
        if (mouseX >= x + 12 && mouseX <= x + 26 && mouseY >= y + 14 && mouseY <= y + 52) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§e⚡ Electrical Guidance System"),
                    Text.literal(String.format("§f%,d / %,d FE", this.handler.getEnergy(), this.handler.getMaxEnergy())),
                    Text.literal("§7Draws 50 FE per 3-block row"),
                    Text.literal("§8Powers onboard GPS & laser leveling")
            ), mouseX, mouseY);
        }

        // Fuel Tooltip (Right)
        if (mouseX >= x + 147 && mouseX <= x + 165 && mouseY >= y + 14 && mouseY <= y + 52) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§6🔥 Engine Combustion Fuel"),
                    Text.literal(String.format("§f%,d / %,d Fuel", this.handler.getFuelLevel(), this.handler.getMaxFuel())),
                    Text.literal("§7Accepts Gasoline, Biofuel, High-Octane, or Coal"),
                    Text.literal("§8Powers heavy compaction roller")
            ), mouseX, mouseY);
        }
    }
}
