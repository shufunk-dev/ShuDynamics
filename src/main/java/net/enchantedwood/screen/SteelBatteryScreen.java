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

        // 1. Draw Horizontal Battery Gauge (width = 100px, at x + 38, y + 36)
        int energyWidth = this.handler.getScaledEnergy(100);
        if (energyWidth > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 38, y + 36, 0.0f, 166.0f, energyWidth, 18, 256, 256);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 4210752, false);

        String energyStr = String.format("%,d / %,d FE", this.handler.getEnergy(), this.handler.getMaxEnergy());
        int energyStrWidth = this.textRenderer.getWidth(energyStr);
        context.drawText(this.textRenderer, energyStr, (this.backgroundWidth - energyStrWidth) / 2, 24, 0x2A3E52, false);

        String rateStr = String.format("Max I/O: %,d FE/t", this.handler.getMaxTransfer());
        int rateStrWidth = this.textRenderer.getWidth(rateStr);
        context.drawText(this.textRenderer, rateStr, (this.backgroundWidth - rateStrWidth) / 2, 58, 0x5E5E5E, false);

        // Slot indicators
        context.drawText(this.textRenderer, Text.literal("§7IN"), 18, 24, 0x888888, false);
        context.drawText(this.textRenderer, Text.literal("§7OUT"), 143, 24, 0x888888, false);

        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 4210752, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Battery Gauge Tooltip
        if (mouseX >= x + 37 && mouseX <= x + 139 && mouseY >= y + 35 && mouseY <= y + 55) {
            double percent = (double) this.handler.getEnergy() / (double) this.handler.getMaxEnergy() * 100.0;
            String energyText = String.format("%,d / %,d FE (%.1f%%)", this.handler.getEnergy(), this.handler.getMaxEnergy(), percent);
            String rateText = String.format("Max Transfer: %,d FE/t", this.handler.getMaxTransfer());
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§3Tier 3 Steel Energy Cell"),
                    Text.literal("§b" + energyText),
                    Text.literal("§7" + rateText)
            ), mouseX, mouseY);
        }

        // Discharge Slot Tooltip
        if (mouseX >= x + 15 && mouseX <= x + 33 && mouseY >= y + 34 && mouseY <= y + 52) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§6📥 Discharge Slot"),
                    Text.literal("§7Place any battery pack here to drain power into this cell.")
            ), mouseX, mouseY);
        }

        // Charge Slot Tooltip
        if (mouseX >= x + 143 && mouseX <= x + 161 && mouseY >= y + 34 && mouseY <= y + 52) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§b⚡ Charge Slot"),
                    Text.literal("§7Place any battery pack or tool here to rapidly recharge it.")
            ), mouseX, mouseY);
        }
    }
}
