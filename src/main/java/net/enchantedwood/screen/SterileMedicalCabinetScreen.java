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
public class SterileMedicalCabinetScreen extends AbstractContainerScreen<SterileMedicalCabinetScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/sterile_medical_cabinet_gui.png");

    public SterileMedicalCabinetScreen(SterileMedicalCabinetScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 176, 196);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 104;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x1E3A5F, false);
        context.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x1E3A5F, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);


        // Tooltips for designated empty slots
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 36) {
            List<Component> tooltip = getSlotGuideTooltip(this.hoveredSlot.index);
            if (tooltip != null) {
                context.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
            }
        }
    }

    private List<Component> getSlotGuideTooltip(int slotId) {
        return switch (slotId) {
            case 0 -> List.of(Component.literal("§b💉 Hypospray Injector"), Component.literal("§7Stores medical application injector."));
            case 1 -> List.of(Component.literal("§9🧪 Empty Hypospray Cartridges"), Component.literal("§7Sterile ampoules ready for filling."));
            case 2 -> List.of(Component.literal("§f🪟 Glass Panes"), Component.literal("§7Raw cartridge wall material."));
            case 3 -> List.of(Component.literal("§f🧊 Glass Blocks"), Component.literal("§7Raw cartridge wall material."));
            case 4 -> List.of(Component.literal("§7⚙ Tin Ingots"), Component.literal("§7Sterile cartridge sealing caps."));
            case 5 -> List.of(Component.literal("§b⚙ Titanium Ingots / Nuggets"), Component.literal("§7Heavy framing & medical reinforcement."));
            case 6 -> List.of(Component.literal("§f💎 Nether Quartz"), Component.literal("§7Cartridge sterilizing flux & catalysts."));
            case 7 -> List.of(Component.literal("§c🔴 Redstone Dust"), Component.literal("§7Cartridge catalytic flux."));
            case 8 -> List.of(Component.literal("§e✨ Glowstone Dust"), Component.literal("§7Cartridge catalytic flux."));

            case 9 -> List.of(Component.literal("§a🧪 Alkaline Base Extract"), Component.literal("§7Compounding Acid-Neutralizing Cartridges."));
            case 10 -> List.of(Component.literal("§6🧪 Cryo-Thermal Extract"), Component.literal("§7Compounding Heat-Buffer Cartridges."));
            case 11 -> List.of(Component.literal("§b🧪 Oxygenated Extract"), Component.literal("§7Compounding Hyper-Oxygenation Cartridges."));
            case 12 -> List.of(Component.literal("§d🧪 Cellular Nanite Extract"), Component.literal("§7Compounding Nanite Trauma Cartridges."));
            case 13 -> List.of(Component.literal("§e🧪 Adrenal Essence"), Component.literal("§7Compounding Adrenaline Stim Cartridges."));
            case 14 -> List.of(Component.literal("§8🌋 Volcanic Ash"), Component.literal("§7Centrifuge byproduct → Heat-buffer thermal catalyst."));
            case 15 -> List.of(Component.literal("§f🦴 Bone Meal"), Component.literal("§7Centrifuge byproduct → Nanite tissue/bone repair matrix."));
            case 16 -> List.of(Component.literal("§e🟡 Sulfur Dust"), Component.literal("§7Centrifuge byproduct → Acid-neutralizing catalyst."));
            case 17 -> List.of(Component.literal("§f🧂 Sugar"), Component.literal("§7Centrifuge byproduct → Adrenaline combat stim catalyst."));

            case 18 -> List.of(Component.literal("§a🟢 Slimeballs"), Component.literal("§7Feedstock for Alkaline Extract."));
            case 19 -> List.of(Component.literal("§6🟠 Magma Cream / Crimson Fungus"), Component.literal("§7Feedstock for Cryo-Thermal Extract."));
            case 20 -> List.of(Component.literal("§2🌿 Kelp / Seagrass / Cucumber"), Component.literal("§7Feedstock for Oxygenated Extract."));
            case 21 -> List.of(Component.literal("§d🐉 Dragon Fruit / Nether Wart"), Component.literal("§7Feedstock for Cellular Nanite Extract."));
            case 22 -> List.of(Component.literal("§e🟡 Glow Berries / Wasabi Root"), Component.literal("§7Feedstock for Adrenal Essence."));
            case 23 -> List.of(Component.literal("§6🔥 Blaze Powder / Fire Crystal"), Component.literal("§7Endothermic heat-buffer catalyst."));
            case 24 -> List.of(Component.literal("§e🍏 Golden Apple / Ghast Tear"), Component.literal("§7Nanite trauma synthesis catalyst."));
            case 25 -> List.of(Component.literal("§f⚙ Aluminum / Iron Ingots"), Component.literal("§7Oxygenation carrier catalyst."));
            case 26 -> List.of(Component.literal("§b🧵 Sterile Polymer Fabric"), Component.literal("§7Cleanroom filtration & Bunny Suit textile."));

            case 27 -> List.of(Component.literal("§a💉 Acid-Neutralizing Cartridge"), Component.literal("§7Standard or ✦ Pure variant."));
            case 28 -> List.of(Component.literal("§6💉 Endothermic Heat-Buffer Cartridge"), Component.literal("§7Standard or ✦ Pure variant."));
            case 29 -> List.of(Component.literal("§b💉 Hyper-Oxygenation Cartridge"), Component.literal("§7Standard or ✦ Pure variant."));
            case 30 -> List.of(Component.literal("§d💉 Nanite Trauma Cartridge"), Component.literal("§7Standard or ✦ Pure variant."));
            case 31 -> List.of(Component.literal("§e💉 Adrenaline Stim Cartridge"), Component.literal("§7Standard or ✦ Pure variant."));
            case 32, 33, 34, 35 -> List.of(Component.literal("§5💉 Dispensary & Loaded Hyposprays"), Component.literal("§7Accepts any Hypospray Injector or filled Cartridges."));

            default -> null;
        };
    }
}
