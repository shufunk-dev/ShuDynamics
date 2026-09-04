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
public class MagmaCrucibleScreen extends HandledScreen<MagmaCrucibleScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/magma_crucible_gui.png");

    public MagmaCrucibleScreen(MagmaCrucibleScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // 1. Energy Bar at x + 18, y + 20 (width 16, height 50)
        int energyHeight = this.handler.getScaledEnergy(50);
        if (energyHeight > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 18, y + 70 - energyHeight, 192.0f, 50.0f - energyHeight, 16, energyHeight, 256, 256);
        }

        // 2. Melt Progress Arrow at x + 66, y + 34 (width 24, height 17)
        int cookWidth = this.handler.getScaledCookProgress(24);
        if (cookWidth > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 66, y + 34, 176.0f, 14.0f, cookWidth, 17, 256, 256);
        }

        // 3. Lava Tank at x + 96, y + 20 (width 16, height 50)
        int lavaHeight = this.handler.getScaledLava(50);
        if (lavaHeight > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 96, y + 70 - lavaHeight, 212.0f, 50.0f - lavaHeight, 16, lavaHeight, 256, 256);
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
        if (mouseX >= x + 17 && mouseX <= x + 35 && mouseY >= y + 19 && mouseY <= y + 71) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§6⚡ Energy Buffer"),
                    Text.literal(String.format("§e%,d / %,d FE", this.handler.getEnergy(), this.handler.getMaxEnergy())),
                    Text.literal("§7Usage: 35 FE/t")
            ), mouseX, mouseY);
        }

        // Lava Tank Tooltip
        if (mouseX >= x + 95 && mouseX <= x + 113 && mouseY >= y + 19 && mouseY <= y + 71) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§c🌋 Lava Buffer Tank"),
                    Text.literal(String.format("§6%,d / 10,000 mB", this.handler.getLavaAmount())),
                    Text.literal("§7Melts Basalt, Blackstone, Magma, and Netherrack")
            ), mouseX, mouseY);
        }

        // Gear Tooltip (when gear installed)
        if (mouseX >= x + 151 && mouseX <= x + 169 && mouseY >= y + 7 && mouseY <= y + 25 && this.handler.getGearTier() != net.enchantedwood.block.custom.GearTier.NONE) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§d⚙ Installed Gear Upgrade"),
                    Text.literal(String.format("§7Speed Tier: §f%s", this.handler.getGearTier().name()))
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.focusedSlot != null && !this.focusedSlot.hasStack() && this.focusedSlot.id < 5) {
            switch (this.focusedSlot.id) {
                case 0 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§e📥 Geological Melting Input"),
                        Text.literal("§7Insert volcanic rock or stone:"),
                        Text.literal("§f• Basalt, Blackstone, Netherrack, Magma Block"),
                        Text.literal("§7Melts stone into fluid lava.")
                ), mouseX, mouseY);
                case 1 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§6💎 Mineral Byproduct Output"),
                        Text.literal("§7Precious slag, sulfur, or minerals appear here.")
                ), mouseX, mouseY);
                case 2 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§b🪣 Empty Bucket Input"),
                        Text.literal("§7Insert empty buckets to automatically"),
                        Text.literal("§7bottle melted lava from the internal tank.")
                ), mouseX, mouseY);
                case 3 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§c🔥 Lava Bucket Output"),
                        Text.literal("§7Filled lava buckets appear here.")
                ), mouseX, mouseY);
                case 4 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§d⚙️ Gear Upgrade Slot"),
                        Text.literal("§7Insert a Gear or Blaze Overclock Core:"),
                        Text.literal("§f• Dramatically increases melting temperature & speed.")
                ), mouseX, mouseY);
            }
        }
    }
}
