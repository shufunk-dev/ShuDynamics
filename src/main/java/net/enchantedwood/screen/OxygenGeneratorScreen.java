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
public class OxygenGeneratorScreen extends HandledScreen<OxygenGeneratorScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/oxygen_generator_gui.png");

    public OxygenGeneratorScreen(OxygenGeneratorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // 1. Water Bar (at x + 38, y + 20, width 14, height 52)
        int waterHeight = this.handler.getScaledWater(52);
        if (waterHeight > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 38, y + 72 - waterHeight, 176.0f, 52.0f - waterHeight, 14, waterHeight, 256, 256);
        }

        // 2. Oxygen Bar (at x + 56, y + 20, width 14, height 52)
        int o2Height = this.handler.getScaledOxygen(52);
        if (o2Height > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 56, y + 72 - o2Height, 190.0f, 52.0f - o2Height, 14, o2Height, 256, 256);
        }

        // 3. Hydrogen Bar (at x + 96, y + 20, width 14, height 52)
        int h2Height = this.handler.getScaledHydrogen(52);
        if (h2Height > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 96, y + 72 - h2Height, 204.0f, 52.0f - h2Height, 14, h2Height, 256, 256);
        }

        // 4. Energy Bar (at x + 152, y + 20, width 14, height 52)
        int energyHeight = this.handler.getScaledEnergy(52);
        if (energyHeight > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 152, y + 72 - energyHeight, 218.0f, 52.0f - energyHeight, 14, energyHeight, 256, 256);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Water Tooltip
        if (mouseX >= x + 37 && mouseX <= x + 53 && mouseY >= y + 19 && mouseY <= y + 73) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§9Water Reservoir"),
                    Text.literal(String.format("§b%,d / %,d mB", this.handler.getWaterAmount(), this.handler.getMaxWater()))
            ), mouseX, mouseY);
        }

        // Oxygen Tooltip
        if (mouseX >= x + 55 && mouseX <= x + 71 && mouseY >= y + 19 && mouseY <= y + 73) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§bOxygen Tank (O₂)"),
                    Text.literal(String.format("§f%,d / %,d mB", this.handler.getOxygenAmount(), this.handler.getMaxOxygen()))
            ), mouseX, mouseY);
        }

        // Hydrogen Tooltip
        if (mouseX >= x + 95 && mouseX <= x + 111 && mouseY >= y + 19 && mouseY <= y + 73) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§6Hydrogen Tank (H₂)"),
                    Text.literal(String.format("§e%,d / %,d mB", this.handler.getHydrogenAmount(), this.handler.getMaxHydrogen()))
            ), mouseX, mouseY);
        }

        // Energy Tooltip
        if (mouseX >= x + 151 && mouseX <= x + 167 && mouseY >= y + 19 && mouseY <= y + 73) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§e⚡ Energy Buffer"),
                    Text.literal(String.format("§6%,d / %,d FE", this.handler.getEnergy(), this.handler.getMaxEnergy())),
                    Text.literal("§7Usage: 60 FE/t")
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.focusedSlot != null && !this.focusedSlot.hasStack() && this.focusedSlot.id < 6) {
            switch (this.focusedSlot.id) {
                case 0 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§9🪣 Water Bucket Input"),
                        Text.literal("§7Insert Water Buckets to supply water for electrolysis.")
                ), mouseX, mouseY);
                case 1 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§7🪣 Empty Bucket Output"),
                        Text.literal("§7Emptied water buckets appear here.")
                ), mouseX, mouseY);
                case 2 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§b💨 Empty Canister In (Oxygen)"),
                        Text.literal("§7Insert empty gas canisters to fill with O₂ gas.")
                ), mouseX, mouseY);
                case 3 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§b✨ Oxygen Canister Output"),
                        Text.literal("§7Pressurized Oxygen Canisters (O₂) appear here.")
                ), mouseX, mouseY);
                case 4 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§6💨 Empty Canister In (Hydrogen)"),
                        Text.literal("§7Insert empty gas canisters to fill with H₂ gas.")
                ), mouseX, mouseY);
                case 5 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§6✨ Hydrogen Canister Output"),
                        Text.literal("§7Pressurized Hydrogen Canisters (H₂) appear here.")
                ), mouseX, mouseY);
            }
        }
    }
}
