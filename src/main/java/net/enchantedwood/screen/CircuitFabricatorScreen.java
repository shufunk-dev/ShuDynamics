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
public class CircuitFabricatorScreen extends HandledScreen<CircuitFabricatorScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/circuit_fabricator_gui.png");

    public CircuitFabricatorScreen(CircuitFabricatorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // 1. Energy Bar at x + 10, y + 18 (width 12, height 50)
        int energyHeight = this.handler.getScaledEnergy(50);
        if (energyHeight > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 10, y + 68 - energyHeight, 176.0f, 50.0f - energyHeight, 12, energyHeight, 256, 256);
        }

        // 2. Fabrication Laser / Progress Arrow at x + 88, y + 34 (width 24, height 17)
        int cookWidth = this.handler.getScaledCookProgress(24);
        if (cookWidth > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 88, y + 34, 188.0f, 0.0f, cookWidth, 17, 256, 256);
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

        // Energy Bar Tooltip
        if (mouseX >= x + 9 && mouseX <= x + 23 && mouseY >= y + 17 && mouseY <= y + 69) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§6⚡ Energy Buffer"),
                    Text.literal(String.format("§e%,d / %,d FE", this.handler.getEnergy(), this.handler.getMaxEnergy())),
                    Text.literal("§7Draw: 35 FE/t")
            ), mouseX, mouseY);
        }

        // Progress Tooltip
        if (mouseX >= x + 87 && mouseX <= x + 113 && mouseY >= y + 33 && mouseY <= y + 52) {
            int cookProgress = this.handler.getScaledCookProgress(100);
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§b⚡ Circuit Fabrication Laser"),
                    Text.literal(String.format("§7Progress: §f%d%%", cookProgress)),
                    Text.literal("§8Aligns components and sinters microscopic silicon traces.")
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.focusedSlot != null && !this.focusedSlot.hasStack() && this.focusedSlot.id < 6) {
            switch (this.focusedSlot.id) {
                case 0 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§b💿 Semiconductor Substrate"),
                        Text.literal("§7Insert: §fSilicon Wafer §7(Basic/Metallurgy),"),
                        Text.literal("§7        §fBasic Chip §7(for Advanced),"),
                        Text.literal("§7        §fAdvanced Chip §7(for Quantum)"),
                        Text.literal("§8Foundation silicon wafer etched by the precision laser.")
                ), mouseX, mouseY);
                case 1 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§e⚡ Conductor Component"),
                        Text.literal("§7Insert: §fCopper Ingot, Diamond, Netherite Dust, or Gold Ingot"),
                        Text.literal("§8Conductive trace element for circuit pathways.")
                ), mouseX, mouseY);
                case 2 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§6✨ Logic & Semiconductor Gate"),
                        Text.literal("§7Insert: §fGold Nugget, Glowstone Dust, Blaze Powder, or Redstone"),
                        Text.literal("§8Transistor gate and logic frequency modulator.")
                ), mouseX, mouseY);
                case 3 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§a🔮 Signal & Dielectric Enhancer"),
                        Text.literal("§7Insert: §fRedstone, Lapis Lazuli, Enchanted Dust, or Zirconia Nodule"),
                        Text.literal("§8Stabilizing dielectric flux for micro-architecture.")
                ), mouseX, mouseY);
                case 4 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§d💾 Fabricated Microchip / Processor"),
                        Text.literal("§7Outputs finished microcontrollers:"),
                        Text.literal("§f• Basic, Advanced, Quantum Computer Chips"),
                        Text.literal("§6• Metallurgy Controller Chip"),
                        Text.literal("§8Laser-sintered integrated circuits.")
                ), mouseX, mouseY);
                case 5 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§6⚙ Overclock & Gear Socket"),
                        Text.literal("§7Accepts: §fCopper..Diamond Gears §7or §6Blaze Overclock Core"),
                        Text.literal("§8Accelerates laser fabrication speed up to 4.0×.")
                ), mouseX, mouseY);
            }
        }
    }
}
