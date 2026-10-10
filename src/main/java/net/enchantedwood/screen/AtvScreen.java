package net.enchantedwood.screen;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.entity.custom.AtvEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

@Environment(EnvType.CLIENT)
public class AtvScreen extends AbstractContainerScreen<AtvScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/atv_gui.png");

    public AtvScreen(AtvScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // Header and Gauges
        if (this.menu.getAtvInventory() instanceof AtvEntity atv) {
            float speed = atv.getDisplaySpeed();
            int fuel = atv.getFuelLevel();
            int maxFuel = atv.getMaxFuel();
            int fuelPct = maxFuel > 0 ? (fuel * 100 / maxFuel) : 0;

            String speedStr = String.format("🏎️ %.0f km/h", Math.abs(speed));
            String fuelStr = String.format("⛽ %d%%", fuelPct);

            context.text(this.font, Component.literal(speedStr), x + 60, y + 6, 0x00FFFF, false);
            context.text(this.font, Component.literal(fuelStr), x + 120, y + 6, fuelPct > 20 ? 0x55FF55 : 0xFF5555, false);
        }

        // Draw slot boxes for left 2x3 installed parts
        drawSlotBox(context, x + 9, y + 17);
        drawSlotBox(context, x + 9, y + 35);
        drawSlotBox(context, x + 9, y + 53);
        drawSlotBox(context, x + 27, y + 17);
        drawSlotBox(context, x + 27, y + 35);
        drawSlotBox(context, x + 27, y + 53);

        // Draw slot boxes for right column Fuel and Tool
        drawSlotBox(context, x + 141, y + 17);
        drawSlotBox(context, x + 141, y + 53);
    }

    private void drawSlotBox(GuiGraphicsExtractor context, int sx, int sy) {
        context.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
        context.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
        context.fill(sx, sy, sx + 17, sy + 1, 0xFF373737);
        context.fill(sx, sy, sx + 1, sy + 17, 0xFF373737);
        context.fill(sx + 1, sy + 17, sx + 18, sy + 18, 0xFFFFFFFF);
        context.fill(sx + 17, sy + 1, sx + 18, sy + 18, 0xFFFFFFFF);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        // Draw Section Headers
        context.text(this.font, Component.literal("Installed"), 10, 7, 0x555555, false);
        context.text(this.font, Component.literal("Cargo"), 74, 7, 0x555555, false);
        context.text(this.font, Component.literal("Fuel"), 140, 7, 0x555555, false);
        context.text(this.font, Component.literal("Tool"), 140, 44, 0x555555, false);

        // Player Inventory Title
        context.text(this.font, this.playerInventoryTitle, 8, 73, 0x404040, false);
    }

    

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);

        // If hovering over module or fuel slots
        if (this.hoveredSlot != null && this.menu.getCarried().isEmpty()) {
            int slotId = this.hoveredSlot.index;
            Component tooltip = null;

            if (slotId < 6) {
                String slotName = switch (slotId) {
                    case AtvEntity.ENGINE_SLOT -> "Engine";
                    case AtvEntity.TIRE_SLOT -> "Tires";
                    case AtvEntity.SUSPENSION_SLOT -> "Suspension";
                    case AtvEntity.CHASSIS_SLOT -> "Chassis";
                    case AtvEntity.HEADLIGHT_SLOT -> "Headlights";
                    case AtvEntity.TRUNK_SLOT -> "Cargo Trunk";
                    default -> "Part";
                };

                if (this.hoveredSlot.hasItem()) {
                    tooltip = Component.literal("§bInstalled " + slotName + "§r\n§8Modify/upgrade at Vehicle Fabricator");
                } else {
                    tooltip = Component.literal("§8No " + slotName + " Installed§r\n§7Install at Vehicle Fabricator");
                }
            } else if (slotId == AtvEntity.FUEL_SLOT && !this.hoveredSlot.hasItem()) {
                tooltip = Component.literal("§6Fuel / Battery Slot§r\n§7Insert Gasoline, Biofuel, High-Octane, Coal, or Charged Battery.");
            } else if (slotId == AtvEntity.TOOL_SLOT && !this.hoveredSlot.hasItem()) {
                tooltip = Component.literal("§6Attachment Tool Slot§r\n§7Insert Mining Drill Bit, Tree Harvester Saw, or Crop Harvester.\n§8Operates while driving and auto-routes all harvests into cargo trunk.");
            }

            if (tooltip != null) {
                context.setTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
            }
        }
    }
}
