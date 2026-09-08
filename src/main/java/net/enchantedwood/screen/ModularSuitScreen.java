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

@Environment(EnvType.CLIENT)
public class ModularSuitScreen extends HandledScreen<ModularSuitScreenHandler> {
    private static final Identifier GENERIC_GUI = Identifier.ofVanilla("textures/gui/container/generic_54.png");

    private ButtonWidget headTab;
    private ButtonWidget chestTab;
    private ButtonWidget legsTab;
    private ButtonWidget bootsTab;

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

        this.addDrawableChild(this.headTab);
        this.addDrawableChild(this.chestTab);
        this.addDrawableChild(this.legsTab);
        this.addDrawableChild(this.bootsTab);
    }

    private void selectTab(int tabIndex) {
        if (this.client != null && this.client.interactionManager != null) {
            this.client.interactionManager.clickButton(this.handler.syncId, tabIndex);
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Base container background
        context.drawTexture(RenderPipelines.GUI_TEXTURED, GENERIC_GUI, x, y, 0.0f, 0.0f, this.backgroundWidth, 71, 256, 256);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, GENERIC_GUI, x, y + 71, 0.0f, 125.0f, this.backgroundWidth, 95, 256, 256);

        // Highlight active tab
        int active = this.handler.getActiveTab();
        int tabW = 41;
        int tabX = x + 5 + active * 42;
        context.fill(tabX - 1, y + 13, tabX + tabW + 1, y + 29, 0xFF00E5FF);

        // Draw Energy Meter
        int energy = this.handler.getCurrentPieceEnergy();
        int maxEnergy = this.handler.getCurrentPieceMaxEnergy();
        int meterX = x + 44;
        int meterY = y + 31;
        int meterW = 104;
        int meterH = 9;

        context.fill(meterX - 1, meterY - 1, meterX + meterW + 1, meterY + meterH + 1, 0xFF10141A);
        context.fill(meterX, meterY, meterX + meterW, meterY + meterH, 0xFF1F2937);

        if (maxEnergy > 0 && energy > 0) {
            int fillW = Math.min(meterW, (int) ((long) energy * meterW / maxEnergy));
            context.fill(meterX, meterY, meterX + fillW, meterY + meterH, 0xFF00E5FF);
        }

        // Draw Slot Boxes
        drawSlot(context, x + 43, y + 44);  // Battery
        drawSlot(context, x + 71, y + 44);  // Logic Core
        drawSlot(context, x + 103, y + 44); // Module A
        drawSlot(context, x + 131, y + 44); // Module B

        // Show warnings if no suit piece is equipped in this slot
        if (!this.handler.hasPieceEquipped(active)) {
            context.fill(x + 20, y + 64, x + 156, y + 78, 0xCC200000);
            context.drawText(this.textRenderer, Text.literal("⚠ No Modular Piece Equipped"), x + 23, y + 67, 0xFF5555, false);
        }
    }

    private void drawSlot(DrawContext context, int sx, int sy) {
        context.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
        context.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
        context.fill(sx + 1, sy + 1, sx + 16, sy + 16, 0xFF1E1E1E);
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
