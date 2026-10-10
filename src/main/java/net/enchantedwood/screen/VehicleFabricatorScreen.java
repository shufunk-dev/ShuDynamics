package net.enchantedwood.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.enchantedwood.EnchantedWoodMod;

import java.util.List;

@Environment(EnvType.CLIENT)
public class VehicleFabricatorScreen extends AbstractContainerScreen<VehicleFabricatorScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/vehicle_fabricator_gui.png");
    private Button assembleButton;

    public VehicleFabricatorScreen(VehicleFabricatorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 176, 222);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Assemble / Apply Upgrades Button
        this.assembleButton = Button.builder(Component.literal("🛠️"), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            }
        }).bounds(x + 138, y + 52, 24, 18).build();

        this.addRenderableWidget(this.assembleButton);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Draw main GUI texture
        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // Draw FE Energy Gauge (x + 9, y + 19, w: 10, h: 94)
        int energy = this.menu.getEnergy();
        int maxEnergy = this.menu.getMaxEnergy();
        if (maxEnergy > 0 && energy > 0) {
            int scaledHeight = Math.min(94, (int) ((long) energy * 94 / maxEnergy));
            int energyY = (y + 19) + (94 - scaledHeight);
            context.fill(x + 9, energyY, x + 20, y + 19 + 94, 0xFFFF2222);
        }

        // Draw Assembly Progress Arrow (x + 138, y + 45, w: 24, h: 4)
        if (this.menu.isFabricating()) {
            int progress = this.menu.getProgress();
            int maxProgress = this.menu.getMaxProgress();
            if (maxProgress > 0) {
                int progressWidth = Math.min(24, (int) ((long) progress * 24 / maxProgress));
                // Cyan / Green active fabrication progress bar
                context.fill(x + 138, y + 46, x + 138 + progressWidth, y + 49, 0xFF00FFCC);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractLabels(context, mouseX, mouseY);

        // Render clean slot labels right on the GUI surface
        context.text(this.font, "Seat", 69, 12, 0xFF88AACC, false);
        context.text(this.font, "Engine", 23, 34, 0xFF88AACC, false);
        context.text(this.font, "Chassis", 62, 44, 0xFF88AACC, false);
        int suspX = 106 + 9 - this.font.width("Suspension") / 2;
        context.text(this.font, "Suspension", suspX, 34, 0xFF88AACC, false);
        context.text(this.font, "Tires", 28, 76, 0xFF88AACC, false);
        context.text(this.font, "Lights", 65, 76, 0xFF88AACC, false);
        context.text(this.font, "Trunk", 101, 76, 0xFF88AACC, false);
        context.text(this.font, "ATV In", 137, 14, 0xFF88AACC, false);
        context.text(this.font, "Out", 143, 72, 0xFF88AACC, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);

        // Update assemble button text / active state
        if (this.assembleButton != null) {
            if (this.menu.isFabricating()) {
                this.assembleButton.setMessage(Component.literal("⏳"));
                this.assembleButton.active = false;
            } else {
                this.assembleButton.setMessage(Component.literal("🛠️"));
                this.assembleButton.active = this.menu.canFabricate();
            }
        }

        // Empty Slot Tooltip Guides
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem()) {
            int slotIndex = this.hoveredSlot.getContainerSlot();
            switch (slotIndex) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e🚗 Vehicle In / Upgrade Bay"),
                        Component.literal("§7Place an existing ATV here to modify,"),
                        Component.literal("§7upgrade parts, or swap components.")
                ), mouseX, mouseY);
                case 1 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e🪑 Seat Slot"),
                        Component.literal("§fRequired: §aATV Leather Seat"),
                        Component.literal("§7Ergonomic driver seating.")
                ), mouseX, mouseY);
                case 2 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e⚙️ Engine Slot"),
                        Component.literal("§fRequired: §aEngine Module"),
                        Component.literal("§7Copper, Aluminum, Steel, or Titanium Engine."),
                        Component.literal("§7Drives vehicle horsepower & top speed.")
                ), mouseX, mouseY);
                case 3 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e🏗️ Chassis Slot"),
                        Component.literal("§fRequired: §aATV Chassis"),
                        Component.literal("§7Aluminum, Steel, or Titanium Chassis."),
                        Component.literal("§7Heavy structural vehicle frame.")
                ), mouseX, mouseY);
                case 4 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e🚜 Suspension Slot"),
                        Component.literal("§fRequired: §a4x Suspension Units"),
                        Component.literal("§7Aluminum, Steel, or Titanium Suspension (set of 4)."),
                        Component.literal("§7Improves step clearance & shock absorption.")
                ), mouseX, mouseY);
                case 5 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e🛞 Tires Slot"),
                        Component.literal("§fRequired: §a4x Tires"),
                        Component.literal("§7Rubber, Steel Rim, or Studded Tires.")
                ), mouseX, mouseY);
                case 6 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e💡 Headlights Slot"),
                        Component.literal("§fRequired: §aHeadlights Module"),
                        Component.literal("§7Halogen (12), LED (15), or Xenon High-Beams.")
                ), mouseX, mouseY);
                case 7 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e📦 Cargo Trunk Slot (Optional)"),
                        Component.literal("§fOptional: §aSmall, Medium, or Large Trunk"),
                        Component.literal("§7Mounts 9 to 27 mobile cargo chest slots.")
                ), mouseX, mouseY);
                case 8 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§a✨ Vehicle Output Bay"),
                        Component.literal("§7Finished or upgraded ATV appears here."),
                        Component.literal("§7Shift-click or grab when fabrication completes.")
                ), mouseX, mouseY);
            }
        }

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Energy Bar Hover Tooltip (x + 8 .. 20, y + 18 .. 114)
        if (mouseX >= x + 8 && mouseX <= x + 20 && mouseY >= y + 18 && mouseY <= y + 114) {
            int energy = this.menu.getEnergy();
            int maxEnergy = this.menu.getMaxEnergy();
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§e⚡ Energy Storage"),
                    Component.literal(String.format("§f%,d / %,d FE", energy, maxEnergy)),
                    Component.literal("§7Draws 5 FE/t during fabrication.")
            ), mouseX, mouseY);
        }

        // Assemble Button & Progress Hover Tooltip (x + 138 .. 162, y + 45 .. 70)
        if (mouseX >= x + 138 && mouseX <= x + 162 && mouseY >= y + 45 && mouseY <= y + 70) {
            if (this.menu.isFabricating()) {
                int progress = this.menu.getProgress();
                int maxProgress = this.menu.getMaxProgress();
                double remainingSeconds = (double) (maxProgress - progress) / 20.0;
                int percent = (int) ((double) progress * 100.0 / maxProgress);
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§b⚙️ Fabrication in Progress..."),
                        Component.literal(String.format("§fProgress: §a%d%% §7(%.1fs remaining)", percent, remainingSeconds)),
                        Component.literal("§8Hydraulic tooling and alignment in progress.")
                ), mouseX, mouseY);
            } else if (this.menu.canFabricate()) {
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§a🛠️ Assemble / Apply Upgrades"),
                        Component.literal("§7Click to start tiered assembly timer!"),
                        Component.literal("§8Higher tier components require longer precision fabrication.")
                ), mouseX, mouseY);
            } else {
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6🛠️ Vehicle Assembly Bay"),
                        Component.literal("§cRequired: Seat, Engine, Chassis, Suspension (4), Tires (4)."),
                        Component.literal("§7Or place an existing ATV in the top slot to modify.")
                ), mouseX, mouseY);
            }
        }
    }
}
