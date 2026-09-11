package net.enchantedwood.screen;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.block.custom.GearTier;
import net.enchantedwood.block.entity.PolymerLoomBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

@Environment(EnvType.CLIENT)
public class PolymerLoomScreen extends HandledScreen<PolymerLoomScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/polymer_loom_gui.png");

    public PolymerLoomScreen(PolymerLoomScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // Energy Bar at x + 18, y + 20 (width 16, height 50)
        int energyHeight = this.handler.getScaledEnergy(50);
        if (energyHeight > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 18, y + 70 - energyHeight, 192.0f, 50.0f - energyHeight, 16, energyHeight, 256, 256);
        }

        // Cook Progress Arrow at x + 76, y + 35 (width 24, height 17)
        int cookWidth = this.handler.getScaledCookProgress(24);
        if (cookWidth > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 76, y + 35, 176.0f, 14.0f, cookWidth, 17, 256, 256);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 4210752, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 4210752, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Energy Bar Tooltip
        if (mouseX >= x + 17 && mouseX <= x + 35 && mouseY >= y + 19 && mouseY <= y + 71) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§e⚡ Energy Storage"),
                    Text.literal(String.format("§6%,d / %,d FE", this.handler.getEnergy(), this.handler.getMaxEnergy())),
                    Text.literal("§7Draw: §a40 FE/t §7(Active Weaving)")
            ), mouseX, mouseY);
        }

        // Gear Tooltip (when gear is installed)
        if (mouseX >= x + 151 && mouseX <= x + 169 && mouseY >= y + 7 && mouseY <= y + 25 && this.handler.getGearTier() != GearTier.NONE) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§d⚙ Installed Loom Gear"),
                    Text.literal(String.format("§7Overclock Tier: §f%s", this.handler.getGearTier().name())),
                    Text.literal(String.format("§aWeaving Speed: §e%d ticks/item", PolymerLoomBlockEntity.getTierCookTime(this.handler.getGearTier())))
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.focusedSlot != null && !this.focusedSlot.hasStack() && this.focusedSlot.id < 5) {
            switch (this.focusedSlot.id) {
                case 0 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§e🧶 Raw Polymer / Textile Feed"),
                        Text.literal("§7Insert: §fRubber §7or §fSterile Polymer Fabric"),
                        Text.literal("§8Polymer base for spinning lint-free textiles.")
                ), mouseX, mouseY);
                case 1 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§b🧵 Reinforcing Fibers / Glass"),
                        Text.literal("§7Insert: §fString, White Wool, Glass Pane, Aluminum Ingot"),
                        Text.literal("§8Tensile threads woven into the polymer matrix.")
                ), mouseX, mouseY);
                case 2 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§6⚙ Additive / Hermetic Sealant"),
                        Text.literal("§7Insert: §fSilicon, Silicon Wafer, Titanium Nugget, Rubber"),
                        Text.literal("§8Anti-static semiconductor / hermetic bounding agents.")
                ), mouseX, mouseY);
                case 3 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§d✨ Finished Cleanroom Product"),
                        Text.literal("§7Outputs:"),
                        Text.literal("§f• Sterile Polymer Fabric (4×)"),
                        Text.literal("§f• Cleanroom Hood, Smock, Trousers, Booties")
                ), mouseX, mouseY);
                case 4 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§5⚙ Loom Overclock Gear Slot"),
                        Text.literal("§7Insert: §fMachine Speed Gear (Iron to Netherite)"),
                        Text.literal("§8Dramatically accelerates spinning & weaving speed.")
                ), mouseX, mouseY);
            }
        }
    }
}
