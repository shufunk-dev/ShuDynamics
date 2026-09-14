package net.enchantedwood.screen;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.block.custom.CastingMode;
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

import java.util.List;

@Environment(EnvType.CLIENT)
public class CastingPortScreen extends HandledScreen<CastingPortScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/casting_port_gui.png");

    private ButtonWidget modeButton;

    public CastingPortScreen(CastingPortScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        this.modeButton = ButtonWidget.builder(Text.literal(this.handler.getMode().getDisplayName()), button -> {
            if (this.client != null && this.client.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult hitResult) {
                ClientPlayNetworking.send(new net.enchantedwood.network.ToggleCastingPortModePayload(hitResult.getBlockPos()));
            }
        }).dimensions(x + 60, y + 14, 48, 14).build();

        this.addDrawableChild(this.modeButton);
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        if (this.modeButton != null) {
            this.modeButton.setMessage(Text.literal(this.handler.getMode().getDisplayName()));
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // 1. Fluid Buffer Bar at x + 36, y + 18 (width 16, height 50)
        int fluidHeight = this.handler.getScaledFluid(50);
        if (fluidHeight > 0) {
            MoltenMetal fluid = this.handler.getFluidType();
            int color = fluid != MoltenMetal.NONE ? (fluid.getColor() | 0xFF000000) : 0xFFD8D8D8;
            context.fill(x + 36, y + 68 - fluidHeight, x + 36 + 16, y + 68, color);
        }

        // 2. Solidification Progress Arrow at x + 72, y + 34 (width 24, height 17)
        int cookWidth = this.handler.getScaledProgress(24);
        if (cookWidth > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 72, y + 34, 176.0f, 0.0f, cookWidth, 17, 256, 256);
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

        // Fluid Tooltip
        if (mouseX >= x + 35 && mouseX <= x + 53 && mouseY >= y + 17 && mouseY <= y + 69) {
            MoltenMetal fluid = this.handler.getFluidType();
            String name = (fluid != MoltenMetal.NONE) ? fluid.getDisplayName() : "Empty";
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§d💧 Fluid Intake Buffer"),
                    Text.literal(String.format("§fType: §e%s", name)),
                    Text.literal(String.format("§7Volume: §b%,d / 2,000 mB", this.handler.getFluidAmount()))
            ), mouseX, mouseY);
        }

        // Mode Tooltip
        if (mouseX >= x + 64 && mouseX <= x + 104 && mouseY >= y + 16 && mouseY <= y + 28) {
            CastingMode mode = this.handler.getMode();
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§6⚡ Casting Mold"),
                    Text.literal(String.format("§fMode: §e%s", mode.getDisplayName())),
                    Text.literal(String.format("§7Cost: §b%d mB / item", mode.getFluidCostMb())),
                    Text.literal("§8Click to cycle (Ingot -> Block -> Nugget)")
            ), mouseX, mouseY);
        }

        // Progress Tooltip
        if (mouseX >= x + 71 && mouseX <= x + 97 && mouseY >= y + 33 && mouseY <= y + 52) {
            int progress = this.handler.getScaledProgress(100);
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§b❄ Mold Solidification"),
                    Text.literal(String.format("§7Progress: §f%d%%", progress))
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.focusedSlot != null && !this.focusedSlot.hasStack() && this.focusedSlot.id == 0) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§b❄ Solidified Cast Product"),
                    Text.literal("§7Outputs solidified metal items:"),
                    Text.literal("§f• Nuggets (10 mB)"),
                    Text.literal("§f• Ingots (90 mB)"),
                    Text.literal("§f• Blocks (810 mB)"),
                    Text.literal("§8Automatically pushes into adjacent chests, hoppers, or pipes.")
            ), mouseX, mouseY);
        }
    }
}
