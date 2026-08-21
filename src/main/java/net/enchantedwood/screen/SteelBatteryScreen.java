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
public class SteelBatteryScreen extends HandledScreen<SteelBatteryScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/steel_battery_gui.png");

    public SteelBatteryScreen(SteelBatteryScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // Draw Horizontal Battery Gauge (width = 100px, at x + 38, y + 36)
        int energyWidth = this.handler.getScaledEnergy(100);
        if (energyWidth > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 38, y + 36, 0.0f, 166.0f, energyWidth, 18, 256, 256);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        // Battery Gauge Tooltip
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        if (mouseX >= x + 37 && mouseX <= x + 139 && mouseY >= y + 35 && mouseY <= y + 55) {
            String energyText = String.format("%,d / %,d FE", this.handler.getEnergy(), this.handler.getMaxEnergy());
            String rateText = String.format("Max Transfer: %,d FE/t", this.handler.getMaxTransfer());
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§3Tier 3 Steel Energy Cell"),
                    Text.literal("§b" + energyText),
                    Text.literal("§7" + rateText)
            ), mouseX, mouseY);
        }
    }
}
