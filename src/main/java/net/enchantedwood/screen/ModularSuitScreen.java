package net.enchantedwood.screen;

import net.enchantedwood.EnchantedWoodMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ModularSuitScreen extends HandledScreen<ModularSuitScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/modular_suit_gui.png");

    private ButtonWidget headTab;
    private ButtonWidget chestTab;
    private ButtonWidget legsTab;
    private ButtonWidget bootsTab;
    private ButtonWidget repairTab;
    private ButtonWidget suitBayTab;

    public ModularSuitScreen(ModularSuitScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        this.titleX = 8;
        this.titleY = 4;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        int tabW = 41;
        int tabH = 14;
        int tabY = y + 14;

        this.headTab = ButtonWidget.builder(Text.literal("🪖 Head"), button -> selectTab(0))
                .dimensions(x + 5, tabY, tabW, tabH).build();
        this.chestTab = ButtonWidget.builder(Text.literal("🛡️ Chest"), button -> selectTab(1))
                .dimensions(x + 47, tabY, tabW, tabH).build();
        this.legsTab = ButtonWidget.builder(Text.literal("👖 Legs"), button -> selectTab(2))
                .dimensions(x + 89, tabY, tabW, tabH).build();
        this.bootsTab = ButtonWidget.builder(Text.literal("🥾 Boots"), button -> selectTab(3))
                .dimensions(x + 131, tabY, tabW, tabH).build();

        // Top Navigation Tabs (Repair & Suit Bay when linked to Powered Anvil)
        this.repairTab = ButtonWidget.builder(Text.literal("⚡ Repair"), button -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 4);
            }
        }).dimensions(x + 5, y - 16, 80, 16).build();

        this.suitBayTab = ButtonWidget.builder(Text.literal("🛠️ Suit Bay"), button -> {})
                .dimensions(x + 88, y - 16, 80, 16).build();
        this.suitBayTab.active = false;

        this.addDrawableChild(this.headTab);
        this.addDrawableChild(this.chestTab);
        this.addDrawableChild(this.legsTab);
        this.addDrawableChild(this.bootsTab);
        this.addDrawableChild(this.repairTab);
        this.addDrawableChild(this.suitBayTab);
    }

    private void selectTab(int tabIndex) {
        if (this.client != null && this.client.interactionManager != null) {
            this.client.interactionManager.clickButton(this.handler.syncId, tabIndex);
            this.handler.loadTab(tabIndex);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hasAnvil = this.handler.hasAnvilLinked();
        this.repairTab.visible = hasAnvil;
        this.suitBayTab.visible = hasAnvil;
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        // Empty Slot Tooltip Guides
        if (this.focusedSlot != null && !this.focusedSlot.hasStack() && this.focusedSlot.id < 4) {
            int tab = this.handler.getActiveTab();
            switch (this.focusedSlot.id) {
                case 0 -> context.drawTooltip(this.textRenderer, java.util.List.of(
                        Text.literal("§e🔋 Battery / Power Cell Slot"),
                        Text.literal("§7Accepts: §fCopper, Aluminum, Steel, or Tungsten Battery"),
                        Text.literal("§8Provides internal energy capacity to this armor piece.")
                ), mouseX, mouseY);
                case 1 -> {
                    java.util.List<Text> chipTooltip = switch (tab) {
                        case 0 -> java.util.List.of(
                                Text.literal("§b💻 Logic Core / CPU Slot"),
                                Text.literal("§7Accepts: §fBasic Chip, Advanced Processor, Quantum Core"),
                                Text.literal("§8Controls HUD diagnostics and night vision timing.")
                        );
                        case 1 -> java.util.List.of(
                                Text.literal("§b💻 Logic Core / CPU Slot"),
                                Text.literal("§7Accepts: §fBasic Chip, Advanced Processor, Quantum Core"),
                                Text.literal("§8Optimizes thruster fuel burn and energy flow.")
                        );
                        case 2 -> java.util.List.of(
                                Text.literal("§b💻 Logic Core / CPU Slot"),
                                Text.literal("§7Accepts: §fBasic Chip, Advanced Processor, Quantum Core"),
                                Text.literal("§8Unlocks enhanced sprinting speed multiplier.")
                        );
                        default -> java.util.List.of(
                                Text.literal("§b💻 Logic Core / CPU Slot"),
                                Text.literal("§7Accepts: §fBasic Chip, Advanced Processor, Quantum Core"),
                                Text.literal("§8Unlocks automatic step-assist over full blocks.")
                        );
                    };
                    context.drawTooltip(this.textRenderer, chipTooltip, mouseX, mouseY);
                }
                case 2, 3 -> {
                    String slotName = this.focusedSlot.id == 2 ? "A" : "B";
                    java.util.List<Text> moduleTooltip = switch (tab) {
                        case 0 -> java.util.List.of(
                                Text.literal("§a⚙️ Module Slot " + slotName),
                                Text.literal("§7Accepts: §fAdaptive Night Vision HUD"),
                                Text.literal("§7or §fNanite Auto-Repair Matrix"),
                                Text.literal("§8Hardware upgrade socket for helmet sensory & auto-repair systems.")
                        );
                        case 1 -> java.util.List.of(
                                Text.literal("§a⚙️ Module Slot " + slotName),
                                Text.literal("§7Accepts: §fHydrogen Thrusters§7, §fIon Repulsors§7,"),
                                Text.literal("§fFluoropolymer Acid Plating§7, §fThermal Refractory Plating§7,"),
                                Text.literal("§7or §fNanite Auto-Repair Matrix"),
                                Text.literal("§8Hardware socket for flight, propulsion & environmental shields.")
                        );
                        case 2 -> java.util.List.of(
                                Text.literal("§a⚙️ Module Slot " + slotName),
                                Text.literal("§7Accepts: §fSpeed Servo Leg Module§7,"),
                                Text.literal("§fFluoropolymer Acid Plating§7, §fThermal Refractory Plating§7,"),
                                Text.literal("§7or §fNanite Auto-Repair Matrix"),
                                Text.literal("§8Hardware upgrade socket for locomotive kinetic enhancement & hazard plating.")
                        );
                        default -> java.util.List.of(
                                Text.literal("§a⚙️ Module Slot " + slotName),
                                Text.literal("§7Accepts: §fHydraulic Step-Assist§7, §fHigh-Jump Actuators§7,"),
                                Text.literal("§7or §fNanite Auto-Repair Matrix"),
                                Text.literal("§8Hardware upgrade socket for vertical mobility & terrain clearance.")
                        );
                    };
                    context.drawTooltip(this.textRenderer, moduleTooltip, mouseX, mouseY);
                }
            }
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Draw top tab backdrops when linked to an Anvil
        if (this.handler.hasAnvilLinked()) {
            context.fill(x + 4, y - 17, x + 86, y, 0xFF222222);
            context.fill(x + 87, y - 17, x + 169, y, 0xFF3C3C3C);
        }

        // Clean container background
        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // Highlight active tab
        int active = this.handler.getActiveTab();
        int tabW = 41;
        int tabX = x + 5 + active * 42;
        context.fill(tabX - 1, y + 13, tabX + tabW + 1, y + 29, 0xFF00E5FF);

        // Draw Energy Meter inside recess
        int energy = this.handler.getCurrentPieceEnergy();
        int maxEnergy = this.handler.getCurrentPieceMaxEnergy();
        int meterX = x + 44;
        int meterY = y + 31;
        int meterW = 104;
        int meterH = 9;

        if (maxEnergy > 0 && energy > 0) {
            int fillW = Math.min(meterW, (int) ((long) energy * meterW / maxEnergy));
            context.fill(meterX, meterY, meterX + fillW, meterY + meterH, 0xFF00E5FF);
        }

        // Show warnings if no suit piece is equipped in this slot
        if (!this.handler.hasPieceEquipped(active)) {
            context.fill(x + 20, y + 64, x + 156, y + 78, 0xCC200000);
            context.drawText(this.textRenderer, Text.literal("⚠ No Modular Piece Equipped"), x + 23, y + 67, 0xFF5555, false);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0x00E5FF, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0x404040, false);

        // Slot Labels
        context.drawText(this.textRenderer, Text.literal("Bat"), 45, 64, 0xAAAAAA, false);
        context.drawText(this.textRenderer, Text.literal("Chip"), 73, 64, 0xAAAAAA, false);
        context.drawText(this.textRenderer, Text.literal("ModA"), 102, 64, 0xAAAAAA, false);
        context.drawText(this.textRenderer, Text.literal("ModB"), 130, 64, 0xAAAAAA, false);

        // Energy text overlay on meter
        int energy = this.handler.getCurrentPieceEnergy();
        int maxEnergy = this.handler.getCurrentPieceMaxEnergy();
        String energyStr = maxEnergy > 0 ? String.format("%d / %d FE", energy, maxEnergy) : "No Battery";
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(45.0f, 32.0f);
        matrices.scale(0.7f, 0.7f);
        context.drawText(this.textRenderer, Text.literal(energyStr), 0, 0, 0xFFFFFF, true);
        matrices.popMatrix();
    }
}
