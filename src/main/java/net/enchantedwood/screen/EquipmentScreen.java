package net.enchantedwood.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;

@Environment(EnvType.CLIENT)
public class EquipmentScreen extends HandledScreen<EquipmentScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/equipment_gui.png");

    public EquipmentScreen(EquipmentScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, Text.literal("Player Equipment"));
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // Draw slot labels above slots
        context.drawText(this.textRenderer, Text.literal("§aCAPE"), x + 49, y + 16, 0xFFFFFF, true);
        context.drawText(this.textRenderer, Text.literal("§eHEART"), x + 101, y + 16, 0xFFFFFF, true);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
