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

        // Massive Multi-Fluid Reservoir Gauge (width = 36, height = 52, at x + 70, y + 20)
        int currentLava = this.handler.getLavaAmount();
        int maxLava = this.handler.getMaxLava();
        if (maxLava > 0 && currentLava > 0) {
            int fluidHeight = (int) ((long) currentLava * 52 / maxLava);
            if (fluidHeight > 0) {
                net.enchantedwood.fluid.MoltenMetal fluid = this.handler.getFluidType();
                if (fluid == net.enchantedwood.fluid.MoltenMetal.LAVA || fluid == net.enchantedwood.fluid.MoltenMetal.NONE) {
                    // UV for fluid texture at (176, 52 - fluidHeight) with width 36
                    context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 70, y + 72 - fluidHeight, 176.0f, 52.0f - fluidHeight, 36, fluidHeight, 256, 256);
                } else {
                    int color = fluid.getColor() | 0xFF000000;
                    context.fill(x + 70, y + 72 - fluidHeight, x + 70 + 36, y + 72, color);
                }
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

        // Fluid Filter / Lock Status Badge
        net.enchantedwood.fluid.MoltenMetal filter = this.handler.getFilterFluid();
        if (filter != null && filter != net.enchantedwood.fluid.MoltenMetal.NONE) {
            context.drawText(this.textRenderer, Text.literal("§6🔒 " + filter.getDisplayName()), 114, 16, 0xFFFFFF, false);
        } else {
            context.drawText(this.textRenderer, Text.literal("§7🔓 Any Fluid"), 114, 16, 0x888888, false);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Tooltip for Reservoir (x + 70 to x + 106, y + 20 to y + 72)
        if (mouseX >= x + 70 && mouseX <= x + 106 && mouseY >= y + 20 && mouseY <= y + 72) {
            int current = this.handler.getLavaAmount();
            int max = this.handler.getMaxLava();
            int buckets = current / 1000;
            int maxBuckets = max / 1000;
            net.enchantedwood.fluid.MoltenMetal fluid = this.handler.getFluidType();
            String title = (fluid != null && fluid != net.enchantedwood.fluid.MoltenMetal.NONE)
                    ? "§6" + fluid.getDisplayName() + " Reservoir"
                    : "§6Titanium Multi-Fluid Reservoir (Empty)";
            List<Text> tooltip = new java.util.ArrayList<>();
            tooltip.add(Text.literal(title));
            tooltip.add(Text.literal(String.format("§e%,d / %,d mB", current, max)));
            tooltip.add(Text.literal(String.format("§7(%d / %d Buckets)", buckets, maxBuckets)));
            if (fluid != null && fluid != net.enchantedwood.fluid.MoltenMetal.NONE) {
                tooltip.add(Text.literal("§dFluid Stored: §f" + fluid.getDisplayName()));
            } else {
                tooltip.add(Text.literal("§7Accepts Lava or any of 14 Molten Metals"));
            }

            net.enchantedwood.fluid.MoltenMetal activeFilter = this.handler.getFilterFluid();
            if (activeFilter != null && activeFilter != net.enchantedwood.fluid.MoltenMetal.NONE) {
                tooltip.add(Text.literal("§a🔒 Filter Locked: §f" + activeFilter.getDisplayName()));
            } else {
                tooltip.add(Text.literal("§7🔓 Filter: Unlocked (Accepts any fluid)"));
            }
            tooltip.add(Text.literal("§8Sneak-click with an ingot to lock fluid"));
            tooltip.add(Text.literal("§8Sneak-click with empty hand to unlock"));
            tooltip.add(Text.literal("§8Inbound: Top Center Valve"));
            tooltip.add(Text.literal("§8Outbound: All Outer Casings"));
            context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.focusedSlot != null && !this.focusedSlot.hasStack() && this.focusedSlot.id < 2) {
            switch (this.focusedSlot.id) {
                case 0 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§e🪣 Lava Bucket Fill / Drain Input"),
                        Text.literal("§7Insert empty buckets to drain lava, or"),
                        Text.literal("§7insert filled lava buckets to fill the reservoir."),
                        Text.literal("§8(Molten metals are piped in/out via Titanium Pipes)")
                ), mouseX, mouseY);
                case 1 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§a✨ Processed Bucket Output"),
                        Text.literal("§7Filled or emptied buckets appear here.")
                ), mouseX, mouseY);
            }
        }
    }
}
