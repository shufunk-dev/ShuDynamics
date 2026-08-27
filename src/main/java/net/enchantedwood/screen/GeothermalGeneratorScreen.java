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
public class GeothermalGeneratorScreen extends HandledScreen<GeothermalGeneratorScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/geothermal_generator_gui.png");

    public GeothermalGeneratorScreen(GeothermalGeneratorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // 1. Lava Tank (width = 16, height = 50, at x + 18, y + 20)
        int lavaHeight = this.handler.getScaledLava(50);
        if (lavaHeight > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 18, y + 70 - lavaHeight, 212.0f, 50.0f - lavaHeight, 16, lavaHeight, 256, 256);
        }

        // 2. Burning Flame (at x + 76, y + 36)
        if (this.handler.isBurning()) {
            int fuelHeight = this.handler.getScaledFuelProgress(14);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 76, y + 50 - fuelHeight, 176.0f, 14.0f - fuelHeight, 14, fuelHeight + 1, 256, 256);
        }

        // 3. Energy Bar (width = 16, height = 50, at x + 121, y + 20)
        int energyHeight = this.handler.getScaledEnergy(50);
        if (energyHeight > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 121, y + 70 - energyHeight, 192.0f, 50.0f - energyHeight, 16, energyHeight, 256, 256);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 4210752, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 4210752, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Lava Tank Tooltip
        if (mouseX >= x + 17 && mouseX <= x + 35 && mouseY >= y + 19 && mouseY <= y + 71) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§c🌋 Lava Buffer Tank"),
                    Text.literal(String.format("§6%,d / 10,000 mB", this.handler.getLavaAmount())),
                    Text.literal("§7Accepts Lava Buckets, Magma Blocks, Fire Crystals, or Pumps")
            ), mouseX, mouseY);
        }

        // Energy Bar Tooltip
        if (mouseX >= x + 120 && mouseX <= x + 138 && mouseY >= y + 19 && mouseY <= y + 71) {
            int rate = Math.round(750 * this.handler.getGearMultiplier());
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§6⚡ Energy Buffer"),
                    Text.literal(String.format("§e%,d / %,d FE", this.handler.getEnergy(), this.handler.getMaxEnergy())),
                    Text.literal(String.format("§aGeneration: +%d FE/t (%s Gear)", rate, this.handler.getGearTier().name()))
            ), mouseX, mouseY);
        }

        // Gear Slot Tooltip
        if (mouseX >= x + 151 && mouseX <= x + 169 && mouseY >= y + 7 && mouseY <= y + 25) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§d⚙ Gear Upgrade Slot"),
                    Text.literal(String.format("§7Current: §f%s (x%.2f Multiplier)", this.handler.getGearTier().name(), this.handler.getGearMultiplier()))
            ), mouseX, mouseY);
        }
    }
}
