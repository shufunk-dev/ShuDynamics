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
public class OxygenGeneratorScreen extends AbstractContainerScreen<OxygenGeneratorScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/oxygen_generator_gui.png");

    public OxygenGeneratorScreen(OxygenGeneratorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // 1. Water Bar (at x + 38, y + 20, width 14, height 52)
        int waterHeight = this.menu.getScaledWater(52);
        if (waterHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 38, y + 72 - waterHeight, 176.0f, 52.0f - waterHeight, 14, waterHeight, 256, 256);
        }

        // 2. Oxygen Bar (at x + 56, y + 20, width 14, height 52)
        int o2Height = this.menu.getScaledOxygen(52);
        if (o2Height > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 56, y + 72 - o2Height, 190.0f, 52.0f - o2Height, 14, o2Height, 256, 256);
        }

        // 3. Hydrogen Bar (at x + 96, y + 20, width 14, height 52)
        int h2Height = this.menu.getScaledHydrogen(52);
        if (h2Height > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 96, y + 72 - h2Height, 204.0f, 52.0f - h2Height, 14, h2Height, 256, 256);
        }

        // 4. Energy Bar (at x + 152, y + 20, width 14, height 52)
        int energyHeight = this.menu.getScaledEnergy(52);
        if (energyHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 152, y + 72 - energyHeight, 218.0f, 52.0f - energyHeight, 14, energyHeight, 256, 256);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);


        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Water Tooltip
        if (mouseX >= x + 37 && mouseX <= x + 53 && mouseY >= y + 19 && mouseY <= y + 73) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§9Water Reservoir"),
                    Component.literal(String.format("§b%,d / %,d mB", this.menu.getWaterAmount(), this.menu.getMaxWater()))
            ), mouseX, mouseY);
        }

        // Oxygen Tooltip
        if (mouseX >= x + 55 && mouseX <= x + 71 && mouseY >= y + 19 && mouseY <= y + 73) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§bOxygen Tank (O₂)"),
                    Component.literal(String.format("§f%,d / %,d mB", this.menu.getOxygenAmount(), this.menu.getMaxOxygen()))
            ), mouseX, mouseY);
        }

        // Hydrogen Tooltip
        if (mouseX >= x + 95 && mouseX <= x + 111 && mouseY >= y + 19 && mouseY <= y + 73) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§6Hydrogen Tank (H₂)"),
                    Component.literal(String.format("§e%,d / %,d mB", this.menu.getHydrogenAmount(), this.menu.getMaxHydrogen()))
            ), mouseX, mouseY);
        }

        // Energy Tooltip
        if (mouseX >= x + 151 && mouseX <= x + 167 && mouseY >= y + 19 && mouseY <= y + 73) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§e⚡ Energy Buffer"),
                    Component.literal(String.format("§6%,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy())),
                    Component.literal("§7Usage: 60 FE/t")
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 6) {
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§9🪣 Water Bucket Input"),
                        Component.literal("§7Insert Water Buckets to supply water for electrolysis.")
                ), mouseX, mouseY);
                case 1 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§7🪣 Empty Bucket Output"),
                        Component.literal("§7Emptied water buckets appear here.")
                ), mouseX, mouseY);
                case 2 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§b💨 Empty Canister In (Oxygen)"),
                        Component.literal("§7Insert empty gas canisters to fill with O₂ gas.")
                ), mouseX, mouseY);
                case 3 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§b✨ Oxygen Canister Output"),
                        Component.literal("§7Pressurized Oxygen Canisters (O₂) appear here.")
                ), mouseX, mouseY);
                case 4 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6💨 Empty Canister In (Hydrogen)"),
                        Component.literal("§7Insert empty gas canisters to fill with H₂ gas.")
                ), mouseX, mouseY);
                case 5 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6✨ Hydrogen Canister Output"),
                        Component.literal("§7Pressurized Hydrogen Canisters (H₂) appear here.")
                ), mouseX, mouseY);
            }
        }
    }
}
