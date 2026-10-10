package net.enchantedwood.screen;

import net.enchantedwood.EnchantedWoodMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;

@Environment(EnvType.CLIENT)
public class BrickOvenScreen extends AbstractContainerScreen<BrickOvenScreenHandler> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/brick_oven_gui.png");

    public BrickOvenScreen(BrickOvenScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // 1. Electric Energy Gauge (x + 17, y + 20, w: 12, h: 48)
        int energyH = this.menu.getScaledEnergy(48);
        if (energyH > 0) {
            int topY = y + 20 + 48 - energyH;
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 17, topY, 192.0f, 48.0f - energyH, 12, energyH, 256, 256);
        }

        // 2. Hearth Flame (x + 56, y + 36, w: 14, h: 14)
        if (this.menu.isBurning()) {
            int burn = this.menu.getBurnProgress(14);
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 56, y + 36 + 14 - burn, 176.0f, 14.0f - burn, 14, burn, 256, 256);
        } else if (this.menu.hasEnergy()) {
            // Illuminated electric heat flame
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 56, y + 36, 176.0f, 0.0f, 14, 14, 256, 256);
        }

        // 3. Baking Progress Arrow (x + 79, y + 34, w: 24, h: 17)
        int cook = this.menu.getCookProgress(24);
        if (cook > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 79, y + 34, 176.0f, 14.0f, cook, 17, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
        context.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);


        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Energy Buffer Tooltip (x + 16 .. 30, y + 18 .. 70)
        if (mouseX >= x + 16 && mouseX <= x + 30 && mouseY >= y + 18 && mouseY <= y + 70) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§e⚡ Heating Element (Energy Buffer)"),
                    Component.literal(String.format("§6%,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy())),
                    Component.literal("§7Usage: 20 FE/t while baking"),
                    Component.literal("§8Connect energy cables directly to any side.")
            ), mouseX, mouseY);
        }

        // Hearth Flame Tooltip (x + 55 .. 71, y + 35 .. 51)
        if (mouseX >= x + 55 && mouseX <= x + 71 && mouseY >= y + 35 && mouseY <= y + 51) {
            if (this.menu.isBurning()) {
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6🔥 Oven Hearth: Active"),
                        Component.literal("§aBurning solid fuel."),
                        Component.literal("§7Baking at 2.5x standard speed.")
                ), mouseX, mouseY);
            } else if (this.menu.hasEnergy()) {
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6🔥 Oven Hearth: Active"),
                        Component.literal("§b⚡ Powered by electric heating coil (20 FE/t)."),
                        Component.literal("§7No coal or wood needed while electrified!")
                ), mouseX, mouseY);
            } else {
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6🔥 Oven Hearth: Cold"),
                        Component.literal("§7Insert solid fuel into the lower slot"),
                        Component.literal("§7or connect energy cables to power automatically.")
                ), mouseX, mouseY);
            }
        }

        // Empty Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 3) {
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e📥 Baking Input Slot"),
                        Component.literal("§7Place items to bake or boil:"),
                        Component.literal("§f• Wheat Flour ➔ 2x Bread"),
                        Component.literal("§f• Pizza Dough ➔ Burger Buns"),
                        Component.literal("§f• Raw Pizzas, Patties, Meats, Corn"),
                        Component.literal("§f• Water Buckets ➔ 4x Salt (returns bucket)"),
                        Component.literal("§7Smokes & bakes 2.5x faster than a furnace.")
                ), mouseX, mouseY);
                case 1 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6🔥 Solid Fuel Slot"),
                        Component.literal("§7Insert combustible fuels:"),
                        Component.literal("§f• Coal, Charcoal, Wood Logs, Planks, Sticks"),
                        Component.literal("§f• Lava Buckets (returns empty bucket)"),
                        Component.literal("§b⚡ Or connect energy cables to power without fuel!")
                ), mouseX, mouseY);
                case 2 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§a✨ Baked Goods Output"),
                        Component.literal("§7Finished food, bread, buns, and salt appear here.")
                ), mouseX, mouseY);
            }
        }
    }
}
