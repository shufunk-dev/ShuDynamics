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
public class AlloyFoundryScreen extends AbstractContainerScreen<AlloyFoundryScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/alloy_foundry_gui.png");

    public AlloyFoundryScreen(AlloyFoundryScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.titleLabelX = 8;
        this.titleLabelY = 7;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // 1. Energy Bar at x + 18, y + 20 (width 16, height 50) using sprite at u=208, v=0
        int energyHeight = this.menu.getScaledEnergy(50);
        if (energyHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 18, y + 70 - energyHeight, 208.0f, 50.0f - energyHeight, 16, energyHeight, 256, 256);
        }

        // 2. Molten Alloy Progress Arrow at x + 86, y + 34 (width 24, height 17) using sprite at u=176, v=14
        int cookWidth = this.menu.getScaledCookProgress(24);
        if (cookWidth > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 86, y + 34, 176.0f, 14.0f, cookWidth, 17, 256, 256);

            // Active smelting molten thermal flecks at the pouring tip
            if (this.menu.isCooking()) {
                long time = System.currentTimeMillis();
                int sparkX = x + 86 + cookWidth - 1;
                int sparkY = y + 42 + (int)((time / 100) % 3) - 1;
                int sparkColor = ((time / 80) % 2 == 0) ? 0xFFFFF8D0 : 0xFFFFB300;
                context.fill(sparkX, sparkY, sparkX + 2, sparkY + 2, sparkColor);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        // Machine title in illuminated warm ivory on the dark titanium nameplate
        context.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFE8E0D0, false);

        // Player Inventory label in crisp dark slate
        context.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF373A40, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Energy Bar Tooltip
        if (mouseX >= x + 16 && mouseX <= x + 35 && mouseY >= y + 18 && mouseY <= y + 71) {
            int energy = this.menu.getEnergy();
            int max = this.menu.getMaxEnergy();
            int pct = max > 0 ? (int)(((long)energy * 100) / max) : 0;
            String status = this.menu.isCooking() ? "§6♨ Induction Heating (40 FE/t)" : (energy >= 40 ? "§a● Ready (Induction Coils Hot)" : "§c⚡ Depleted");

            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§6⚡ Induction Energy Buffer"),
                    Component.literal(String.format("§e%,d / %,d FE §7(%d%%)", energy, max, pct)),
                    Component.literal("§7Base Draw: §f40 FE/t"),
                    Component.literal("§7Status: " + status)
            ), mouseX, mouseY);
        }

        // Smelting Progress Arrow Tooltip
        if (mouseX >= x + 86 && mouseX <= x + 109 && mouseY >= y + 34 && mouseY <= y + 50) {
            if (this.menu.isCooking()) {
                int pct = this.menu.getScaledCookProgress(100);
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6♨ Induction Smelting Chamber"),
                        Component.literal(String.format("§eSmelting Progress: §f%d%%", pct)),
                        Component.literal("§7Alloying base and flux metals into casting mold.")
                ), mouseX, mouseY);
            } else {
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6♨ Induction Smelting Chamber"),
                        Component.literal("§7Insert 2 compatible metals to forge an alloy:"),
                        Component.literal("§8• Copper + Tin -> Bronze"),
                        Component.literal("§8• Cobalt + Ardite -> Manyullyn"),
                        Component.literal("§8• Tungsten + Carbon -> Tungsten Carbide")
                ), mouseX, mouseY);
            }
        }

        // Gear Tooltip (when gear installed)
        if (mouseX >= x + 150 && mouseX <= x + 170 && mouseY >= y + 6 && mouseY <= y + 26) {
            net.enchantedwood.block.custom.GearTier tier = this.menu.getGearTier();
            if (tier != net.enchantedwood.block.custom.GearTier.NONE) {
                String speedMultiplier = switch (tier) {
                    case IRON -> "1.2x";
                    case COPPER -> "1.5x";
                    case BRONZE -> "1.8x";
                    case GOLD -> "2.3x";
                    case TITANIUM -> "2.8x";
                    case DIAMOND -> "3.6x";
                    case NETHERITE -> "8.0x";
                    case BLAZE_OVERCLOCK -> "10.7x (Hyper-Melter)";
                    case INSANIUM_OVERCLOCK -> "53.3x (Instant Melt)";
                    default -> "1.0x";
                };
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§d⚙ Installed Gear Upgrade"),
                        Component.literal(String.format("§7Speed Tier: §f%s §a(%s Speed)", tier.name(), speedMultiplier)),
                        Component.literal("§8Significantly accelerates melting cycle.")
                ), mouseX, mouseY);
            }
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 4) {
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e📥 Primary Alloy Ingot Slot"),
                        Component.literal("§7Insert primary base metal:"),
                        Component.literal("§f• Copper, Tungsten, Iron, Cobalt, etc.")
                ), mouseX, mouseY);
                case 1 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e📥 Secondary Alloy Ingot Slot"),
                        Component.literal("§7Insert flux or secondary alloy metal:"),
                        Component.literal("§f• Tin (Bronze), Ardite (Manyullyn), Coke/Carbon")
                ), mouseX, mouseY);
                case 2 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§a✨ Master Ingot Casting Mold"),
                        Component.literal("§7Smelted alloy ingots are cast and collected here.")
                ), mouseX, mouseY);
                case 3 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§d⚙️ Gear Overclock Bay"),
                        Component.literal("§7Insert a Gear or Blaze Core:"),
                        Component.literal("§f• Drastically accelerates induction heating speed.")
                ), mouseX, mouseY);
            }
        }
    }
}
