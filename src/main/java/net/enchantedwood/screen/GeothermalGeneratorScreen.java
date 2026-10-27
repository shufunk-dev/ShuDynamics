package net.enchantedwood.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.enchantedwood.EnchantedWoodMod;

import java.util.List;

@Environment(EnvType.CLIENT)
public class GeothermalGeneratorScreen extends AbstractContainerScreen<GeothermalGeneratorScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/geothermal_generator_gui.png");

    public GeothermalGeneratorScreen(GeothermalGeneratorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.titleLabelX = 8;
        this.titleLabelY = 7;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // 1. Lava Tank (width = 16, height = 50, at x + 18, y + 20)
        int lavaHeight = this.menu.getScaledLava(50);
        if (lavaHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 18, y + 70 - lavaHeight, 212.0f, 50.0f - lavaHeight, 16, lavaHeight, 256, 256);
        }

        // 2. Burning Flame (at x + 76, y + 36)
        if (this.menu.isBurning()) {
            int fuelHeight = this.menu.getScaledFuelProgress(14);
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 76, y + 50 - fuelHeight, 176.0f, 14.0f - fuelHeight, 14, fuelHeight + 1, 256, 256);
        }

        // 3. Energy Bar (width = 16, height = 50, at x + 121, y + 20)
        int energyHeight = this.menu.getScaledEnergy(50);
        if (energyHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 121, y + 70 - energyHeight, 192.0f, 50.0f - energyHeight, 16, energyHeight, 256, 256);
        }

        // 4. Geothermal Reactor Core Animation (x + 94..116, y + 21..68)
        long time = System.currentTimeMillis();
        if (this.menu.isBurning()) {
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
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        // Machine Title in illuminated warm ivory without colliding with gear slot
        context.text(this.font, Component.literal("Geothermal Gen (Tier 4)"), this.titleLabelX, this.titleLabelY, 0xFFE2DDD2, false);

        // Inventory label in muted lavender slate
        context.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFB8A8C8, false);

        // --- CENTRAL CORE DIAGNOSTIC MONITOR TELEMETRY (x = 95, y = 23) ---
        boolean burning = this.menu.isBurning();
        if (burning) {
            context.text(this.font, Component.literal("§a● CORE"), 95, 23, 0xFF40FF90, false);
            context.text(this.font, Component.literal("§c♨ MAX"), 95, 33, 0xFFFF4444, false);
        } else {
            context.text(this.font, Component.literal("§7○ IDLE"), 95, 23, 0xFF708090, false);
            context.text(this.font, Component.literal("§8♨ COOL"), 95, 33, 0xFF666666, false);
        }

        int lavaPct = (int)(((long)this.menu.getLavaAmount() * 100) / 10000);
        context.text(this.font, Component.literal(String.format("§6%d%%", lavaPct)), 95, 45, 0xFFFF9900, false);

        int maxEnergy = this.menu.getMaxEnergy();
        int energyPct = maxEnergy > 0 ? (int)(((long)this.menu.getEnergy() * 100) / maxEnergy) : 0;
        context.text(this.font, Component.literal(String.format("§e%d%%", energyPct)), 95, 57, 0xFFFFD700, false);

        // --- AUXILIARY OVERCLOCK SUB-PANEL TELEMETRY (x = 142, y = 32) ---
        context.text(this.font, Component.literal("§dO/CLK"), 142, 32, 0xFFE040FB, false);
        context.text(this.font, Component.literal(String.format("§f%.2fx", this.menu.getGearMultiplier())), 142, 42, 0xFFFFFFFF, false);

        int rate = Math.round(750 * this.menu.getGearMultiplier());
        if (burning) {
            context.text(this.font, Component.literal(String.format("§a+%d", rate)), 142, 52, 0xFF40FF90, false);
        } else {
            context.text(this.font, Component.literal("§8+0"), 142, 52, 0xFF666666, false);
        }
        context.text(this.font, Component.literal("§7FE/t"), 142, 61, 0xFF888888, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Lava Tank Tooltip
        if (mouseX >= x + 17 && mouseX <= x + 35 && mouseY >= y + 19 && mouseY <= y + 71) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§c🌋 Lava Buffer Tank"),
                    Component.literal(String.format("§6%,d / 10,000 mB", this.menu.getLavaAmount())),
                    Component.literal("§7Accepts Lava Buckets, Magma Blocks, Fire Crystals, or Pumps")
            ), mouseX, mouseY);
        }

        // Central Core Diagnostic Monitor Tooltip
        if (mouseX >= x + 92 && mouseX <= x + 118 && mouseY >= y + 18 && mouseY <= y + 72) {
            int rate = Math.round(750 * this.menu.getGearMultiplier());
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§c🌋 Geothermal Core Diagnostics"),
                    Component.literal(this.menu.isBurning() ? "§aThermal Turbine: ACTIVE" : "§7Thermal Turbine: OFFLINE"),
                    Component.literal(String.format("§eCurrent Rate: +%d FE/t", this.menu.isBurning() ? rate : 0)),
                    Component.literal(String.format("§6Lava Reserve: %,d mB", this.menu.getLavaAmount())),
                    Component.literal(String.format("§bBuffer: %,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy()))
            ), mouseX, mouseY);
        }

        // Auxiliary Overclock Sub-Panel Tooltip
        if (mouseX >= x + 139 && mouseX <= x + 171 && mouseY >= y + 28 && mouseY <= y + 72) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§d⚙ Turbine Overclock Telemetry"),
                    Component.literal(String.format("§7Installed Gear: §f%s", this.menu.getGearTier().name())),
                    Component.literal(String.format("§aOutput Multiplier: §f%.2fx", this.menu.getGearMultiplier())),
                    Component.literal(String.format("§eEffective Capacity: §f+%,d FE/t", Math.round(750 * this.menu.getGearMultiplier())))
            ), mouseX, mouseY);
        }

        // Energy Bar Tooltip
        if (mouseX >= x + 120 && mouseX <= x + 138 && mouseY >= y + 19 && mouseY <= y + 71) {
            int rate = Math.round(750 * this.menu.getGearMultiplier());
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§6⚡ Energy Buffer"),
                    Component.literal(String.format("§e%,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy())),
                    Component.literal(String.format("§aGeneration: +%d FE/t (%s Gear)", rate, this.menu.getGearTier().name()))
            ), mouseX, mouseY);
        }

        // Gear Slot Tooltip (when installed)
        if (mouseX >= x + 151 && mouseX <= x + 169 && mouseY >= y + 7 && mouseY <= y + 25 && this.menu.getGearTier() != net.enchantedwood.block.custom.GearTier.NONE) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§d⚙ Installed Gear Upgrade"),
                    Component.literal(String.format("§7Current: §f%s (x%.2f Multiplier)", this.menu.getGearTier().name(), this.menu.getGearMultiplier()))
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 3) {
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e📥 Thermal Fuel Input"),
                        Component.literal("§7Insert thermal power sources:"),
                        Component.literal("§f• Lava Buckets, Magma Blocks, Fire Crystals")
                ), mouseX, mouseY);
                case 1 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§b🪣 Empty Bucket Output"),
                        Component.literal("§7Emptied lava containers appear here.")
                ), mouseX, mouseY);
                case 2 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§d⚙️ Gear Upgrade Slot"),
                        Component.literal("§7Insert a Gear or Blaze Overclock Core:"),
                        Component.literal("§f• Dramatically boosts geothermal FE/t generation.")
                ), mouseX, mouseY);
            }
        }
    }
}
