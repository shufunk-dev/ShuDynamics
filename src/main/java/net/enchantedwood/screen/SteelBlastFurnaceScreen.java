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
public class SteelBlastFurnaceScreen extends HandledScreen<SteelBlastFurnaceScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/steel_blast_furnace_gui.png");

    public SteelBlastFurnaceScreen(SteelBlastFurnaceScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // 1. Hydrogen Gas Bar (at x + 20, y + 20, width 14, height 52)
        int h2Height = this.handler.getScaledHydrogen(52);
        if (h2Height > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 20, y + 72 - h2Height, 176.0f, 52.0f - h2Height, 14, h2Height, 256, 256);
        }

        // 2. Cook Arrow Progress (at x + 76, y + 35, width 24, height 17)
        int cookWidth = this.handler.getScaledCookProgress(24);
        if (cookWidth > 0) {
            float vOffset = this.handler.isGreenMode() ? 71.0f : 54.0f; // Green arrow when using Green Steel H2!
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 76, y + 35, 176.0f, vOffset, cookWidth, 17, 256, 256);
        }

        // 3. Energy Bar (at x + 152, y + 20, width 14, height 52)
        int energyHeight = this.handler.getScaledEnergy(52);
        if (energyHeight > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 152, y + 72 - energyHeight, 190.0f, 52.0f - energyHeight, 14, energyHeight, 256, 256);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Hydrogen Tooltip
        if (mouseX >= x + 19 && mouseX <= x + 35 && mouseY >= y + 19 && mouseY <= y + 73) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§dHydrogen Level (H₂)"),
                    Text.literal(String.format("§f%,d / %,d mB", this.handler.getHydrogenAmount(), this.handler.getMaxHydrogen())),
                    Text.literal("§7Direct reduction: 100 mB / ingot")
            ), mouseX, mouseY);
        }

        // Energy Tooltip
        if (mouseX >= x + 151 && mouseX <= x + 167 && mouseY >= y + 19 && mouseY <= y + 73) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§e⚡ Energy Buffer"),
                    Text.literal(String.format("§6%,d / %,d FE", this.handler.getEnergy(), this.handler.getMaxEnergy())),
                    Text.literal("§7Usage: 200 FE/t")
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.focusedSlot != null && !this.focusedSlot.hasStack() && this.focusedSlot.id < 5) {
            switch (this.focusedSlot.id) {
                case 0 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§e📥 Iron Input Slot"),
                        Text.literal("§7Insert Iron Ingots or Iron Dust.")
                ), mouseX, mouseY);
                case 1 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§8🔥 Coke Coal Fuel Slot"),
                        Text.literal("§fRequired: §aCoke Coal"),
                        Text.literal("§7Provides high-carbon blast reduction.")
                ), mouseX, mouseY);
                case 2 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§a✨ High-Grade Steel Ingot"),
                        Text.literal("§7Refined steel ingots appear here.")
                ), mouseX, mouseY);
                case 3 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§d💨 Hydrogen Canister In (Optional)"),
                        Text.literal("§fOptional: §aHydrogen Canister (H₂)"),
                        Text.literal("§7Accelerates steel reduction reactions.")
                ), mouseX, mouseY);
                case 4 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§7💨 Empty Canister Out"),
                        Text.literal("§7Depleted hydrogen canisters appear here.")
                ), mouseX, mouseY);
            }
        }
    }
}
