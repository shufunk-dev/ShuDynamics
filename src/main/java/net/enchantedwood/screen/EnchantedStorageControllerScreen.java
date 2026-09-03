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

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class EnchantedStorageControllerScreen extends HandledScreen<EnchantedStorageControllerScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/enchanted_storage_controller_gui.png");

    public EnchantedStorageControllerScreen(EnchantedStorageControllerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        this.titleY = 6;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // 1. Draw burning flame icon if backup fuel is burning (above center fuel slot)
        if (this.handler.isFuelPowered()) {
            int fuelHeight = this.handler.getBurnProgressScaled(14);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 81, y + 45 - fuelHeight, 176.0f, 14.0f - fuelHeight, 14, fuelHeight + 1, 256, 256);
        }

        // 2. Draw Vertical Power Gauge (50px height at x + 150, y + 20)
        int energyHeight = this.handler.getScaledEnergy(50);
        if (energyHeight > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 150, y + 70 - energyHeight, 192.0f, 50.0f - energyHeight, 12, energyHeight, 256, 256);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);

        if (this.handler.isGridPowered()) {
            context.drawText(this.textRenderer, Text.literal("⚡ ONLINE (Grid)").formatted(net.minecraft.util.Formatting.GREEN, net.minecraft.util.Formatting.BOLD), 18, 20, 0x55FF55, false);
            String energyText = String.format("%,d FE", this.handler.getEnergy());
            context.drawText(this.textRenderer, Text.literal(energyText).formatted(net.minecraft.util.Formatting.YELLOW), 18, 32, 0xFFFF55, false);
        } else if (this.handler.isFuelPowered()) {
            int totalSeconds = this.handler.getBurnTime() / 20;
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            String timeText = String.format("%dm %02ds backup", minutes, seconds);

            context.drawText(this.textRenderer, Text.literal("⚡ ONLINE (Fuel)").formatted(net.minecraft.util.Formatting.AQUA, net.minecraft.util.Formatting.BOLD), 18, 20, 0x55FFFF, false);
            context.drawText(this.textRenderer, Text.literal(timeText).formatted(net.minecraft.util.Formatting.GOLD), 18, 32, 0xFFAA00, false);
        } else {
            context.drawText(this.textRenderer, Text.literal("❌ OFFLINE").formatted(net.minecraft.util.Formatting.RED, net.minecraft.util.Formatting.BOLD), 18, 20, 0xFF5555, false);
            context.drawText(this.textRenderer, Text.literal("Grid or Fuel needed").formatted(net.minecraft.util.Formatting.GRAY), 18, 32, 0xAAAAAA, false);
        }

        // Status indicators on upgrade slots
        if (this.handler.hasChunkLoader()) {
            context.drawText(this.textRenderer, Text.literal("●").formatted(net.minecraft.util.Formatting.GREEN), 39, 68, 0x55FF55, false);
        }
        if (this.handler.hasInterdimensionalCard()) {
            context.drawText(this.textRenderer, Text.literal("●").formatted(net.minecraft.util.Formatting.DARK_PURPLE), 127, 68, 0xAA00AA, false);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Tooltip over Vertical Power Bar (x + 149 .. 163, y + 19 .. 71)
        if (mouseX >= x + 149 && mouseX <= x + 163 && mouseY >= y + 19 && mouseY <= y + 71) {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("§6⚡ Controller Power Status"));
            lines.add(Text.literal("§eGrid Energy: §f" + String.format("%,d / %,d FE", this.handler.getEnergy(), this.handler.getMaxEnergy())));

            if (this.handler.isFuelPowered()) {
                int totalSeconds = this.handler.getBurnTime() / 20;
                int minutes = totalSeconds / 60;
                int seconds = totalSeconds % 60;
                lines.add(Text.literal("§bEmergency Fuel: §f" + minutes + "m " + seconds + "s"));
                lines.add(Text.literal("§aStatus: Running on Emergency Fuel"));
            } else if (this.handler.isGridPowered()) {
                lines.add(Text.literal("§aStatus: Running on Grid Power (10 FE/t)"));
            } else {
                lines.add(Text.literal("§cStatus: Offline (Connect Cables or insert Fuel)"));
            }

            context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        }

        // Tooltip over Slot 1: Chunk Loader Slot (x + 35 .. 53, y + 47 .. 65)
        if (mouseX >= x + 35 && mouseX <= x + 53 && mouseY >= y + 47 && mouseY <= y + 65) {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("§b⚓ Chunk Loader Module Slot"));
            if (this.handler.hasChunkLoader()) {
                lines.add(Text.literal("§a● Status: ACTIVE (Base Chunk Loaded 24/7)"));
                lines.add(Text.literal("§7Enables infinite Overworld wireless access."));
            } else {
                lines.add(Text.literal("§7○ Status: EMPTY"));
                lines.add(Text.literal("§8Insert a Chunk Loader Module to keep base loaded."));
            }
            context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        }

        // Tooltip over Slot 0: Backup Fuel Slot (x + 79 .. 97, y + 47 .. 65)
        if (mouseX >= x + 79 && mouseX <= x + 97 && mouseY >= y + 47 && mouseY <= y + 65) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§d🔥 Emergency Backup Fuel Slot"),
                    Text.literal("§7Accepts: Enchanted Coal Block or Lava"),
                    Text.literal("§8Used automatically when Grid FE runs out.")
            ), mouseX, mouseY);
        }

        // Tooltip over Slot 2: Interdimensional Card Slot (x + 123 .. 141, y + 47 .. 65)
        if (mouseX >= x + 123 && mouseX <= x + 141 && mouseY >= y + 47 && mouseY <= y + 65) {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("§5🌌 Interdimensional Card Slot"));
            if (this.handler.hasInterdimensionalCard()) {
                lines.add(Text.literal("§5● Status: ACTIVE (Cross-Dimension Link Active)"));
                lines.add(Text.literal("§7Enables remote quarry & wireless access from:"));
                lines.add(Text.literal("§d✦ Nether, The End & Mining Dimension!"));
                lines.add(Text.literal("§a✨ Base 3x3 chunk area kept loaded 24/7."));
            } else {
                lines.add(Text.literal("§7○ Status: READY"));
                lines.add(Text.literal("§8Insert Interdimensional Card to access across dimensions."));
            }
            context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        }
    }
}
