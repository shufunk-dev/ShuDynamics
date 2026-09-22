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
public class HarmonicRecordPressScreen extends HandledScreen<HarmonicRecordPressScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/harmonic_record_press_gui.png");

    public HarmonicRecordPressScreen(HarmonicRecordPressScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // 1. Progress Needle / Record Cutter Arrow
        int progress = this.handler.getScaledProgress();
        if (progress > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 76, y + 35, 176.0f, 14.0f, progress + 1, 16, 256, 256);
        }

        // 2. Energy Bar
        int energyHeight = this.handler.getScaledEnergy();
        if (energyHeight > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 152, y + 72 - energyHeight, 190.0f, 52.0f - energyHeight, 14, energyHeight, 256, 256);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Energy Tooltip
        if (mouseX >= x + 151 && mouseX <= x + 167 && mouseY >= y + 19 && mouseY <= y + 73) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§e⚡ Energy Storage"),
                    Text.literal(String.format("§f%,d / %,d FE", this.handler.getEnergy(), this.handler.getMaxEnergy())),
                    Text.literal("§7Draws 40 FE/t during record pressing.")
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.focusedSlot != null && !this.focusedSlot.hasStack() && this.focusedSlot.id < 4) {
            switch (this.focusedSlot.id) {
                case 0 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§e💿 Blank Vinyl Slot"),
                        Text.literal("§7Insert a §fBlank Vinyl Disc§7 to cut."),
                        Text.literal("§8Crafted with Rubber + Resin + Iron Nugget.")
                ), mouseX, mouseY);
                case 1 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§d🔮 Acoustic Catalyst Slot"),
                        Text.literal("§7Insert a thematic catalyst to imprint:"),
                        Text.literal("§5• Crying Obsidian §8(Rip the Sky Wide)"),
                        Text.literal("§c• Fire Crystal §8(Rift of the Colossus)"),
                        Text.literal("§6• High-Octane Fuel §8(Highway Overdrive)"),
                        Text.literal("§b• Silicon Wafer §8(Sterile Protocol)"),
                        Text.literal("§a• Storage Crystal §8(Subroutine 64k)"),
                        Text.literal("§d• Master Rainbow Roll §8(Haven Bloom)"),
                        Text.literal("§4• Manyullyn Ingot §8(Heart of the Crucible)"),
                        Text.literal("§9• Hydrogen Canister §8(Stratosphere Break)"),
                        Text.literal("§3• Diving Mask / Nautilus §8(Abyssal Pressure)"),
                        Text.literal("§e• Sulfur Dust §8(Anoxic Echoes)"),
                        Text.literal("§7• Any Music Disc §8(Duplicates the record!)")
                ), mouseX, mouseY);
                case 2 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§a📦 Pressed Music Disc Output"),
                        Text.literal("§7The finished record is placed here.")
                ), mouseX, mouseY);
                case 3 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§6⚙ Overclocking Gear Socket"),
                        Text.literal("§7Insert mechanical or enchanted gears"),
                        Text.literal("§7to accelerate pressing speed up to 8x.")
                ), mouseX, mouseY);
            }
        }
    }
}
