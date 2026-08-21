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
public class CopperGeneratorScreen extends HandledScreen<CopperGeneratorScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/copper_generator_gui.png");

    public CopperGeneratorScreen(CopperGeneratorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // 1. Draw burning flame
        if (this.handler.isBurning()) {
            int fuelHeight = this.handler.getScaledFuelProgress(14);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 81, y + 49 - fuelHeight, 176.0f, 14.0f - fuelHeight, 14, fuelHeight + 1, 256, 256);
        }

        // 2. Draw Energy Bar (height = 50px, at x + 138, y + 20)
        int energyHeight = this.handler.getScaledEnergy(50);
        if (energyHeight > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 138, y + 70 - energyHeight, 192.0f, 50.0f - energyHeight, 16, energyHeight, 256, 256);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        // Energy Bar Tooltip
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        if (mouseX >= x + 137 && mouseX <= x + 155 && mouseY >= y + 19 && mouseY <= y + 71) {
            String energyText = String.format("%,d / %,d FE", this.handler.getEnergy(), this.handler.getMaxEnergy());
            String rateText = String.format("Output: +%d FE/t", this.handler.getGenerationRate());
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§6Energy Buffer"),
                    Text.literal("§e" + energyText),
                    Text.literal("§a" + rateText)
            ), mouseX, mouseY);
        }
    }
}
