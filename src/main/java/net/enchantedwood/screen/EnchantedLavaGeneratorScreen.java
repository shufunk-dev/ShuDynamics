package net.enchantedwood.screen;

import net.enchantedwood.block.custom.GearTier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.enchantedwood.EnchantedWoodMod;

import java.util.List;

@Environment(EnvType.CLIENT)
public class EnchantedLavaGeneratorScreen extends AbstractContainerScreen<EnchantedLavaGeneratorScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/lava_generator_gui.png");

    public EnchantedLavaGeneratorScreen(EnchantedLavaGeneratorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.titleLabelX = 8;
        this.titleLabelY = 7;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // 1. Draw Enchanted Fuel Flame at (x + 27, y + 36, w: 14, h: 14)
        if (this.menu.isBurning()) {
            int fuelHeight = this.menu.getScaledFuelProgress();
            if (fuelHeight > 0) {
                context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 27, y + 50 - fuelHeight, 176.0f, 14.0f - fuelHeight, 14, fuelHeight, 256, 256);
            }
        }

        // 2. Draw Cook Progress Arrow at (x + 66, y + 19, w: 24, h: 17)
        int cookWidth = this.menu.getScaledCookProgress();
        if (cookWidth > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 66, y + 19, 176.0f, 14.0f, cookWidth, 17, 256, 256);

            // Active lava melt sparks
            if (this.menu.isCrafting()) {
                long time = System.currentTimeMillis();
                int sparkX = x + 66 + cookWidth - 1;
                int sparkY = y + 27 + (int) ((time / 100) % 3) - 1;
                int sparkColor = ((time / 80) % 2 == 0) ? 0xFFFF7700 : 0xFFFFDD33;
                context.fill(sparkX, sparkY, sparkX + 2, sparkY + 2, sparkColor);
            }
        }

        // 3. Draw Lava Fluid Reservoir Gauge at (x + 140, y + 17, width: 16, height: 52)
        int lavaHeight = this.menu.getScaledLavaProgress();
        if (lavaHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 140, y + 69 - lavaHeight, 176.0f, 83.0f - lavaHeight, 16, lavaHeight, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFF0C0FF, false);
        context.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF373A40, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Custom Tooltip for Lava Gauge when hovered (x: 139..157, y: 16..70)
        if (mouseX >= x + 139 && mouseX <= x + 157 && mouseY >= y + 16 && mouseY <= y + 70) {
            int lava = this.menu.getLavaAmount();
            int buckets = lava / 1000;
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§c🌋 Lava Reservoir Buffer"),
                    Component.literal(String.format("§6%,d / 10,000 mB §7(%d Buckets)", lava, buckets)),
                    Component.literal("§7Feeds adjacent Lava Pipes, Tanks, and Buckets.")
            ), mouseX, mouseY);
        }

        // Gear Tooltip (when gear installed in slot 2 at x + 62, y + 53)
        if (mouseX >= x + 61 && mouseX <= x + 79 && mouseY >= y + 52 && mouseY <= y + 70) {
            int tierOrdinal = this.menu.getSlot(2).hasItem() ? 1 : 0;
            // Let slot tooltip handle empty, but give custom upgrade details
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 5) {
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e📥 Cobblestone Input"),
                        Component.literal("§7Insert cobblestone to melt down into molten lava."),
                        Component.literal("§8Ratio: 10 Cobblestone -> 1,000 mB (1 Bucket).")
                ), mouseX, mouseY);
                case 1 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§d🔥 Enchanted Fuel Slot"),
                        Component.literal("§fAccepts: §aEnchanted Coal §7(10k ticks / 500s)"),
                        Component.literal("§f      or §aEnchanted Coal Block §7(90k ticks / 75m)"),
                        Component.literal("§7Provides sustained high-temperature thermal power.")
                ), mouseX, mouseY);
                case 2 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6⚙️ Enchanted Gear Upgrade Slot"),
                        Component.literal("§7Insert an Enchanted Gear or Blaze Overclock Core:"),
                        Component.literal("§f• Greatly accelerates melting & lava generation speed.")
                ), mouseX, mouseY);
                case 3 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§b🪣 Empty Bucket Input"),
                        Component.literal("§7Insert empty iron or copper buckets to fill automatically.")
                ), mouseX, mouseY);
                case 4 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§c🔥 Lava Bucket Output"),
                        Component.literal("§7Filled lava buckets are extracted here.")
                ), mouseX, mouseY);
            }
        }
    }
}
