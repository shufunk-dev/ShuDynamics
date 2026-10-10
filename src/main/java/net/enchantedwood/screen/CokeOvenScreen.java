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
public class CokeOvenScreen extends AbstractContainerScreen<CokeOvenScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/coke_oven_gui.png");

    public CokeOvenScreen(CokeOvenScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    private void drawSlotBox(GuiGraphicsExtractor context, int boxX, int boxY) {
        context.fill(boxX, boxY, boxX + 18, boxY + 1, 0xFF373737);
        context.fill(boxX, boxY, boxX + 1, boxY + 18, 0xFF373737);
        context.fill(boxX + 1, boxY + 1, boxX + 17, boxY + 17, 0xFF8B8B8B);
        context.fill(boxX + 1, boxY + 17, boxX + 18, boxY + 18, 0xFFFFFFFF);
        context.fill(boxX + 17, boxY + 1, boxX + 18, boxY + 18, 0xFFFFFFFF);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // Draw Mineral Tar slot frame at (142, 35) -> box at (141, 34)
        drawSlotBox(context, x + 141, y + 34);

        // Cook Progress Arrow at (79, 34, 24, 17)
        int cookWidth = this.menu.getScaledCookProgress(24);
        if (cookWidth > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 79, y + 34, 176.0f, 14.0f, cookWidth, 17, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        context.text(this.font, Component.literal("§8Tar"), 142, 23, 0x555555, false);
        context.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);


        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 3) {
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e📥 Raw Carbon Input"),
                        Component.literal("§7Insert Coal, Charcoal, or Logs:"),
                        Component.literal("§7Bakes carbon in oxygen-free pyrolysis.")
                ), mouseX, mouseY);
                case 1 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§8🔥 Coke Coal Output"),
                        Component.literal("§7High-efficiency industrial fuel appears here.")
                ), mouseX, mouseY);
                case 2 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6🛢️ Mineral Tar Byproduct"),
                        Component.literal("§7Recovered condensate from coal pyrolysis.")
                ), mouseX, mouseY);
            }
        }
    }
}
