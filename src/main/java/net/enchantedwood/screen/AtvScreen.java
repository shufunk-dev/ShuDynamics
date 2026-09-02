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

    private void drawSlotBox(DrawContext context, int sx, int sy) {
        context.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
        context.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
        context.fill(sx, sy, sx + 17, sy + 1, 0xFF373737);
        context.fill(sx, sy, sx + 1, sy + 17, 0xFF373737);
        context.fill(sx + 1, sy + 17, sx + 18, sy + 18, 0xFFFFFFFF);
        context.fill(sx + 17, sy + 1, sx + 18, sy + 18, 0xFFFFFFFF);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Draw Section Headers
        context.drawText(this.textRenderer, Text.literal("Installed"), 10, 7, 0x555555, false);
        context.drawText(this.textRenderer, Text.literal("Cargo"), 74, 7, 0x555555, false);
        context.drawText(this.textRenderer, Text.literal("Fuel"), 140, 7, 0x555555, false);
        context.drawText(this.textRenderer, Text.literal("Tool"), 140, 44, 0x555555, false);

        // Player Inventory Title
        context.drawText(this.textRenderer, this.playerInventoryTitle, 8, 73, 0x404040, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext context, int x, int y) {
        super.drawMouseoverTooltip(context, x, y);

        // If hovering over module or fuel slots
        if (this.focusedSlot != null && this.handler.getCursorStack().isEmpty()) {
            int slotId = this.focusedSlot.id;
            Text tooltip = null;

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

                if (this.focusedSlot.hasStack()) {
                    tooltip = Text.literal("§bInstalled " + slotName + "§r\n§8Modify/upgrade at Vehicle Fabricator");
                } else {
                    tooltip = Text.literal("§8No " + slotName + " Installed§r\n§7Install at Vehicle Fabricator");
                }
            } else if (slotId == AtvEntity.FUEL_SLOT && !this.focusedSlot.hasStack()) {
                tooltip = Text.literal("§6Fuel / Battery Slot§r\n§7Insert Gasoline, Biofuel, High-Octane, Coal, or Charged Battery.");
            } else if (slotId == AtvEntity.TOOL_SLOT && !this.focusedSlot.hasStack()) {
                tooltip = Text.literal("§6Attachment Tool Slot§r\n§7Insert Mining Drill Bit, Tree Harvester Saw, or Crop Harvester.\n§8Operates while driving and auto-routes all harvests into cargo trunk.");
            }

            if (tooltip != null) {
                context.drawTooltip(this.textRenderer, tooltip, x, y);
            }
        }
    }
}
