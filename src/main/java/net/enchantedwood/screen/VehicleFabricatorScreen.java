package net.enchantedwood.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;

import java.util.List;

@Environment(EnvType.CLIENT)
public class VehicleFabricatorScreen extends HandledScreen<VehicleFabricatorScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/vehicle_fabricator_gui.png");
    private ButtonWidget assembleButton;

    public VehicleFabricatorScreen(VehicleFabricatorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 222;
        this.titleX = 8;
        this.titleY = 6;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Assemble / Apply Upgrades Button
        this.assembleButton = ButtonWidget.builder(Text.literal("🛠️"), button -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 0);
            }
        }).dimensions(x + 138, y + 52, 24, 18).build();

        this.addDrawableChild(this.assembleButton);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Draw main GUI texture
        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // Draw FE Energy Gauge (x + 9, y + 19, w: 10, h: 94)
        int energy = this.handler.getEnergy();
        int maxEnergy = this.handler.getMaxEnergy();
        if (maxEnergy > 0 && energy > 0) {
            int scaledHeight = Math.min(94, (int) ((long) energy * 94 / maxEnergy));
            int energyY = (y + 19) + (94 - scaledHeight);
            context.fill(x + 9, energyY, x + 19, y + 19 + 94, 0xFFFF2222);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Energy Bar Hover Tooltip (x + 8 .. 20, y + 18 .. 114)
        if (mouseX >= x + 8 && mouseX <= x + 20 && mouseY >= y + 18 && mouseY <= y + 114) {
            int energy = this.handler.getEnergy();
            int maxEnergy = this.handler.getMaxEnergy();
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§e⚡ Energy Storage"),
                    Text.literal(String.format("§f%,d / %,d FE", energy, maxEnergy)),
                    Text.literal("§7Draws 100 FE per assembly.")
            ), mouseX, mouseY);
        }

        // Assemble Button Hover Tooltip (x + 138 .. 162, y + 52 .. 70)
        if (mouseX >= x + 138 && mouseX <= x + 162 && mouseY >= y + 52 && mouseY <= y + 70) {
            boolean ready = this.handler.canFabricate();
            if (ready) {
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§a🛠️ Assemble / Apply Upgrades"),
                        Text.literal("§7Click to synthesize your custom Modular ATV!")
                ), mouseX, mouseY);
            } else {
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§6🛠️ Vehicle Assembly Bay"),
                        Text.literal("§cRequired: Seat, Engine, Chassis, Suspension, Tires (4)."),
                        Text.literal("§7Or place an existing ATV in the top slot to modify.")
                ), mouseX, mouseY);
            }
        }
    }
}
