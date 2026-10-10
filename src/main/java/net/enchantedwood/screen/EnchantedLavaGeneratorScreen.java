package net.enchantedwood.screen;

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
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // Draw Fuel Flame
        if (this.menu.isBurning()) {
            int fuelHeight = this.menu.getScaledFuelProgress();
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 27, y + 49 - fuelHeight, 176.0f, 14.0f - fuelHeight, 14, fuelHeight + 1, 256, 256);
        }

        // Draw Cook Progress Arrow
        int cookWidth = this.menu.getScaledCookProgress();
        if (cookWidth > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 66, y + 19, 176.0f, 14.0f, cookWidth + 1, 17, 256, 256);
        }

        // Draw Lava Fluid Reservoir Gauge (x: 140, y: 17, width: 16, height: 52)
        int lavaHeight = this.menu.getScaledLavaProgress();
        if (lavaHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 140, y + 69 - lavaHeight, 176.0f, 83.0f - lavaHeight, 16, lavaHeight, 256, 256);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);


        // Custom Tooltip for Lava Gauge when hovered (x: 140, y: 17, width: 16, height: 52)
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        if (mouseX >= x + 140 && mouseX <= x + 156 && mouseY >= y + 17 && mouseY <= y + 69) {
            int lava = this.menu.getLavaAmount();
            int buckets = lava / 1000;
            context.setTooltipForNextFrame(this.font, Component.literal("§c🌋 Lava Gauge: §f" + String.format("%,d", lava) + " / 10,000 mB §7(" + buckets + " Bucket" + (buckets == 1 ? "" : "s") + ")"), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 5) {
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e📥 Cobblestone Input"),
                        Component.literal("§7Insert cobblestone to melt down into molten lava.")
                ), mouseX, mouseY);
                case 1 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§d🔥 Enchanted Fuel Slot"),
                        Component.literal("§fRequired: §aEnchanted Coal Block"),
                        Component.literal("§7Provides sustained high-temperature thermal power.")
                ), mouseX, mouseY);
                case 2 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6⚙️ Enchanted Gear Upgrade Slot"),
                        Component.literal("§7Insert an Enchanted Gear or Blaze Overclock Core:"),
                        Component.literal("§f• Greatly accelerates melting & lava generation speed.")
                ), mouseX, mouseY);
                case 3 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§b🪣 Empty Bucket Input"),
                        Component.literal("§7Insert empty iron or copper buckets to fill.")
                ), mouseX, mouseY);
                case 4 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§c🔥 Lava Bucket Output"),
                        Component.literal("§7Filled lava buckets appear here.")
                ), mouseX, mouseY);
            }
        }
    }
}
