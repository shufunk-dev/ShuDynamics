package net.enchantedwood.screen;

import net.enchantedwood.EnchantedWoodMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

@Environment(EnvType.CLIENT)
public class ModularSuitScreen extends AbstractContainerScreen<ModularSuitScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/modular_suit_gui.png");

    private Button headTab;
    private Button chestTab;
    private Button legsTab;
    private Button bootsTab;
    private Button repairTab;
    private Button suitBayTab;

    public ModularSuitScreen(ModularSuitScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.titleLabelX = 8;
        this.titleLabelY = 4;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        int tabW = 41;
        int tabH = 14;
        int tabY = y + 14;

        this.headTab = Button.builder(Component.literal("🪖 Head"), button -> selectTab(0))
                .bounds(x + 5, tabY, tabW, tabH).build();
        this.chestTab = Button.builder(Component.literal("🛡️ Chest"), button -> selectTab(1))
                .bounds(x + 47, tabY, tabW, tabH).build();
        this.legsTab = Button.builder(Component.literal("👖 Legs"), button -> selectTab(2))
                .bounds(x + 89, tabY, tabW, tabH).build();
        this.bootsTab = Button.builder(Component.literal("🥾 Boots"), button -> selectTab(3))
                .bounds(x + 131, tabY, tabW, tabH).build();

        // Top Navigation Tabs (Repair & Suit Bay when linked to Powered Anvil)
        this.repairTab = Button.builder(Component.literal("⚡ Repair"), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 4);
            }
        }).bounds(x + 5, y - 16, 80, 16).build();

        this.suitBayTab = Button.builder(Component.literal("🛠️ Suit Bay"), button -> {})
                .bounds(x + 88, y - 16, 80, 16).build();
        this.suitBayTab.active = false;

        this.addRenderableWidget(this.headTab);
        this.addRenderableWidget(this.chestTab);
        this.addRenderableWidget(this.legsTab);
        this.addRenderableWidget(this.bootsTab);
        this.addRenderableWidget(this.repairTab);
        this.addRenderableWidget(this.suitBayTab);
    }

    private void selectTab(int tabIndex) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, tabIndex);
            this.menu.loadTab(tabIndex);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);

        boolean hasAnvil = this.menu.hasAnvilLinked();
        this.repairTab.visible = hasAnvil;
        this.suitBayTab.visible = hasAnvil;

        // Empty Slot Tooltip Guides
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 4) {
            int tab = this.menu.getActiveTab();
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, java.util.List.of(
                        Component.literal("§e🔋 Battery / Power Cell Slot"),
                        Component.literal("§7Accepts: §fCopper, Aluminum, Steel, Tungsten Battery, or Infinite Matrix"),
                        Component.literal("§8Provides internal energy capacity (or limitless FE) to this armor piece.")
                ), mouseX, mouseY);
                case 1 -> {
                    java.util.List<Component> chipTooltip = switch (tab) {
                        case 0 -> java.util.List.of(
                                Component.literal("§b💻 Logic Core / CPU Slot"),
                                Component.literal("§7Accepts: §fBasic Chip, Advanced Processor, Quantum Core"),
                                Component.literal("§8Controls HUD diagnostics and night vision timing.")
                        );
                        case 1 -> java.util.List.of(
                                Component.literal("§b💻 Logic Core / CPU Slot"),
                                Component.literal("§7Accepts: §fBasic Chip, Advanced Processor, Quantum Core"),
                                Component.literal("§8Optimizes thruster fuel burn and energy flow.")
                        );
                        case 2 -> java.util.List.of(
                                Component.literal("§b💻 Logic Core / CPU Slot"),
                                Component.literal("§7Accepts: §fBasic Chip, Advanced Processor, Quantum Core"),
                                Component.literal("§8Unlocks enhanced sprinting speed multiplier.")
                        );
                        default -> java.util.List.of(
                                Component.literal("§b💻 Logic Core / CPU Slot"),
                                Component.literal("§7Accepts: §fBasic Chip, Advanced Processor, Quantum Core"),
                                Component.literal("§8Unlocks automatic step-assist over full blocks.")
                        );
                    };
                    context.setComponentTooltipForNextFrame(this.font, chipTooltip, mouseX, mouseY);
                }
                case 2, 3 -> {
                    String slotName = this.hoveredSlot.index == 2 ? "A" : "B";
                    java.util.List<Component> moduleTooltip = switch (tab) {
                        case 0 -> java.util.List.of(
                                Component.literal("§a⚙️ Module Slot " + slotName),
                                Component.literal("§7Accepts: §fAdaptive Night Vision HUD"),
                                Component.literal("§7or §fNanite Auto-Repair Matrix"),
                                Component.literal("§8Hardware upgrade socket for helmet sensory & auto-repair systems.")
                        );
                        case 1 -> java.util.List.of(
                                Component.literal("§a⚙️ Module Slot " + slotName),
                                Component.literal("§7Accepts: §fHydrogen Thrusters§7, §fIon Repulsors§7,"),
                                Component.literal("§fFluoropolymer Acid Plating§7, §fThermal Refractory Plating§7,"),
                                Component.literal("§7or §fNanite Auto-Repair Matrix"),
                                Component.literal("§8Hardware socket for flight, propulsion & environmental shields.")
                        );
                        case 2 -> java.util.List.of(
                                Component.literal("§a⚙️ Module Slot " + slotName),
                                Component.literal("§7Accepts: §fSpeed Servo Leg Module§7,"),
                                Component.literal("§fFluoropolymer Acid Plating§7, §fThermal Refractory Plating§7,"),
                                Component.literal("§7or §fNanite Auto-Repair Matrix"),
                                Component.literal("§8Hardware upgrade socket for locomotive kinetic enhancement & hazard plating.")
                        );
                        default -> java.util.List.of(
                                Component.literal("§a⚙️ Module Slot " + slotName),
                                Component.literal("§7Accepts: §fHydraulic Step-Assist§7, §fHigh-Jump Actuators§7,"),
                                Component.literal("§7or §fNanite Auto-Repair Matrix"),
                                Component.literal("§8Hardware upgrade socket for vertical mobility & terrain clearance.")
                        );
                    };
                    context.setComponentTooltipForNextFrame(this.font, moduleTooltip, mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Draw top tab backdrops when linked to an Anvil
        if (this.menu.hasAnvilLinked()) {
            context.fill(x + 4, y - 17, x + 86, y, 0xFF222222);
            context.fill(x + 87, y - 17, x + 169, y, 0xFF3C3C3C);
        }

        // Clean container background
        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // Highlight active tab
        int active = this.menu.getActiveTab();
        int tabW = 41;
        int tabX = x + 5 + active * 42;
        context.fill(tabX - 1, y + 13, tabX + tabW + 1, y + 29, 0xFF00E5FF);

        // Draw Energy Meter inside recess
        int energy = this.menu.getCurrentPieceEnergy();
        int maxEnergy = this.menu.getCurrentPieceMaxEnergy();
        int meterX = x + 44;
        int meterY = y + 31;
        int meterW = 104;
        int meterH = 9;

        if (maxEnergy > 0 && energy > 0) {
            int fillW = Math.min(meterW, (int) ((long) energy * meterW / maxEnergy));
            context.fill(meterX, meterY, meterX + fillW, meterY + meterH, 0xFF00E5FF);
        }

        // Show warnings if no suit piece is equipped in this slot
        if (!this.menu.hasPieceEquipped(active)) {
            context.fill(x + 20, y + 64, x + 156, y + 78, 0xCC200000);
            context.text(this.font, Component.literal("⚠ No Modular Piece Equipped"), x + 23, y + 67, 0xFF5555, false);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x00E5FF, false);
        context.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        // Slot Labels
        context.text(this.font, Component.literal("Bat"), 45, 64, 0xAAAAAA, false);
        context.text(this.font, Component.literal("Chip"), 73, 64, 0xAAAAAA, false);
        context.text(this.font, Component.literal("ModA"), 102, 64, 0xAAAAAA, false);
        context.text(this.font, Component.literal("ModB"), 130, 64, 0xAAAAAA, false);

        // Energy text overlay on meter
        int energy = this.menu.getCurrentPieceEnergy();
        int maxEnergy = this.menu.getCurrentPieceMaxEnergy();
        String energyStr = maxEnergy > 0 ? String.format("%d / %d FE", energy, maxEnergy) : "No Battery";
        var matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate(45.0f, 32.0f);
        matrices.scale(0.7f, 0.7f);
        context.text(this.font, Component.literal(energyStr), 0, 0, 0xFFFFFF, true);
        matrices.popMatrix();
    }
}
