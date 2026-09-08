package net.enchantedwood.screen;

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
    private static final Identifier GENERIC_GUI = Identifier.ofVanilla("textures/gui/container/generic_54.png");

    private ButtonWidget repairButton;
    private ButtonWidget repairTab;
    private ButtonWidget suitBayTab;
    private ButtonWidget suitBayHeaderTab;

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

        // In-window Suit Bay Tab Button
        this.suitBayHeaderTab = ButtonWidget.builder(Text.literal("🛠️ Suit Bay"), button -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 1);
            }
        }).dimensions(x + 95, y + 4, 73, 14).build();

        // ⚡ Repair Button
        this.repairButton = ButtonWidget.builder(Text.literal("⚡ Repair"), button -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 0);
            }
        }).dimensions(x + 104, y + 44, 58, 18).build();

        this.addDrawableChild(this.repairTab);
        this.addDrawableChild(this.suitBayTab);
        this.addDrawableChild(this.suitBayHeaderTab);
        this.addDrawableChild(this.repairButton);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Top tab backdrops
        context.fill(x + 4, y - 17, x + 86, y, 0xFF3C3C3C);
        context.fill(x + 87, y - 17, x + 169, y, 0xFF222222);

        // Base container background
        context.drawTexture(RenderPipelines.GUI_TEXTURED, GENERIC_GUI, x, y, 0.0f, 0.0f, this.backgroundWidth, 71, 256, 256);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, GENERIC_GUI, x, y + 71, 0.0f, 125.0f, this.backgroundWidth, 95, 256, 256);

        // Vertical Energy Gauge (x + 12, y + 22, w: 10, h: 48)
        int energy = this.handler.getEnergy();
        int maxEnergy = this.handler.getMaxEnergy();
        int gx = x + 14;
        int gy = y + 22;
        int gw = 10;
        int gh = 46;

        context.fill(gx - 1, gy - 1, gx + gw + 1, gy + gh + 1, 0xFF111827);
        context.fill(gx, gy, gx + gw, gy + gh, 0xFF1F2937);

        if (maxEnergy > 0 && energy > 0) {
            int fillH = Math.min(gh, (int) ((long) energy * gh / maxEnergy));
            int fy = (gy + gh) - fillH;
            context.fill(gx, fy, gx + gw, gy + gh, 0xFF00E5FF);
        }

        // Draw Slot Boxes
        drawSlot(context, x + 37, y + 44); // Input slot
        drawSlot(context, x + 75, y + 44); // Material slot

        // Draw Anvil Plus Sign
        context.drawText(this.textRenderer, Text.literal("+"), x + 61, y + 49, 0x888888, false);

        // Active Repair Tab Pill highlight
        context.fill(x + 7, y + 3, x + 85, y + 17, 0x3300E5FF);
    }

    private void drawSlot(DrawContext context, int sx, int sy) {
        context.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
        context.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
        context.fill(sx + 1, sy + 1, sx + 16, sy + 16, 0xFF1E1E1E);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, Text.literal("⚡ Powered Anvil"), 8, 6, 0x00E5FF, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0x404040, false);

        context.drawText(this.textRenderer, Text.literal("Item"), 38, 33, 0xAAAAAA, false);
        context.drawText(this.textRenderer, Text.literal("Ingot"), 76, 33, 0xAAAAAA, false);

        int energy = this.handler.getEnergy();
        boolean hasEnergy = energy >= 2500;
        ItemStack input = this.handler.getInputStack();
        boolean hasDamaged = !input.isEmpty() && input.isDamaged();

        if (hasDamaged) {
            context.drawText(this.textRenderer, Text.literal("Cost: 2,500 FE"), 105, 33, hasEnergy ? 0x55FF55 : 0xFF5555, false);
        }

        // Tooltip for Energy Gauge
        int relX = mouseX - ((this.width - this.backgroundWidth) / 2);
        int relY = mouseY - ((this.height - this.backgroundHeight) / 2);
        if (relX >= 14 && relX <= 24 && relY >= 22 && relY <= 68) {
            context.drawTooltip(this.textRenderer, Text.literal(String.format("§e⚡ Energy: §f%,d / %,d FE", energy, this.handler.getMaxEnergy())), relX, relY);
        }
    }
}
