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
import net.enchantedwood.block.custom.GearTier;

import java.util.List;

@Environment(EnvType.CLIENT)
public class CryoFreezerScreen extends HandledScreen<CryoFreezerScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/cryo_freezer_gui.png");

    public CryoFreezerScreen(CryoFreezerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // 1. Energy Gauge at x + 16, y + 20 (width 14, height 50)
        int energyHeight = this.handler.getScaledEnergy(50);
        if (energyHeight > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 16, y + 70 - energyHeight, 192.0f, 50.0f - energyHeight, 14, energyHeight, 256, 256);
        }

        // 2. Water Reservoir at x + 42, y + 20 (width 18, height 50)
        int waterHeight = this.handler.getScaledWater(50);
        if (waterHeight > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 42, y + 70 - waterHeight, 212.0f, 50.0f - waterHeight, 18, waterHeight, 256, 256);
        }

        // 3. Freezing Progress Indicator at x + 120, y + 35 (width 22, height 16)
        int freezeWidth = this.handler.getScaledProgress(22);
        if (freezeWidth > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 120, y + 35, 176.0f, 14.0f, freezeWidth, 16, 256, 256);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 4210752, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 4210752, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Energy Bar Tooltip (x: 15..30, y: 19..70)
        if (mouseX >= x + 15 && mouseX <= x + 30 && mouseY >= y + 19 && mouseY <= y + 70) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§b⚡ Energy Buffer"),
                    Text.literal(String.format("§e%,d / %,d FE", this.handler.getEnergy(), this.handler.getMaxEnergy())),
                    Text.literal("§7Usage: 25 FE/t (reduced by gears)")
            ), mouseX, mouseY);
        }

        // Water Tank Tooltip (x: 41..60, y: 19..70)
        if (mouseX >= x + 41 && mouseX <= x + 60 && mouseY >= y + 19 && mouseY <= y + 70) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§9💧 Internal Water Reservoir"),
                    Text.literal(String.format("§b%,d / 10,000 mB", this.handler.getWaterAmount())),
                    Text.literal("§7Feeds cryogenic freezing coils. Connects to Water Pump & pipes.")
            ), mouseX, mouseY);
        }

        // Freezing Progress Tooltip (x: 119..142, y: 34..51)
        if (mouseX >= x + 119 && mouseX <= x + 142 && mouseY >= y + 34 && mouseY <= y + 51) {
            int pct = (this.handler.getFreezeProgress() * 100) / Math.max(1, this.handler.getTotalFreezeTime());
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§b❄ Cryogenic Freezing"),
                    Text.literal(String.format("§7Progress: §f%d%%", pct))
            ), mouseX, mouseY);
        }

        // Gear Tooltip (when gear installed, x: 151..169, y: 7..25)
        if (this.handler.getActiveGearTier() != GearTier.NONE) {
            if (mouseX >= x + 151 && mouseX <= x + 169 && mouseY >= y + 7 && mouseY <= y + 25) {
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§d⚙ Installed Gear Upgrade"),
                        Text.literal(String.format("§7Tier: §f%s", this.handler.getActiveGearTier().name())),
                        Text.literal(String.format("§7Freezing Time: §b%d ticks", this.handler.getTotalFreezeTime()))
                ), mouseX, mouseY);
            }
        }

        // Empty Machine Slot Tooltips
        if (this.focusedSlot != null && !this.focusedSlot.hasStack() && this.focusedSlot.id < 5) {
            switch (this.focusedSlot.id) {
                case 0 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§b🪣 Water Bucket Input"),
                        Text.literal("§7Insert Water Buckets to replenish the internal tank.")
                ), mouseX, mouseY);
                case 1 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§7🪣 Bucket Return"),
                        Text.literal("§7Empty buckets returned here.")
                ), mouseX, mouseY);
                case 2 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§f❄ Solid Material Input (Optional)"),
                        Text.literal("§7Leave empty for Water -> Ice,"),
                        Text.literal("§7or insert Ice (for Packed Ice) or Packed Ice (for Blue Ice).")
                ), mouseX, mouseY);
                case 3 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§b🧊 Frozen Product Output"),
                        Text.literal("§7Manufactured Ice, Packed Ice, or Blue Ice appears here.")
                ), mouseX, mouseY);
                case 4 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§d⚙️ Gear Upgrade Slot"),
                        Text.literal("§7Insert an alloy Gear to overclock freezing speed.")
                ), mouseX, mouseY);
            }
        }
    }
}
