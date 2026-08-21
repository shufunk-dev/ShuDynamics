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

@Environment(EnvType.CLIENT)
public class EnchantedLavaGeneratorScreen extends HandledScreen<EnchantedLavaGeneratorScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/lava_generator_gui.png");

    public EnchantedLavaGeneratorScreen(EnchantedLavaGeneratorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // Draw Fuel Flame
        if (this.handler.isBurning()) {
            int fuelHeight = this.handler.getScaledFuelProgress();
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 27, y + 49 - fuelHeight, 176.0f, 14.0f - fuelHeight, 14, fuelHeight + 1, 256, 256);
        }

        // Draw Cook Progress Arrow
        int cookWidth = this.handler.getScaledCookProgress();
        if (cookWidth > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 66, y + 19, 176.0f, 14.0f, cookWidth + 1, 17, 256, 256);
        }

        // Draw Lava Fluid Reservoir Gauge (x: 140, y: 17, width: 16, height: 52)
        int lavaHeight = this.handler.getScaledLavaProgress();
        if (lavaHeight > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 140, y + 69 - lavaHeight, 176.0f, 83.0f - lavaHeight, 16, lavaHeight, 256, 256);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        // Custom Tooltip for Lava Gauge when hovered (x: 140, y: 17, width: 16, height: 52)
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        if (mouseX >= x + 140 && mouseX <= x + 156 && mouseY >= y + 17 && mouseY <= y + 69) {
            int lava = this.handler.getLavaAmount();
            int buckets = lava / 1000;
            context.drawTooltip(this.textRenderer, Text.literal("Lava Gauge: " + lava + " / 10,000 mL (" + buckets + " Bucket" + (buckets == 1 ? "" : "s") + ")"), mouseX, mouseY);
        }
    }
}
