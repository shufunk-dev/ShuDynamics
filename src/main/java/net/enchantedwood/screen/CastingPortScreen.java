package net.enchantedwood.screen;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.block.custom.CastingMode;
import net.enchantedwood.fluid.MoltenMetal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;

@Environment(EnvType.CLIENT)
public class CastingPortScreen extends AbstractContainerScreen<CastingPortScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/casting_port_gui.png");

    private Button modeButton;

    public CastingPortScreen(CastingPortScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        this.modeButton = Button.builder(Component.literal(this.menu.getMode().getDisplayName()), button -> {
            if (this.minecraft != null && this.minecraft.hitResult instanceof net.minecraft.world.phys.BlockHitResult hitResult) {
                ClientPlayNetworking.send(new net.enchantedwood.network.ToggleCastingPortModePayload(hitResult.getBlockPos()));
            }
        }).bounds(x + 58, y + 14, 56, 14).build();

        this.addRenderableWidget(this.modeButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.modeButton != null) {
            this.modeButton.setMessage(Component.literal(this.menu.getMode().getDisplayName()));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // 1. Fluid Buffer Bar at x + 36, y + 18 (width 16, height 50)
        int fluidHeight = this.menu.getScaledFluid(50);
        if (fluidHeight > 0) {
            MoltenMetal fluid = this.menu.getFluidType();
            int color = fluid != MoltenMetal.NONE ? (fluid.getColor() | 0xFF000000) : 0xFFD8D8D8;
            context.fill(x + 36, y + 68 - fluidHeight, x + 36 + 16, y + 68, color);
        }

        // 2. Solidification Progress Arrow at x + 72, y + 34 (width 24, height 17)
        int cookWidth = this.menu.getScaledProgress(24);
        if (cookWidth > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 72, y + 34, 176.0f, 0.0f, cookWidth, 17, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.text(this.font, this.title, 8, 6, 4210752, false);
        context.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        boolean online = this.menu.isNetworkOnline();
        String badge = online ? "§a✦ Link" : "§8○ Offline";
        context.text(this.font, Component.literal(badge), this.imageWidth - 52, 6, 0xFFFFFF, true);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);


        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Fluid Tooltip
        if (mouseX >= x + 35 && mouseX <= x + 53 && mouseY >= y + 17 && mouseY <= y + 69) {
            MoltenMetal fluid = this.menu.getFluidType();
            String name = (fluid != MoltenMetal.NONE) ? fluid.getDisplayName() : "Empty";
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§d💧 Fluid Intake Buffer"),
                    Component.literal(String.format("§fType: §e%s", name)),
                    Component.literal(String.format("§7Volume: §b%,d / 2,000 mB", this.menu.getFluidAmount()))
            ), mouseX, mouseY);
        }

        // Mode Tooltip
        if (mouseX >= x + 56 && mouseX <= x + 116 && mouseY >= y + 14 && mouseY <= y + 28) {
            CastingMode mode = this.menu.getMode();
            List<Component> lines = new java.util.ArrayList<>();
            lines.add(Component.literal("§6⚡ Casting Mold Mode"));
            lines.add(Component.literal(String.format("§fActive: §e%s", mode.getDisplayName())));
            if (mode == CastingMode.STANDBY) {
                lines.add(Component.literal("§a● Standby: Auto-Craft On Demand"));
                lines.add(Component.literal("§7Does not cast autonomously on tick."));
                lines.add(Component.literal("§7Allows Super Computer to cast directly as needed."));
            } else {
                lines.add(Component.literal(String.format("§7Continuous Cast: §b%d mB / %s", mode.getFluidCostMb(), mode.getDisplayName().toLowerCase())));
                lines.add(Component.literal("§8Pushes finished items into adjacent inventories."));
            }
            lines.add(Component.literal("§8Click to cycle (Standby -> Ingot -> Block -> Nugget)"));
            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }

        // Progress Tooltip
        if (mouseX >= x + 71 && mouseX <= x + 97 && mouseY >= y + 33 && mouseY <= y + 52) {
            int progress = this.menu.getScaledProgress(100);
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§b❄ Mold Solidification"),
                    Component.literal(String.format("§7Progress: §f%d%%", progress))
            ), mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index == 0) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§b❄ Solidified Cast Product"),
                    Component.literal("§7Outputs solidified metal items:"),
                    Component.literal("§f• Nuggets (10 mB)"),
                    Component.literal("§f• Ingots (90 mB)"),
                    Component.literal("§f• Blocks (810 mB)"),
                    Component.literal("§8Automatically pushes into adjacent chests, hoppers, or pipes.")
            ), mouseX, mouseY);
        }

        // Wireless Link Tooltip
        if (mouseX >= x + this.imageWidth - 56 && mouseX <= x + this.imageWidth - 6 && mouseY >= y + 4 && mouseY <= y + 18) {
            boolean online = this.menu.isNetworkOnline();
            List<Component> lines = new java.util.ArrayList<>();
            lines.add(Component.literal("§6📡 Wireless Storage Link"));
            if (online) {
                lines.add(Component.literal("§a● Status: Connected to Digital Storage"));
                lines.add(Component.literal("§7Cast ingots/blocks automatically beam straight"));
                lines.add(Component.literal("§7into your Base Storage Network!"));
            } else {
                lines.add(Component.literal("§7○ Status: Offline (No Terminal in range)"));
                lines.add(Component.literal("§8Bring within 16 blocks of a Storage Terminal or"));
                lines.add(Component.literal("§8Sneak + Right-Click with Wrench to bind cross-distance."));
            }
            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }
    }
}
