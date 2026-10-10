package net.enchantedwood.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.enchantedwood.EnchantedWoodMod;

import java.util.List;

@Environment(EnvType.CLIENT)
public class TitaniumTankScreen extends AbstractContainerScreen<TitaniumTankScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/titanium_tank_gui.png");

    public TitaniumTankScreen(TitaniumTankScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Base GUI background
        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // Massive Multi-Fluid Reservoir Gauge (width = 36, height = 52, at x + 70, y + 20)
        int currentLava = this.menu.getLavaAmount();
        int maxLava = this.menu.getMaxLava();
        if (maxLava > 0 && currentLava > 0) {
            int fluidHeight = (int) ((long) currentLava * 52 / maxLava);
            if (fluidHeight > 0) {
                net.enchantedwood.fluid.MoltenMetal fluid = this.menu.getFluidType();
                if (fluid == net.enchantedwood.fluid.MoltenMetal.LAVA || fluid == net.enchantedwood.fluid.MoltenMetal.NONE) {
                    // UV for fluid texture at (176, 52 - fluidHeight) with width 36
                    context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 70, y + 72 - fluidHeight, 176.0f, 52.0f - fluidHeight, 36, fluidHeight, 256, 256);
                } else {
                    int color = fluid.getColor() | 0xFF000000;
                    context.fill(x + 70, y + 72 - fluidHeight, x + 70 + 36, y + 72, color);
                }
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.text(this.font, this.title, 8, 6, 4210752, false);
        context.text(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 4210752, false);

        // Status string
        String statusText = this.menu.isFormed() ? "§a✔ 5x5 Formed" : "§c✖ Incomplete";
        context.text(this.font, Component.literal(statusText), 114, 6, 0xFFFFFF, true);

        // Fluid Filter / Lock Status Badge
        net.enchantedwood.fluid.MoltenMetal filter = this.menu.getFilterFluid();
        if (filter != null && filter != net.enchantedwood.fluid.MoltenMetal.NONE) {
            context.text(this.font, Component.literal("§6🔒 " + filter.getDisplayName()), 114, 16, 0xFFFFFF, false);
        } else {
            context.text(this.font, Component.literal("§7🔓 Any Fluid"), 114, 16, 0x888888, false);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);


        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Tooltip for Reservoir (x + 70 to x + 106, y + 20 to y + 72)
        if (mouseX >= x + 70 && mouseX <= x + 106 && mouseY >= y + 20 && mouseY <= y + 72) {
            int current = this.menu.getLavaAmount();
            int max = this.menu.getMaxLava();
            int buckets = current / 1000;
            int maxBuckets = max / 1000;
            net.enchantedwood.fluid.MoltenMetal fluid = this.menu.getFluidType();
            String title = (fluid != null && fluid != net.enchantedwood.fluid.MoltenMetal.NONE)
                    ? "§6" + fluid.getDisplayName() + " Reservoir"
                    : "§6Titanium Multi-Fluid Reservoir (Empty)";
            List<Component> tooltip = new java.util.ArrayList<>();
            tooltip.add(Component.literal(title));
            tooltip.add(Component.literal(String.format("§e%,d / %,d mB", current, max)));
            tooltip.add(Component.literal(String.format("§7(%d / %d Buckets)", buckets, maxBuckets)));
            if (fluid != null && fluid != net.enchantedwood.fluid.MoltenMetal.NONE) {
                tooltip.add(Component.literal("§dFluid Stored: §f" + fluid.getDisplayName()));
            } else {
                tooltip.add(Component.literal("§7Accepts Lava or any of 14 Molten Metals"));
            }

            net.enchantedwood.fluid.MoltenMetal activeFilter = this.menu.getFilterFluid();
            if (activeFilter != null && activeFilter != net.enchantedwood.fluid.MoltenMetal.NONE) {
                tooltip.add(Component.literal("§a🔒 Filter Locked: §f" + activeFilter.getDisplayName()));
            } else {
                tooltip.add(Component.literal("§7🔓 Filter: Unlocked (Accepts any fluid)"));
            }
            tooltip.add(Component.literal("§8Sneak-click with an ingot to lock fluid"));
            tooltip.add(Component.literal("§8Sneak-click with empty hand to unlock"));
            tooltip.add(Component.literal("§8Inbound: Top Center Valve"));
            tooltip.add(Component.literal("§8Outbound: All Outer Casings"));
            context.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 2) {
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e🪣 Lava Bucket Fill / Drain Input"),
                        Component.literal("§7Insert empty buckets to drain lava, or"),
                        Component.literal("§7insert filled lava buckets to fill the reservoir."),
                        Component.literal("§8(Molten metals are piped in/out via Titanium Pipes)")
                ), mouseX, mouseY);
                case 1 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§a✨ Processed Bucket Output"),
                        Component.literal("§7Filled or emptied buckets appear here.")
                ), mouseX, mouseY);
            }
        }
    }
}
