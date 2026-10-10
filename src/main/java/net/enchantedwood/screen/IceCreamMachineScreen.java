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
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class IceCreamMachineScreen extends AbstractContainerScreen<IceCreamMachineScreenHandler> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/ice_cream_machine_gui.png");

    public IceCreamMachineScreen(IceCreamMachineScreenHandler handler, Inventory inventory, Component title) {
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

        // Draw Energy Vertical Gauge (x + 15, y + 20, w: 10, h: 45)
        int energyH = this.menu.getScaledEnergy(45);
        if (energyH > 0) {
            int energyY = (y + 20) + (45 - energyH);
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 15, energyY, 192.0f, 45.0f - energyH, 10, energyH, 256, 256);
        }

        // Draw Churning Progress (x + 82, y + 34, w: 28, h: 17)
        int churn = this.menu.getScaledProgress(28);
        if (churn > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 82, y + 34, 176.0f, 46.0f, churn, 17, 256, 256);
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

        // Energy Bar Hover Tooltip (x + 15 .. 25, y + 19 .. 66)
        if (mouseX >= x + 15 && mouseX <= x + 25 && mouseY >= y + 19 && mouseY <= y + 66) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§b⚡ Churner Energy"));
            lines.add(Component.literal(String.format("§f%,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy())));
            lines.add(Component.literal("§7Can also be hand-cranked by right-clicking the block!"));
            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }

        // Refrigerant Slot Tooltip (x + 33 .. 51, y + 19 .. 37)
        if (mouseX >= x + 33 && mouseX <= x + 51 && mouseY >= y + 19 && mouseY <= y + 37) {
            if (!this.menu.getSlot(IceCreamMachineScreenHandler.REFRIGERANT_SLOT).hasItem()) {
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§b❄ Refrigerant Slot"),
                        Component.literal("§7Accepts: Ice, Ice Cubes, Packed/Blue Ice, Snow, or Salt")
                ), mouseX, mouseY);
            }
        }
    }
}
