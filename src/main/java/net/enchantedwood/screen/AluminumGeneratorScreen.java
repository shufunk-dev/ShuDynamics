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
public class AluminumGeneratorScreen extends HandledScreen<AluminumGeneratorScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/aluminum_generator_gui.png");

    public AluminumGeneratorScreen(AluminumGeneratorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        this.titleX = 8;
        this.titleY = 7;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
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

        // 3. Digital Generator Status Screen / Monitor (x + 14, y + 19, w: 54, h: 52)
        context.fill(x + 14, y + 19, x + 68, y + 71, 0xFF141923);
        context.fill(x + 15, y + 20, x + 67, y + 70, 0xFF080D12);

        // Dynamic sine wave power line (Cyan for Aluminum)
        long time = System.currentTimeMillis();
        for (int px = 0; px < 50; px++) {
            double wave = this.handler.isBurning() ? Math.sin((time / 130.0) + (px * 0.4)) * 2.4 : 0;
            int py = y + 40 + (int) wave;
            int waveColor = this.handler.isBurning() ? 0xFF00E5FF : 0xFF1B3844;
            context.fill(x + 16 + px, py, x + 17 + px, py + 1, waveColor);
        }

        // Monitor Telemetry Readouts
        if (this.handler.isBurning()) {
            context.drawText(this.textRenderer, Text.literal("§b● GEN ON"), x + 17, y + 23, 0xFF00E5FF, false);
            context.drawText(this.textRenderer, Text.literal("§b+" + this.handler.getGenerationRate() + " FE/t"), x + 17, y + 31, 0xFF00FFCC, false);
            int fuelPct = this.handler.getTotalBurnTime() > 0 ? (int) ((long) this.handler.getBurnTime() * 100 / this.handler.getTotalBurnTime()) : 0;
            context.drawText(this.textRenderer, Text.literal("§e♨ " + fuelPct + "%"), x + 17, y + 45, 0xFFFFCC00, false);
        } else {
            context.drawText(this.textRenderer, Text.literal("§8○ STANDBY"), x + 17, y + 23, 0xFF666666, false);
            context.drawText(this.textRenderer, Text.literal("§80 FE/t"), x + 17, y + 31, 0xFF555555, false);
            context.drawText(this.textRenderer, Text.literal("§8♨ 0%"), x + 17, y + 45, 0xFF555555, false);
        }

        int buffPct = this.handler.getMaxEnergy() > 0 ? (int) ((long) this.handler.getEnergy() * 100 / this.handler.getMaxEnergy()) : 0;
        context.drawText(this.textRenderer, Text.literal("§3🔋 " + buffPct + "%"), x + 17, y + 54, 0xFF00E5FF, false);

        // Bottom micro groove progress bar
        int barY = y + 64;
        context.fill(x + 16, barY, x + 66, barY + 2, 0xFF101922);
        int fillW = Math.max(0, Math.min(50, (buffPct * 50) / 100));
        if (fillW > 0) {
            context.fill(x + 16, barY, x + 16 + fillW, barY + 2, 0xFF00E5FF);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0xFFE2DDD2, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0xFF373A40, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Energy Bar Tooltip
        if (mouseX >= x + 137 && mouseX <= x + 155 && mouseY >= y + 19 && mouseY <= y + 71) {
            String energyText = String.format("%,d / %,d FE", this.handler.getEnergy(), this.handler.getMaxEnergy());
            String rateText = String.format("Output: +%d FE/t", this.handler.getGenerationRate());
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§6⚡ Energy Buffer"),
                    Text.literal("§e" + energyText),
                    Text.literal("§a" + rateText)
            ), mouseX, mouseY);
        }

        // Digital Monitor Hover Tooltip (x + 14 .. 68, y + 19 .. 71)
        if (mouseX >= x + 14 && mouseX <= x + 68 && mouseY >= y + 19 && mouseY <= y + 71) {
            int buffPct = this.handler.getMaxEnergy() > 0 ? (int) ((long) this.handler.getEnergy() * 100 / this.handler.getMaxEnergy()) : 0;
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§b⚡ Aluminum Generator (Tier 2) Telemetry"),
                    Text.literal("§8──────────────────────────────────"),
                    Text.literal("§7Operational State: " + (this.handler.isBurning() ? "§a● Generating Power" : "§8○ Standby (No Fuel)")),
                    Text.literal("§7Generation Rate:   §b+" + this.handler.getGenerationRate() + " FE/t"),
                    Text.literal("§7Fuel Burn Remaining:§e " + (this.handler.isBurning() ? String.format("%d ticks (~%.1fs)", this.handler.getBurnTime(), this.handler.getBurnTime() / 20.0f) : "None")),
                    Text.literal("§7Energy Buffer:     §f" + String.format("%,d / %,d FE (%d%%)", this.handler.getEnergy(), this.handler.getMaxEnergy(), buffPct)),
                    Text.literal("§8Advanced combustion chamber yields 5× output over Tier 1.")
            ), mouseX, mouseY);
        }
    }
}
