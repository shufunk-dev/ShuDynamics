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

        // 4. Geothermal Reactor Core Animation (x + 94..116, y + 21..68)
        long time = System.currentTimeMillis();
        if (this.handler.isBurning()) {
            // Rising convective magma heat sparks in core monitor
            for (int i = 0; i < 4; i++) {
                int sparkOffset = (int) ((time / (40 + i * 15) + i * 11) % 44);
                int py = y + 66 - sparkOffset;
                int px = x + 97 + (int) (Math.sin(time * 0.005 + i * 2) * 6 + 6);
                int color = (i % 2 == 0) ? 0xFFFF7700 : 0xFFFFD700;
                context.fill(px, py, px + 2, py + 2, color);
            }
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Machine Title in illuminated warm ivory without colliding with gear slot
        context.drawText(this.textRenderer, Text.literal("Geothermal Gen (Tier 4)"), this.titleX, this.titleY, 0xFFE2DDD2, false);

        // Inventory label in muted lavender slate
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0xFFB8A8C8, false);

        // --- CENTRAL CORE DIAGNOSTIC MONITOR TELEMETRY (x = 95, y = 23) ---
        boolean burning = this.handler.isBurning();
        if (burning) {
            context.drawText(this.textRenderer, Text.literal("§a● CORE"), 95, 23, 0xFF40FF90, false);
            context.drawText(this.textRenderer, Text.literal("§c♨ MAX"), 95, 33, 0xFFFF4444, false);
        } else {
            context.drawText(this.textRenderer, Text.literal("§7○ IDLE"), 95, 23, 0xFF708090, false);
            context.drawText(this.textRenderer, Text.literal("§8♨ COOL"), 95, 33, 0xFF666666, false);
        }

        int lavaPct = (int) (((long) this.handler.getLavaAmount() * 100) / 10000);
        context.drawText(this.textRenderer, Text.literal(String.format("§6%d%%", lavaPct)), 95, 45, 0xFFFF9900, false);

        int maxEnergy = this.handler.getMaxEnergy();
        int energyPct = maxEnergy > 0 ? (int) (((long) this.handler.getEnergy() * 100) / maxEnergy) : 0;
        context.drawText(this.textRenderer, Text.literal(String.format("§e%d%%", energyPct)), 95, 57, 0xFFFFD700, false);

        // --- AUXILIARY OVERCLOCK SUB-PANEL TELEMETRY (x = 142, y = 32) ---
        context.drawText(this.textRenderer, Text.literal("§dO/CLK"), 142, 32, 0xFFE040FB, false);
        context.drawText(this.textRenderer, Text.literal(String.format("§f%.2fx", this.handler.getGearMultiplier())), 142, 42, 0xFFFFFFFF, false);

        int rate = Math.round(750 * this.handler.getGearMultiplier());
        if (burning) {
            context.drawText(this.textRenderer, Text.literal(String.format("§a+%d", rate)), 142, 52, 0xFF40FF90, false);
        } else {
            context.drawText(this.textRenderer, Text.literal("§8+0"), 142, 52, 0xFF666666, false);
        }
        context.drawText(this.textRenderer, Text.literal("§7FE/t"), 142, 61, 0xFF888888, false);
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

        // Central Core Diagnostic Monitor Tooltip
        if (mouseX >= x + 92 && mouseX <= x + 118 && mouseY >= y + 18 && mouseY <= y + 72) {
            int rate = Math.round(750 * this.handler.getGearMultiplier());
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§c🌋 Geothermal Core Diagnostics"),
                    Text.literal(this.handler.isBurning() ? "§aThermal Turbine: ACTIVE" : "§7Thermal Turbine: OFFLINE"),
                    Text.literal(String.format("§eCurrent Rate: +%d FE/t", this.handler.isBurning() ? rate : 0)),
                    Text.literal(String.format("§6Lava Reserve: %,d mB", this.handler.getLavaAmount())),
                    Text.literal(String.format("§bBuffer: %,d / %,d FE", this.handler.getEnergy(), this.handler.getMaxEnergy()))
            ), mouseX, mouseY);
        }

        // Auxiliary Overclock Sub-Panel Tooltip
        if (mouseX >= x + 139 && mouseX <= x + 171 && mouseY >= y + 28 && mouseY <= y + 72) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§d⚙ Turbine Overclock Telemetry"),
                    Text.literal(String.format("§7Installed Gear: §f%s", this.handler.getGearTier().name())),
                    Text.literal(String.format("§aOutput Multiplier: §f%.2fx", this.handler.getGearMultiplier())),
                    Text.literal(String.format("§eEffective Capacity: §f+%,d FE/t", Math.round(750 * this.handler.getGearMultiplier())))
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

        // Gear Slot Tooltip (when installed)
        if (mouseX >= x + 151 && mouseX <= x + 169 && mouseY >= y + 7 && mouseY <= y + 25 && this.handler.getGearTier() != net.enchantedwood.block.custom.GearTier.NONE) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§d⚙ Installed Gear Upgrade"),
                    Text.literal(String.format("§7Current: §f%s (x%.2f Multiplier)", this.handler.getGearTier().name(), this.handler.getGearMultiplier()))
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.focusedSlot != null && !this.focusedSlot.hasStack() && this.focusedSlot.id < 3) {
            switch (this.focusedSlot.id) {
                case 0 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§e📥 Thermal Fuel Input"),
                        Text.literal("§7Insert thermal power sources:"),
                        Text.literal("§f• Lava Buckets, Magma Blocks, Fire Crystals")
                ), mouseX, mouseY);
                case 1 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§b🪣 Empty Bucket Output"),
                        Text.literal("§7Emptied lava containers appear here.")
                ), mouseX, mouseY);
                case 2 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§d⚙️ Gear Upgrade Slot"),
                        Text.literal("§7Insert a Gear or Blaze Overclock Core:"),
                        Text.literal("§f• Dramatically boosts geothermal FE/t generation.")
                ), mouseX, mouseY);
            }
        }
    }
}
