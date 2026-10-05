package net.enchantedwood.screen;

import net.enchantedwood.EnchantedWoodMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

@Environment(EnvType.CLIENT)
public class BrickOvenScreen extends HandledScreen<BrickOvenScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/brick_oven_gui.png");

    public BrickOvenScreen(BrickOvenScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        this.titleX = 8;
        this.titleY = 6;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // 1. Electric Energy Gauge (x + 17, y + 20, w: 12, h: 48)
        int energyH = this.handler.getScaledEnergy(48);
        if (energyH > 0) {
            int topY = y + 20 + 48 - energyH;
            context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 17, topY, 192.0f, 48.0f - energyH, 12, energyH, 256, 256);
        }

        // 2. Hearth Flame (x + 56, y + 36, w: 14, h: 14)
        if (this.handler.isBurning()) {
            int burn = this.handler.getBurnProgress(14);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 56, y + 36 + 14 - burn, 176.0f, 14.0f - burn, 14, burn, 256, 256);
        } else if (this.handler.hasEnergy()) {
            // Illuminated electric heat flame
            context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 56, y + 36, 176.0f, 0.0f, 14, 14, 256, 256);
        }

        // 3. Baking Progress Arrow (x + 79, y + 34, w: 24, h: 17)
        int cook = this.handler.getCookProgress(24);
        if (cook > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 79, y + 34, 176.0f, 14.0f, cook, 17, 256, 256);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0xFF404040, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0xFF404040, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Energy Buffer Tooltip (x + 16 .. 30, y + 18 .. 70)
        if (mouseX >= x + 16 && mouseX <= x + 30 && mouseY >= y + 18 && mouseY <= y + 70) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§e⚡ Heating Element (Energy Buffer)"),
                    Text.literal(String.format("§6%,d / %,d FE", this.handler.getEnergy(), this.handler.getMaxEnergy())),
                    Text.literal("§7Usage: 20 FE/t while baking"),
                    Text.literal("§8Connect energy cables directly to any side.")
            ), mouseX, mouseY);
        }

        // Hearth Flame Tooltip (x + 55 .. 71, y + 35 .. 51)
        if (mouseX >= x + 55 && mouseX <= x + 71 && mouseY >= y + 35 && mouseY <= y + 51) {
            if (this.handler.isBurning()) {
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§6🔥 Oven Hearth: Active"),
                        Text.literal("§aBurning solid fuel."),
                        Text.literal("§7Baking at 2.5x standard speed.")
                ), mouseX, mouseY);
            } else if (this.handler.hasEnergy()) {
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§6🔥 Oven Hearth: Active"),
                        Text.literal("§b⚡ Powered by electric heating coil (20 FE/t)."),
                        Text.literal("§7No coal or wood needed while electrified!")
                ), mouseX, mouseY);
            } else {
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§6🔥 Oven Hearth: Cold"),
                        Text.literal("§7Insert solid fuel into the lower slot"),
                        Text.literal("§7or connect energy cables to power automatically.")
                ), mouseX, mouseY);
            }
        }

        // Empty Slot Tooltips
        if (this.focusedSlot != null && !this.focusedSlot.hasStack() && this.focusedSlot.id < 3) {
            switch (this.focusedSlot.id) {
                case 0 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§e📥 Baking Input Slot"),
                        Text.literal("§7Place items to bake or boil:"),
                        Text.literal("§f• Wheat Flour ➔ 2x Bread"),
                        Text.literal("§f• Pizza Dough ➔ Burger Buns"),
                        Text.literal("§f• Raw Pizzas, Patties, Meats, Corn"),
                        Text.literal("§f• Water Buckets ➔ 4x Salt (returns bucket)"),
                        Text.literal("§7Smokes & bakes 2.5x faster than a furnace.")
                ), mouseX, mouseY);
                case 1 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§6🔥 Solid Fuel Slot"),
                        Text.literal("§7Insert combustible fuels:"),
                        Text.literal("§f• Coal, Charcoal, Wood Logs, Planks, Sticks"),
                        Text.literal("§f• Lava Buckets (returns empty bucket)"),
                        Text.literal("§b⚡ Or connect energy cables to power without fuel!")
                ), mouseX, mouseY);
                case 2 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§a✨ Baked Goods Output"),
                        Text.literal("§7Finished food, bread, buns, and salt appear here.")
                ), mouseX, mouseY);
            }
        }
    }
}
