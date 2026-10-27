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
public class SteelGeneratorScreen extends AbstractContainerScreen<SteelGeneratorScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/steel_generator_gui.png");

    public SteelGeneratorScreen(SteelGeneratorScreenHandler handler, Inventory inventory, Component title) {
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

        // 1. Draw burning flame
        if (this.menu.isBurning()) {
            int fuelHeight = this.menu.getScaledFuelProgress(14);
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 81, y + 49 - fuelHeight, 176.0f, 14.0f - fuelHeight, 14, fuelHeight + 1, 256, 256);
        }

        // 2. Draw Energy Bar (height = 50px, at x + 138, y + 20)
        int energyHeight = this.menu.getScaledEnergy(50);
        if (energyHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 138, y + 70 - energyHeight, 192.0f, 50.0f - energyHeight, 16, energyHeight, 256, 256);
        }

        // 3. Dynamic Power Wave on Status Screen (x + 18..64, y + 63)
        if (this.menu.isBurning()) {
            long time = System.currentTimeMillis();
            int baseY = y + 64;
            for (int col = 0; col < 46; col += 2) {
                int wave = (int) Math.round(Math.sin((col * 0.35) + (time * 0.012)) * 2.5);
                int py = baseY + wave;
                context.fill(x + 19 + col, py, x + 19 + col + 2, py + 1, 0xFFFFD700);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        // Machine Title in illuminated warm ivory
        context.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFE2DDD2, false);

        // Inventory label in dark slate
        context.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF373A40, false);

        // --- DIGITAL STATUS MONITOR TELEMETRY (x = 18, y = 23) ---
        boolean burning = this.menu.isBurning();
        if (burning) {
            context.text(this.font, Component.literal("§6● GEN ON"), 18, 23, 0xFFFFD700, false);
        } else {
            context.text(this.font, Component.literal("§7○ STANDBY"), 18, 23, 0xFF708090, false);
        }

        String rateStr = burning ? String.format("§e+%,d §7FE/t", this.menu.getGenerationRate()) : "§80 FE/t";
        context.text(this.font, Component.literal(rateStr), 18, 33, 0xFFFFCC00, false);

        if (burning) {
            int fuelPct = this.menu.getScaledFuelProgress(100);
            context.text(this.font, Component.literal("§6♨ §f" + fuelPct + "%"), 18, 43, 0xFFFFA040, false);
        } else {
            context.text(this.font, Component.literal("§8WAIT FUEL"), 18, 43, 0xFF555555, false);
        }

        int max = this.menu.getMaxEnergy();
        int pct = max > 0 ? (int)(((long)this.menu.getEnergy() * 100) / max) : 0;
        context.text(this.font, Component.literal("§b🔋 §f" + pct + "%"), 18, 53, 0xFF64B5F6, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Status Screen Tooltip
        if (mouseX >= x + 13 && mouseX <= x + 70 && mouseY >= y + 18 && mouseY <= y + 72) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§b📟 Generator Diagnostic Monitor"),
                    Component.literal(this.menu.isBurning() ? "§aStatus: Heavy Induction Power Active" : "§7Status: Standby (Awaiting Fuel)"),
                    Component.literal(String.format("§eOutput Rate: +%,d FE/t", this.menu.getGenerationRate())),
                    Component.literal(String.format("§6Fuel Burn: %d%%", this.menu.getScaledFuelProgress(100))),
                    Component.literal(String.format("§bInternal Buffer: %,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy()))
            ), mouseX, mouseY);
        }

        // Energy Bar Tooltip
        if (mouseX >= x + 137 && mouseX <= x + 155 && mouseY >= y + 19 && mouseY <= y + 71) {
            String energyText = String.format("%,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy());
            String rateText = String.format("Output: +%,d FE/t", this.menu.getGenerationRate());
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§6⚡ Heavy Energy Buffer"),
                    Component.literal("§e" + energyText),
                    Component.literal("§a" + rateText)
            ), mouseX, mouseY);
        }

        // Empty Fuel Slot Tooltip
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index == 0) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§6🔥 Solid Fuel Slot"),
                    Component.literal("§7Insert combustible fuel:"),
                    Component.literal("§f• Coal, Charcoal, Coke Coal, Wood, Fire Crystal")
            ), mouseX, mouseY);
        }
    }
}
