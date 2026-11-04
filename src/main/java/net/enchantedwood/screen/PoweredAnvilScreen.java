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
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class PoweredAnvilScreen extends AbstractContainerScreen<PoweredAnvilScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/powered_anvil_gui.png");

    private Button repairButton;
    private Button repairTab;
    private Button suitBayTab;

    public PoweredAnvilScreen(PoweredAnvilScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.titleLabelX = 8;
        this.titleLabelY = 7;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Top Navigation Tabs
        this.repairTab = Button.builder(Component.literal("⚡ Repair"), button -> {})
                .bounds(x + 5, y - 16, 80, 16).build();
        this.repairTab.active = false;

        this.suitBayTab = Button.builder(Component.literal("🛠️ Suit Bay"), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
            }
        }).bounds(x + 88, y - 16, 80, 16).build();

        // ⚡ Repair Button
        this.repairButton = Button.builder(Component.literal("⚡ Repair"), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            }
        }).bounds(x + 104, y + 44, 58, 18).build();

        this.addRenderableWidget(this.repairTab);
        this.addRenderableWidget(this.suitBayTab);
        this.addRenderableWidget(this.repairButton);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Top tab backdrops
        context.fill(x + 4, y - 17, x + 86, y, 0xFF3C3C3C);
        context.fill(x + 87, y - 17, x + 169, y, 0xFF222222);

        // Base container background
        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // Vertical Textured Electric Energy Gauge (x + 13, y + 21, w: 12, h: 48) using UV at u=192, v=0
        int energy = this.menu.getEnergy();
        int maxEnergy = this.menu.getMaxEnergy();
        if (maxEnergy > 0 && energy > 0) {
            int fillH = Math.min(48, (int) ((long) energy * 48 / maxEnergy));
            if (fillH > 0) {
                context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 13, y + 69 - fillH, 192.0f, 48.0f - fillH, 12, fillH, 256, 256);
            }
        }

        // Draw Anvil Plus Sign between input & material slots
        context.text(this.font, Component.literal("§e+"), x + 61, y + 49, 0xFFDD00, false);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.text(this.font, Component.literal("⚡ Powered Anvil"), 8, 7, 0xFF00E5FF, false);
        context.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF373A40, false);

        context.text(this.font, Component.literal("Item"), 38, 33, 0x555555, false);
        context.text(this.font, Component.literal("Ingot"), 74, 33, 0x555555, false);

        int energy = this.menu.getEnergy();
        boolean hasEnergy = energy >= 2500;
        ItemStack input = this.menu.getInputStack();
        boolean hasDamaged = !input.isEmpty() && input.isDamaged();

        if (hasDamaged) {
            context.text(this.font, Component.literal("Cost: 2,500 FE"), 105, 33, hasEnergy ? 0x008000 : 0xAA0000, false);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Tooltip for Energy Gauge (x: 12..26, y: 20..70)
        if (mouseX >= x + 12 && mouseX <= x + 26 && mouseY >= y + 20 && mouseY <= y + 70) {
            int energy = this.menu.getEnergy();
            context.setComponentTooltipForNextFrame(this.font, java.util.List.of(
                    Component.literal("§e⚡ High-Voltage Repair Capacitor"),
                    Component.literal(String.format("§b%,d / %,d FE", energy, this.menu.getMaxEnergy())),
                    Component.literal("§7Cost per Repair Cycle: §f2,500 FE"),
                    Component.literal("§aZero XP Prior-Work Penalty!")
            ), mouseX, mouseY);
        }

        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 2) {
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, java.util.List.of(
                        Component.literal("§e🛠️ Damaged Equipment Slot"),
                        Component.literal("§7Place damaged tools, weapons, or Modular Power Armor here."),
                        Component.literal("§8Repairs using 2,500 FE without any XP prior-work penalty.")
                ), mouseX, mouseY);
                case 1 -> context.setComponentTooltipForNextFrame(this.font, java.util.List.of(
                        Component.literal("§e🔩 Repair Material Slot"),
                        Component.literal("§7Place matching repair material or duplicate item:"),
                        Component.literal("§f• Titanium Ingots §7(for Modular Power Armor)"),
                        Component.literal("§f• Iron, Steel, Diamonds, Netherite §7(for tools/armor)"),
                        Component.literal("§8Consumes 1 unit per repair cycle (+25% durability restored).")
                ), mouseX, mouseY);
            }
        }
    }
}
