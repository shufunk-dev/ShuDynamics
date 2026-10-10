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

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class EnchantedStorageControllerScreen extends AbstractContainerScreen<EnchantedStorageControllerScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/enchanted_storage_controller_gui.png");

    public EnchantedStorageControllerScreen(EnchantedStorageControllerScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // 1. Draw burning flame icon if backup fuel is burning (above center fuel slot)
        if (this.menu.isFuelPowered()) {
            int fuelHeight = this.menu.getBurnProgressScaled(14);
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 81, y + 45 - fuelHeight, 176.0f, 14.0f - fuelHeight, 14, fuelHeight + 1, 256, 256);
        }

        // 2. Draw Vertical Power Gauge (50px height at x + 150, y + 20)
        int energyHeight = this.menu.getScaledEnergy(50);
        if (energyHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 150, y + 70 - energyHeight, 192.0f, 50.0f - energyHeight, 12, energyHeight, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractLabels(context, mouseX, mouseY);

        if (this.menu.isGridPowered()) {
            context.text(this.font, Component.literal("⚡ ONLINE (Grid)").withStyle(net.minecraft.ChatFormatting.GREEN, net.minecraft.ChatFormatting.BOLD), 18, 20, 0x55FF55, false);
            String energyText = String.format("%,d FE", this.menu.getEnergy());
            context.text(this.font, Component.literal(energyText).withStyle(net.minecraft.ChatFormatting.YELLOW), 18, 32, 0xFFFF55, false);
        } else if (this.menu.isFuelPowered()) {
            int totalSeconds = this.menu.getBurnTime() / 20;
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            String timeText = String.format("%dm %02ds backup", minutes, seconds);

            context.text(this.font, Component.literal("⚡ ONLINE (Fuel)").withStyle(net.minecraft.ChatFormatting.AQUA, net.minecraft.ChatFormatting.BOLD), 18, 20, 0x55FFFF, false);
            context.text(this.font, Component.literal(timeText).withStyle(net.minecraft.ChatFormatting.GOLD), 18, 32, 0xFFAA00, false);
        } else {
            context.text(this.font, Component.literal("❌ OFFLINE").withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD), 18, 20, 0xFF5555, false);
            context.text(this.font, Component.literal("Grid or Fuel needed").withStyle(net.minecraft.ChatFormatting.GRAY), 18, 32, 0xAAAAAA, false);
        }

        // Status indicators on upgrade slots
        if (this.menu.hasChunkLoader()) {
            context.text(this.font, Component.literal("●").withStyle(net.minecraft.ChatFormatting.GREEN), 39, 68, 0x55FF55, false);
        }
        if (this.menu.hasInterdimensionalCard()) {
            context.text(this.font, Component.literal("●").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE), 127, 68, 0xAA00AA, false);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);


        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Tooltip over Vertical Power Bar (x + 149 .. 163, y + 19 .. 71)
        if (mouseX >= x + 149 && mouseX <= x + 163 && mouseY >= y + 19 && mouseY <= y + 71) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§6⚡ Controller Power Status"));
            lines.add(Component.literal("§eGrid Energy: §f" + String.format("%,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy())));

            if (this.menu.isFuelPowered()) {
                int totalSeconds = this.menu.getBurnTime() / 20;
                int minutes = totalSeconds / 60;
                int seconds = totalSeconds % 60;
                lines.add(Component.literal("§bEmergency Fuel: §f" + minutes + "m " + seconds + "s"));
                lines.add(Component.literal("§aStatus: Running on Emergency Fuel"));
            } else if (this.menu.isGridPowered()) {
                lines.add(Component.literal("§aStatus: Running on Grid Power (10 FE/t)"));
            } else {
                lines.add(Component.literal("§cStatus: Offline (Connect Cables or insert Fuel)"));
            }

            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }

        // Tooltip over Slot 1: Chunk Loader Slot (x + 35 .. 53, y + 47 .. 65)
        if (mouseX >= x + 35 && mouseX <= x + 53 && mouseY >= y + 47 && mouseY <= y + 65) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§b⚓ Chunk Loader Module Slot"));
            if (this.menu.hasChunkLoader()) {
                lines.add(Component.literal("§a● Status: ACTIVE (Base Chunk Loaded 24/7)"));
                lines.add(Component.literal("§7Enables infinite Overworld wireless access."));
            } else {
                lines.add(Component.literal("§7○ Status: EMPTY"));
                lines.add(Component.literal("§8Insert a Chunk Loader Module to keep base loaded."));
            }
            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }

        // Tooltip over Slot 0: Backup Fuel Slot (x + 79 .. 97, y + 47 .. 65)
        if (mouseX >= x + 79 && mouseX <= x + 97 && mouseY >= y + 47 && mouseY <= y + 65) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§d🔥 Emergency Backup Fuel Slot"),
                    Component.literal("§7Accepts: Enchanted Coal Block or Lava"),
                    Component.literal("§8Used automatically when Grid FE runs out.")
            ), mouseX, mouseY);
        }

        // Tooltip over Slot 2: Interdimensional Card Slot (x + 123 .. 141, y + 47 .. 65)
        if (mouseX >= x + 123 && mouseX <= x + 141 && mouseY >= y + 47 && mouseY <= y + 65) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§5🌌 Interdimensional Card Slot"));
            if (this.menu.hasInterdimensionalCard()) {
                lines.add(Component.literal("§5● Status: ACTIVE (Cross-Dimension Link Active)"));
                lines.add(Component.literal("§7Enables remote quarry & wireless access from:"));
                lines.add(Component.literal("§d✦ Nether, The End & Mining Dimension!"));
                lines.add(Component.literal("§a✨ Base 3x3 chunk area kept loaded 24/7."));
            } else {
                lines.add(Component.literal("§7○ Status: READY"));
                lines.add(Component.literal("§8Insert Interdimensional Card to access across dimensions."));
            }
            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }
    }
}
