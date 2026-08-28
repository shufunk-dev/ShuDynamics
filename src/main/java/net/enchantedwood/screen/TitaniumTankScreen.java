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

import java.util.List;

@Environment(EnvType.CLIENT)
public class TitaniumTankScreen extends HandledScreen<TitaniumTankScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/titanium_tank_gui.png");

    public TitaniumTankScreen(TitaniumTankScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Base GUI background
        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // Massive Lava Reservoir Gauge (width = 36, height = 52, at x + 70, y + 20)
        int currentLava = this.handler.getLavaAmount();
        int maxLava = this.handler.getMaxLava();
        if (maxLava > 0 && currentLava > 0) {
            int fluidHeight = (int) ((long) currentLava * 52 / maxLava);
            if (fluidHeight > 0) {
                // UV for fluid texture at (176, 52 - fluidHeight) with width 36
                context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 70, y + 72 - fluidHeight, 176.0f, 52.0f - fluidHeight, 36, fluidHeight, 256, 256);
            }
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, 8, 6, 4210752, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, 8, this.backgroundHeight - 94, 4210752, false);

        // Status string
        String statusText = this.handler.isFormed() ? "§a✔ 5x5 Formed" : "§c✖ Incomplete";
        context.drawText(this.textRenderer, Text.literal(statusText), 114, 6, 0xFFFFFF, true);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Tooltip for Lava Reservoir (x + 70 to x + 106, y + 20 to y + 72)
        if (mouseX >= x + 70 && mouseX <= x + 106 && mouseY >= y + 20 && mouseY <= y + 72) {
            int current = this.handler.getLavaAmount();
            int max = this.handler.getMaxLava();
            int buckets = current / 1000;
            int maxBuckets = max / 1000;
            context.drawTooltip(
                    this.textRenderer,
                    List.of(
                            Text.literal("§6Molten Lava Reservoir"),
                            Text.literal(String.format("§e%,d / %,d mB", current, max)),
                            Text.literal(String.format("§7(%d / %d Buckets)", buckets, maxBuckets)),
                            Text.literal("§8Inbound: Top Center Valve"),
                            Text.literal("§8Outbound: All Outer Casings")
                    ),
                    mouseX,
                    mouseY
            );
        }
    }
}
