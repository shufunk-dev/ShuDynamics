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
public class FuelRefineryScreen extends HandledScreen<FuelRefineryScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/fuel_refinery_gui.png");

    public FuelRefineryScreen(FuelRefineryScreenHandler handler, PlayerInventory inventory, Text title) {
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

        // 2. Refining Progress Arrow (x + 74, y + 34, width 24, height 17)
        int progressWidth = this.handler.getScaledProgress();
        if (progressWidth > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 74, y + 34, 176.0f, 36.0f, progressWidth, 17, 256, 256);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Energy Tooltip
        if (mouseX >= x + 12 && mouseX <= x + 26 && mouseY >= y + 14 && mouseY <= y + 52) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§e⚡ Energy Storage"),
                    Text.literal(String.format("§f%,d / %,d FE", this.handler.getEnergy(), this.handler.getMaxEnergy())),
                    Text.literal("§7Consumes 20 FE/t while refining")
            ), mouseX, mouseY);
        }
    }
}
