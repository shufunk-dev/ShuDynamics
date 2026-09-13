package net.enchantedwood.screen;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.fluid.MoltenMetal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class InductionSmelterScreen extends HandledScreen<InductionSmelterScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/induction_smelter_gui.png");

    private ButtonWidget alloyButton;

    public InductionSmelterScreen(InductionSmelterScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        this.alloyButton = ButtonWidget.builder(getAlloyText(), button -> {
            if (this.client != null && this.client.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult hitResult) {
                ClientPlayNetworking.send(new net.enchantedwood.network.ToggleInductionSmelterAlloyPayload(hitResult.getBlockPos()));
            }
        }).dimensions(x + 70, y + 4, 52, 14).build();

        this.addDrawableChild(this.alloyButton);
    }

    private Text getAlloyText() {
        return this.handler.isAlloyingEnabled() ? Text.literal("⚡ ALLOY: ON") : Text.literal("○ ALLOY: OFF");
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        if (this.alloyButton != null) {
            this.alloyButton.visible = this.handler.hasChip();
            this.alloyButton.setMessage(getAlloyText());
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // 1. Lava Gauge at x + 10, y + 18 (width 12, height 50)
        int lavaHeight = this.handler.getScaledLava(50);
        if (lavaHeight > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 10, y + 68 - lavaHeight, 176.0f, 50.0f - lavaHeight, 12, lavaHeight, 256, 256);
        }

        // 2. Energy Gauge at x + 26, y + 18 (width 12, height 50)
        int energyHeight = this.handler.getScaledEnergy(50);
        if (energyHeight > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 26, y + 68 - energyHeight, 188.0f, 50.0f - energyHeight, 12, energyHeight, 256, 256);
        }

        // 3. Smelting Flame/Progress at x + 76, y + 42 (width 24, height 17)
        int cookWidth = this.handler.getScaledCookProgress(24);
        if (cookWidth > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 76, y + 42, 200.0f, 0.0f, cookWidth, 17, 256, 256);
        }

        // 4. Molten Metal Reservoir at x + 142, y + 26 (width 14, height 50)
        int fluidHeight = this.handler.getScaledMoltenVolume(50);
        if (fluidHeight > 0) {
            MoltenMetal top = this.handler.getMostAbundantFluid();
            int color = top.getColor() | 0xFF000000;
            context.fill(x + 142, y + 76 - fluidHeight, x + 142 + 14, y + 76, color);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, 8, 6, 4210752, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 4210752, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Lava Tooltip
        if (mouseX >= x + 9 && mouseX <= x + 23 && mouseY >= y + 17 && mouseY <= y + 69) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§c🔥 Thermal Lava Reservoir"),
                    Text.literal(String.format("§6%,d / %,d mB", this.handler.getLava(), this.handler.getMaxLava())),
                    Text.literal("§7Draw: 5 mB / smelt cycle")
            ), mouseX, mouseY);
        }

        // Energy Tooltip
        if (mouseX >= x + 25 && mouseX <= x + 39 && mouseY >= y + 17 && mouseY <= y + 69) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§6⚡ Induction Energy Coils"),
                    Text.literal(String.format("§e%,d / %,d FE", this.handler.getEnergy(), this.handler.getMaxEnergy())),
                    Text.literal("§7Draw: 45 FE/t")
            ), mouseX, mouseY);
        }

        // Progress Tooltip
        if (mouseX >= x + 75 && mouseX <= x + 101 && mouseY >= y + 41 && mouseY <= y + 60) {
            int progress = this.handler.getScaledCookProgress(100);
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§e🔥 Induction Liquefaction"),
                    Text.literal(String.format("§7Progress: §f%d%%", progress))
            ), mouseX, mouseY);
        }

        // Molten Metal Chamber Tooltip
        if (mouseX >= x + 141 && mouseX <= x + 157 && mouseY >= y + 25 && mouseY <= y + 77) {
            List<Text> tooltip = new ArrayList<>();
            tooltip.add(Text.literal("§d💧 Molten Metal Reservoir"));
            int total = this.handler.getTotalMoltenVolume();
            tooltip.add(Text.literal(String.format("§fTotal Stored: §b%,d / 32,400 mB", total)));
            MoltenMetal top = this.handler.getMostAbundantFluid();
            if (top != MoltenMetal.NONE && total > 0) {
                tooltip.add(Text.literal(String.format("§7Top Fluid: §e%s", top.getDisplayName())));
            } else {
                tooltip.add(Text.literal("§8Chamber Empty"));
            }
            tooltip.add(Text.literal("§8Connect pipes or Casting Port to extract."));
            context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
        }

        // Alloying Switch Tooltip
        if (this.handler.hasChip() && mouseX >= x + 72 && mouseX <= x + 116 && mouseY >= y + 6 && mouseY <= y + 17) {
            boolean on = this.handler.isAlloyingEnabled();
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§6⚡ Metallurgy Logic Controller"),
                    Text.literal(on ? "§aStatus: ACTIVE (Thermal Alloying ON)" : "§7Status: INACTIVE (Pure Metal Smelt)"),
                    Text.literal("§8Click to toggle automatic alloy synthesis.")
            ), mouseX, mouseY);
        }
    }
}
