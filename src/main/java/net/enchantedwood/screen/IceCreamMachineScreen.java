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

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class IceCreamMachineScreen extends HandledScreen<IceCreamMachineScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/ice_cream_machine_gui.png");

    public IceCreamMachineScreen(IceCreamMachineScreenHandler handler, PlayerInventory inventory, Text title) {
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

        // Draw Energy Vertical Gauge (x + 15, y + 20, w: 10, h: 45)
        int energyH = this.handler.getScaledEnergy(45);
        if (energyH > 0) {
            int energyY = (y + 20) + (45 - energyH);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 15, energyY, 192.0f, 45.0f - energyH, 10, energyH, 256, 256);
        }

        // Draw Churning Progress (x + 82, y + 34, w: 28, h: 17)
        int churn = this.handler.getScaledProgress(28);
        if (churn > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 82, y + 34, 176.0f, 46.0f, churn, 17, 256, 256);
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

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Energy Bar Hover Tooltip (x + 15 .. 25, y + 19 .. 66)
        if (mouseX >= x + 15 && mouseX <= x + 25 && mouseY >= y + 19 && mouseY <= y + 66) {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("§b⚡ Churner Energy"));
            lines.add(Text.literal(String.format("§f%,d / %,d FE", this.handler.getEnergy(), this.handler.getMaxEnergy())));
            lines.add(Text.literal("§7Can also be hand-cranked by right-clicking the block!"));
            context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        }

        // Refrigerant Slot Tooltip (x + 33 .. 51, y + 19 .. 37)
        if (mouseX >= x + 33 && mouseX <= x + 51 && mouseY >= y + 19 && mouseY <= y + 37) {
            if (!this.handler.getSlot(IceCreamMachineScreenHandler.REFRIGERANT_SLOT).hasStack()) {
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§b❄ Refrigerant Slot"),
                        Text.literal("§7Accepts: Ice, Ice Cubes, Packed/Blue Ice, Snow, or Salt")
                ), mouseX, mouseY);
            }
        }

        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
