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
public class CrusherScreen extends HandledScreen<CrusherScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/crusher_gui.png");

    public CrusherScreen(CrusherScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // 1. Cook Progress Arrow (at x + 79, y + 34, width 24, height 17)
        int cookWidth = this.handler.getScaledCookProgress(24);
        if (cookWidth > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 79, y + 34, 176.0f, 14.0f, cookWidth + 1, 16, 256, 256);
        }

        // 2. Energy Bar (at x + 152, y + 20, width 14, height 52)
        int energyHeight = this.handler.getScaledEnergy(52);
        if (energyHeight > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 152, y + 72 - energyHeight, 190.0f, 52.0f - energyHeight, 14, energyHeight, 256, 256);
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
        if (mouseX >= x + 151 && mouseX <= x + 167 && mouseY >= y + 19 && mouseY <= y + 73) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§eEnergy Buffer"),
                    Text.literal(String.format("§6%,d / %,d FE", this.handler.getEnergy(), this.handler.getMaxEnergy())),
                    Text.literal("§7Usage: 40 FE/t")
            ), mouseX, mouseY);
        }
    }
}
