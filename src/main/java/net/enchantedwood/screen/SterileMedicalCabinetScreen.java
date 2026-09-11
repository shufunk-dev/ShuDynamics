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
public class SterileMedicalCabinetScreen extends HandledScreen<SterileMedicalCabinetScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/sterile_medical_cabinet_gui.png");

    public SterileMedicalCabinetScreen(SterileMedicalCabinetScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 196;
        this.titleX = 8;
        this.titleY = 6;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = 104;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0x1E3A5F, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0x1E3A5F, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        // Tooltips for designated empty slots
        if (this.focusedSlot != null && !this.focusedSlot.hasStack() && this.focusedSlot.id < 36) {
            List<Text> tooltip = getSlotGuideTooltip(this.focusedSlot.id);
            if (tooltip != null) {
                context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
            }
        }
    }

    private List<Text> getSlotGuideTooltip(int slotId) {
        return switch (slotId) {
            case 0 -> List.of(Text.literal("§b💉 Hypospray Injector"), Text.literal("§7Stores medical application injector."));
            case 1 -> List.of(Text.literal("§9🧪 Empty Hypospray Cartridges"), Text.literal("§7Sterile ampoules ready for filling."));
            case 2 -> List.of(Text.literal("§f🪟 Glass Panes"), Text.literal("§7Raw cartridge wall material."));
            case 3 -> List.of(Text.literal("§f🧊 Glass Blocks"), Text.literal("§7Raw cartridge wall material."));
            case 4 -> List.of(Text.literal("§7⚙ Tin Ingots"), Text.literal("§7Sterile cartridge sealing caps."));
            case 5 -> List.of(Text.literal("§b⚙ Titanium Ingots / Nuggets"), Text.literal("§7Heavy framing & medical reinforcement."));
            case 6 -> List.of(Text.literal("§f💎 Nether Quartz"), Text.literal("§7Cartridge sterilizing flux & catalysts."));
            case 7 -> List.of(Text.literal("§c🔴 Redstone Dust"), Text.literal("§7Cartridge catalytic flux."));
            case 8 -> List.of(Text.literal("§e✨ Glowstone Dust"), Text.literal("§7Cartridge catalytic flux."));

            case 9 -> List.of(Text.literal("§a🧪 Alkaline Base Extract"), Text.literal("§7Compounding Acid-Neutralizing Cartridges."));
            case 10 -> List.of(Text.literal("§6🧪 Cryo-Thermal Extract"), Text.literal("§7Compounding Heat-Buffer Cartridges."));
            case 11 -> List.of(Text.literal("§b🧪 Oxygenated Extract"), Text.literal("§7Compounding Hyper-Oxygenation Cartridges."));
            case 12 -> List.of(Text.literal("§d🧪 Cellular Nanite Extract"), Text.literal("§7Compounding Nanite Trauma Cartridges."));
            case 13 -> List.of(Text.literal("§e🧪 Adrenal Essence"), Text.literal("§7Compounding Adrenaline Stim Cartridges."));
            case 14 -> List.of(Text.literal("§8🌋 Volcanic Ash"), Text.literal("§7Centrifuge byproduct → Heat-buffer thermal catalyst."));
            case 15 -> List.of(Text.literal("§f🦴 Bone Meal"), Text.literal("§7Centrifuge byproduct → Nanite tissue/bone repair matrix."));
            case 16 -> List.of(Text.literal("§e🟡 Sulfur Dust"), Text.literal("§7Centrifuge byproduct → Acid-neutralizing catalyst."));
            case 17 -> List.of(Text.literal("§f🧂 Sugar"), Text.literal("§7Centrifuge byproduct → Adrenaline combat stim catalyst."));

            case 18 -> List.of(Text.literal("§a🟢 Slimeballs"), Text.literal("§7Feedstock for Alkaline Extract."));
            case 19 -> List.of(Text.literal("§6🟠 Magma Cream / Crimson Fungus"), Text.literal("§7Feedstock for Cryo-Thermal Extract."));
            case 20 -> List.of(Text.literal("§2🌿 Kelp / Seagrass / Cucumber"), Text.literal("§7Feedstock for Oxygenated Extract."));
            case 21 -> List.of(Text.literal("§d🐉 Dragon Fruit / Nether Wart"), Text.literal("§7Feedstock for Cellular Nanite Extract."));
            case 22 -> List.of(Text.literal("§e🟡 Glow Berries / Wasabi Root"), Text.literal("§7Feedstock for Adrenal Essence."));
            case 23 -> List.of(Text.literal("§6🔥 Blaze Powder / Fire Crystal"), Text.literal("§7Endothermic heat-buffer catalyst."));
            case 24 -> List.of(Text.literal("§e🍏 Golden Apple / Ghast Tear"), Text.literal("§7Nanite trauma synthesis catalyst."));
            case 25 -> List.of(Text.literal("§f⚙ Aluminum / Iron Ingots"), Text.literal("§7Oxygenation carrier catalyst."));
            case 26 -> List.of(Text.literal("§b🧵 Sterile Polymer Fabric"), Text.literal("§7Cleanroom filtration & Bunny Suit textile."));

            case 27 -> List.of(Text.literal("§a💉 Acid-Neutralizing Cartridge"), Text.literal("§7Standard or ✦ Pure variant."));
            case 28 -> List.of(Text.literal("§6💉 Endothermic Heat-Buffer Cartridge"), Text.literal("§7Standard or ✦ Pure variant."));
            case 29 -> List.of(Text.literal("§b💉 Hyper-Oxygenation Cartridge"), Text.literal("§7Standard or ✦ Pure variant."));
            case 30 -> List.of(Text.literal("§d💉 Nanite Trauma Cartridge"), Text.literal("§7Standard or ✦ Pure variant."));
            case 31 -> List.of(Text.literal("§e💉 Adrenaline Stim Cartridge"), Text.literal("§7Standard or ✦ Pure variant."));
            case 32, 33, 34, 35 -> List.of(Text.literal("§5💉 Dispensary & Loaded Hyposprays"), Text.literal("§7Accepts any Hypospray Injector or filled Cartridges."));

            default -> null;
        };
    }
}
