package net.enchantedwood.screen;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.entity.custom.AtvEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class AtvScreen extends HandledScreen<AtvScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/atv_gui.png");

    public AtvScreen(AtvScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // Header and Gauges
        if (this.handler.getAtvInventory() instanceof AtvEntity atv) {
            float speed = atv.getDisplaySpeed();
            int fuel = atv.getFuelLevel();
            int maxFuel = atv.getMaxFuel();
            int fuelPct = maxFuel > 0 ? (fuel * 100 / maxFuel) : 0;

            String speedStr = String.format("🏎️ %.0f km/h", Math.abs(speed));
            String fuelStr = String.format("⛽ %d%%", fuelPct);

            context.drawText(this.textRenderer, Text.literal(speedStr), x + 60, y + 6, 0x00FFFF, false);
            context.drawText(this.textRenderer, Text.literal(fuelStr), x + 120, y + 6, fuelPct > 20 ? 0x55FF55 : 0xFF5555, false);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
