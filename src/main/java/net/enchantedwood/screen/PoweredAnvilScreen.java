package net.enchantedwood.screen;

import net.enchantedwood.EnchantedWoodMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class PoweredAnvilScreen extends HandledScreen<PoweredAnvilScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/powered_anvil_gui.png");

    private ButtonWidget repairButton;
    private ButtonWidget repairTab;
    private ButtonWidget suitBayTab;

    public PoweredAnvilScreen(PoweredAnvilScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        this.titleX = 8;
        this.titleY = 5;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Top Navigation Tabs
        this.repairTab = ButtonWidget.builder(Text.literal("⚡ Repair"), button -> {})
                .dimensions(x + 5, y - 16, 80, 16).build();
        this.repairTab.active = false;

        this.suitBayTab = ButtonWidget.builder(Text.literal("🛠️ Suit Bay"), button -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 1);
            }
        }).dimensions(x + 88, y - 16, 80, 16).build();

        // ⚡ Repair Button
        this.repairButton = ButtonWidget.builder(Text.literal("⚡ Repair"), button -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 0);
            }
        }).dimensions(x + 104, y + 44, 58, 18).build();

        this.addDrawableChild(this.repairTab);
        this.addDrawableChild(this.suitBayTab);
        this.addDrawableChild(this.repairButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        if (this.focusedSlot != null && !this.focusedSlot.hasStack() && this.focusedSlot.id < 2) {
            switch (this.focusedSlot.id) {
                case 0 -> context.drawTooltip(this.textRenderer, java.util.List.of(
                        Text.literal("§e🛠️ Damaged Equipment Slot"),
                        Text.literal("§7Place damaged tools, weapons, or Modular Power Armor here."),
                        Text.literal("§8Repairs using 2,500 FE without any XP prior-work penalty.")
                ), mouseX, mouseY);
                case 1 -> context.drawTooltip(this.textRenderer, java.util.List.of(
                        Text.literal("§e🔩 Repair Material Slot"),
                        Text.literal("§7Place matching repair material or duplicate item:"),
                        Text.literal("§f• Titanium Ingots §7(for Modular Power Armor)"),
                        Text.literal("§f• Iron, Steel, Diamonds, Netherite §7(for tools/armor)"),
                        Text.literal("§8Consumes 1 unit per repair cycle (+25% durability restored).")
                ), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Top tab backdrops
        context.fill(x + 4, y - 17, x + 86, y, 0xFF3C3C3C);
        context.fill(x + 87, y - 17, x + 169, y, 0xFF222222);

        // Clean base container background without chest grid lines
        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // Vertical Energy Gauge inside recess (x + 13, y + 21, w: 12, h: 48)
        int energy = this.handler.getEnergy();
        int maxEnergy = this.handler.getMaxEnergy();
        int gx = x + 14;
        int gy = y + 22;
        int gw = 10;
        int gh = 46;

        if (maxEnergy > 0 && energy > 0) {
            int fillH = Math.min(gh, (int) ((long) energy * gh / maxEnergy));
            int fy = (gy + gh) - fillH;
            context.fill(gx, fy, gx + gw, gy + gh, 0xFF00E5FF);
        }

        // Draw Anvil Plus Sign between input & material slots
        context.drawText(this.textRenderer, Text.literal("+"), x + 61, y + 49, 0x555555, false);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, Text.literal("⚡ Powered Anvil"), 8, 6, 0x00E5FF, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0x404040, false);

        context.drawText(this.textRenderer, Text.literal("Item"), 38, 33, 0x555555, false);
        context.drawText(this.textRenderer, Text.literal("Ingot"), 76, 33, 0x555555, false);

        int energy = this.handler.getEnergy();
        boolean hasEnergy = energy >= 2500;
        ItemStack input = this.handler.getInputStack();
        boolean hasDamaged = !input.isEmpty() && input.isDamaged();

        if (hasDamaged) {
            context.drawText(this.textRenderer, Text.literal("Cost: 2,500 FE"), 105, 33, hasEnergy ? 0x228822 : 0xAA2222, false);
        }

        // Tooltip for Energy Gauge
        int relX = mouseX - ((this.width - this.backgroundWidth) / 2);
        int relY = mouseY - ((this.height - this.backgroundHeight) / 2);
        if (relX >= 13 && relX <= 25 && relY >= 21 && relY <= 69) {
            context.drawTooltip(this.textRenderer, Text.literal(String.format("§e⚡ Energy: §f%,d / %,d FE", energy, this.handler.getMaxEnergy())), relX, relY);
        }
    }
}

