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

        // Draw Assembly Progress Arrow (x + 138, y + 45, w: 24, h: 4)
        if (this.handler.isFabricating()) {
            int progress = this.handler.getProgress();
            int maxProgress = this.handler.getMaxProgress();
            if (maxProgress > 0) {
                int progressWidth = Math.min(24, (int) ((long) progress * 24 / maxProgress));
                // Cyan / Green active fabrication progress bar
                context.fill(x + 138, y + 46, x + 138 + progressWidth, y + 49, 0xFF00FFCC);
            }
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);

        // Render clean slot labels right on the GUI surface
        context.drawText(this.textRenderer, "Seat", 69, 12, 0xFF88AACC, false);
        context.drawText(this.textRenderer, "Engine", 23, 34, 0xFF88AACC, false);
        context.drawText(this.textRenderer, "Chassis", 62, 44, 0xFF88AACC, false);
        int suspX = 106 + 9 - this.textRenderer.getWidth("Suspension") / 2;
        context.drawText(this.textRenderer, "Suspension", suspX, 34, 0xFF88AACC, false);
        context.drawText(this.textRenderer, "Tires", 28, 76, 0xFF88AACC, false);
        context.drawText(this.textRenderer, "Lights", 65, 76, 0xFF88AACC, false);
        context.drawText(this.textRenderer, "Trunk", 101, 76, 0xFF88AACC, false);
        context.drawText(this.textRenderer, "ATV In", 137, 14, 0xFF88AACC, false);
        context.drawText(this.textRenderer, "Out", 143, 72, 0xFF88AACC, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Update assemble button text / active state
        if (this.assembleButton != null) {
            if (this.handler.isFabricating()) {
                this.assembleButton.setMessage(Text.literal("⏳"));
                this.assembleButton.active = false;
            } else {
                this.assembleButton.setMessage(Text.literal("🛠️"));
                this.assembleButton.active = this.handler.canFabricate();
            }
        }

        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        // Empty Slot Tooltip Guides
        if (this.focusedSlot != null && !this.focusedSlot.hasStack()) {
            int slotIndex = this.focusedSlot.getIndex();
            switch (slotIndex) {
                case 0 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§e🚗 Vehicle In / Upgrade Bay"),
                        Text.literal("§7Place an existing ATV here to modify,"),
                        Text.literal("§7upgrade parts, or swap components.")
                ), mouseX, mouseY);
                case 1 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§e🪑 Seat Slot"),
                        Text.literal("§fRequired: §aATV Leather Seat"),
                        Text.literal("§7Ergonomic driver seating.")
                ), mouseX, mouseY);
                case 2 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§e⚙️ Engine Slot"),
                        Text.literal("§fRequired: §aEngine Module"),
                        Text.literal("§7Copper, Aluminum, Steel, or Titanium Engine."),
                        Text.literal("§7Drives vehicle horsepower & top speed.")
                ), mouseX, mouseY);
                case 3 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§e🏗️ Chassis Slot"),
                        Text.literal("§fRequired: §aATV Chassis"),
                        Text.literal("§7Aluminum, Steel, or Titanium Chassis."),
                        Text.literal("§7Heavy structural vehicle frame.")
                ), mouseX, mouseY);
                case 4 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§e🚜 Suspension Slot"),
                        Text.literal("§fRequired: §a4x Suspension Units"),
                        Text.literal("§7Aluminum, Steel, or Titanium Suspension (set of 4)."),
                        Text.literal("§7Improves step clearance & shock absorption.")
                ), mouseX, mouseY);
                case 5 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§e🛞 Tires Slot"),
                        Text.literal("§fRequired: §a4x Tires"),
                        Text.literal("§7Rubber, Steel Rim, or Studded Tires.")
                ), mouseX, mouseY);
                case 6 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§e💡 Headlights Slot"),
                        Text.literal("§fRequired: §aHeadlights Module"),
                        Text.literal("§7Halogen (12), LED (15), or Xenon High-Beams.")
                ), mouseX, mouseY);
                case 7 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§e📦 Cargo Trunk Slot (Optional)"),
                        Text.literal("§fOptional: §aSmall, Medium, or Large Trunk"),
                        Text.literal("§7Mounts 9 to 27 mobile cargo chest slots.")
                ), mouseX, mouseY);
                case 8 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§a✨ Vehicle Output Bay"),
                        Text.literal("§7Finished or upgraded ATV appears here."),
                        Text.literal("§7Shift-click or grab when fabrication completes.")
                ), mouseX, mouseY);
            }
        }

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Energy Bar Hover Tooltip (x + 8 .. 20, y + 18 .. 114)
        if (mouseX >= x + 8 && mouseX <= x + 20 && mouseY >= y + 18 && mouseY <= y + 114) {
            int energy = this.handler.getEnergy();
            int maxEnergy = this.handler.getMaxEnergy();
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§e⚡ Energy Storage"),
                    Text.literal(String.format("§f%,d / %,d FE", energy, maxEnergy)),
                    Text.literal("§7Draws 5 FE/t during fabrication.")
            ), mouseX, mouseY);
        }

        // Assemble Button & Progress Hover Tooltip (x + 138 .. 162, y + 45 .. 70)
        if (mouseX >= x + 138 && mouseX <= x + 162 && mouseY >= y + 45 && mouseY <= y + 70) {
            if (this.handler.isFabricating()) {
                int progress = this.handler.getProgress();
                int maxProgress = this.handler.getMaxProgress();
                double remainingSeconds = (double) (maxProgress - progress) / 20.0;
                int percent = (int) ((double) progress * 100.0 / maxProgress);
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§b⚙️ Fabrication in Progress..."),
                        Text.literal(String.format("§fProgress: §a%d%% §7(%.1fs remaining)", percent, remainingSeconds)),
                        Text.literal("§8Hydraulic tooling and alignment in progress.")
                ), mouseX, mouseY);
            } else if (this.handler.canFabricate()) {
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§a🛠️ Assemble / Apply Upgrades"),
                        Text.literal("§7Click to start tiered assembly timer!"),
                        Text.literal("§8Higher tier components require longer precision fabrication.")
                ), mouseX, mouseY);
            } else {
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§6🛠️ Vehicle Assembly Bay"),
                        Text.literal("§cRequired: Seat, Engine, Chassis, Suspension (4), Tires (4)."),
                        Text.literal("§7Or place an existing ATV in the top slot to modify.")
                ), mouseX, mouseY);
            }
        }
    }
}
