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
public class AlloyFoundryScreen extends HandledScreen<AlloyFoundryScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/alloy_foundry_gui.png");

    public AlloyFoundryScreen(AlloyFoundryScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        this.titleX = 8;
        this.titleY = 7;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // 1. Energy Bar at x + 18, y + 20 (width 16, height 50) using sprite at u=208, v=0
        int energyHeight = this.handler.getScaledEnergy(50);
        if (energyHeight > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 18, y + 70 - energyHeight, 208.0f, 50.0f - energyHeight, 16, energyHeight, 256, 256);
        }

        // 2. Molten Alloy Progress Arrow at x + 86, y + 34 (width 24, height 17) using sprite at u=176, v=14
        int cookWidth = this.handler.getScaledCookProgress(24);
        if (cookWidth > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 86, y + 34, 176.0f, 14.0f, cookWidth, 17, 256, 256);

            // Active smelting molten thermal flecks at the pouring tip
            if (this.handler.isCooking()) {
                long time = System.currentTimeMillis();
                int sparkX = x + 86 + cookWidth - 1;
                int sparkY = y + 42 + (int) ((time / 100) % 3) - 1;
                int sparkColor = ((time / 80) % 2 == 0) ? 0xFFFFF8D0 : 0xFFFFB300;
                context.fill(sparkX, sparkY, sparkX + 2, sparkY + 2, sparkColor);
            }
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Machine title in illuminated warm ivory on the dark titanium nameplate
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0xFFE8E0D0, false);

        // Player Inventory label in crisp dark slate
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0xFF373A40, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Energy Bar Tooltip
        if (mouseX >= x + 16 && mouseX <= x + 35 && mouseY >= y + 18 && mouseY <= y + 71) {
            int energy = this.handler.getEnergy();
            int max = this.handler.getMaxEnergy();
            int pct = max > 0 ? (int) (((long) energy * 100) / max) : 0;
            String status = this.handler.isCooking() ? "§6♨ Induction Heating (40 FE/t)" : (energy >= 40 ? "§a● Ready (Induction Coils Hot)" : "§c⚡ Depleted");

            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§6⚡ Induction Energy Buffer"),
                    Text.literal(String.format("§e%,d / %,d FE §7(%d%%)", energy, max, pct)),
                    Text.literal("§7Base Draw: §f40 FE/t"),
                    Text.literal("§7Status: " + status)
            ), mouseX, mouseY);
        }

        // Smelting Progress Arrow Tooltip
        if (mouseX >= x + 86 && mouseX <= x + 109 && mouseY >= y + 34 && mouseY <= y + 50) {
            if (this.handler.isCooking()) {
                int pct = this.handler.getScaledCookProgress(100);
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§6♨ Induction Smelting Chamber"),
                        Text.literal(String.format("§eSmelting Progress: §f%d%%", pct)),
                        Text.literal("§7Alloying base and flux metals into casting mold.")
                ), mouseX, mouseY);
            } else {
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§6♨ Induction Smelting Chamber"),
                        Text.literal("§7Insert 2 compatible metals to forge an alloy:"),
                        Text.literal("§8• Copper + Tin -> Bronze"),
                        Text.literal("§8• Cobalt + Ardite -> Manyullyn"),
                        Text.literal("§8• Tungsten + Carbon -> Tungsten Carbide")
                ), mouseX, mouseY);
            }
        }

        // Gear Tooltip (when gear installed)
        if (mouseX >= x + 150 && mouseX <= x + 170 && mouseY >= y + 6 && mouseY <= y + 26) {
            net.enchantedwood.block.custom.GearTier tier = this.handler.getGearTier();
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
                    default -> "1.0x";
                };
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§d⚙ Installed Gear Upgrade"),
                        Text.literal(String.format("§7Speed Tier: §f%s §a(%s Speed)", tier.name(), speedMultiplier)),
                        Text.literal("§8Significantly accelerates melting cycle.")
                ), mouseX, mouseY);
            }
        }

        // Empty Machine Slot Tooltips
        if (this.focusedSlot != null && !this.focusedSlot.hasStack() && this.focusedSlot.id < 4) {
            switch (this.focusedSlot.id) {
                case 0 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§e📥 Primary Alloy Ingot Slot"),
                        Text.literal("§7Insert primary base metal:"),
                        Text.literal("§f• Copper, Tungsten, Iron, Cobalt, etc.")
                ), mouseX, mouseY);
                case 1 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§e📥 Secondary Alloy Ingot Slot"),
                        Text.literal("§7Insert flux or secondary alloy metal:"),
                        Text.literal("§f• Tin (Bronze), Ardite (Manyullyn), Coke/Carbon")
                ), mouseX, mouseY);
                case 2 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§a✨ Master Ingot Casting Mold"),
                        Text.literal("§7Smelted alloy ingots are cast and collected here.")
                ), mouseX, mouseY);
                case 3 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§d⚙️ Gear Overclock Bay"),
                        Text.literal("§7Insert a Gear or Blaze Core:"),
                        Text.literal("§f• Drastically accelerates induction heating speed.")
                ), mouseX, mouseY);
            }
        }
    }
}
