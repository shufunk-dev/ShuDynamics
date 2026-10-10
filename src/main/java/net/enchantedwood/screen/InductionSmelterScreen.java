package net.enchantedwood.screen;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.fluid.MoltenMetal;
import net.enchantedwood.network.InductionSmelterActionPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.BlockHitResult;
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class InductionSmelterScreen extends AbstractContainerScreen<InductionSmelterScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/induction_smelter_gui.png");

    private Button alloyButton;
    private Button dumpButton;
    private Button ejectButton;

    public InductionSmelterScreen(InductionSmelterScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    private BlockPos getTargetPos() {
        if (this.minecraft != null && this.minecraft.hitResult instanceof BlockHitResult hitResult) {
            return hitResult.getBlockPos();
        }
        return BlockPos.ZERO;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int panelX = x + 176;
        int panelY = y + 16;

        // 1. Alloy Toggle Button
        this.alloyButton = Button.builder(getAlloyText(), button -> {
            ClientPlayNetworking.send(new InductionSmelterActionPayload(getTargetPos(), 0));
        }).bounds(panelX + 6, panelY + 68, 54, 15).build();

        // 2. Dump / Purge Button (Destroys Tanks 1 & 2)
        this.dumpButton = Button.builder(Component.literal("§c🗑 Dump"), button -> {
            ClientPlayNetworking.send(new InductionSmelterActionPayload(getTargetPos(), 1));
        }).bounds(panelX + 6, panelY + 86, 26, 15).build();

        // 3. Eject Button (Pumps Tanks 1 & 2 into Pipes)
        this.ejectButton = Button.builder(getEjectText(), button -> {
            ClientPlayNetworking.send(new InductionSmelterActionPayload(getTargetPos(), 2));
        }).bounds(panelX + 34, panelY + 86, 26, 15).build();

        this.addRenderableWidget(this.alloyButton);
        this.addRenderableWidget(this.dumpButton);
        this.addRenderableWidget(this.ejectButton);
    }

    private Component getAlloyText() {
        return this.menu.isAlloyingEnabled() ? Component.literal("§a⚡ MIX: ON") : Component.literal("§7○ MIX: OFF");
    }

    private Component getEjectText() {
        return this.menu.isEjectingHoldingTanks() ? Component.literal("§b⏏...") : Component.literal("§b⏏");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean hasChip = this.menu.hasChip();
        if (this.alloyButton != null) {
            this.alloyButton.visible = hasChip;
            this.alloyButton.setMessage(getAlloyText());
        }
        if (this.dumpButton != null) {
            this.dumpButton.visible = hasChip;
        }
        if (this.ejectButton != null) {
            this.ejectButton.visible = hasChip;
            this.ejectButton.setMessage(getEjectText());
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Base machine frame
        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // 1. Lava Gauge at x + 10, y + 18 (width 12, height 50)
        int lavaHeight = this.menu.getScaledLava(50);
        if (lavaHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 10, y + 68 - lavaHeight, 176.0f, 50.0f - lavaHeight, 12, lavaHeight, 256, 256);
        }

        // 2. Energy Gauge at x + 26, y + 18 (width 12, height 50)
        int energyHeight = this.menu.getScaledEnergy(50);
        if (energyHeight > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 26, y + 68 - energyHeight, 188.0f, 50.0f - energyHeight, 12, energyHeight, 256, 256);
        }

        // 3. Smelting Flame/Progress at x + 76, y + 42 (width 24, height 17)
        int cookWidth = this.menu.getScaledCookProgress(24);
        if (cookWidth > 0) {
            context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x + 76, y + 42, 200.0f, 0.0f, cookWidth, 17, 256, 256);
        }

        // 4. Molten Metal Reservoir (Internal Tank 3) at x + 142, y + 26 (width 14, height 50)
        int fluidHeight = this.menu.getScaledMoltenVolume(50);
        if (fluidHeight > 0) {
            MoltenMetal top = this.menu.getMostAbundantFluid();
            int color = top.getColor() | 0xFF000000;
            context.fill(x + 142, y + 76 - fluidHeight, x + 142 + 14, y + 76, color);
        }

        // 5. High-Tech "Mixing Bay" Side Panel (appears when Mixing Chip is installed)
        if (this.menu.hasChip()) {
            int panelX = x + 176;
            int panelY = y + 16;
            int panelW = 66;
            int panelH = 126;

            // Panel outer beveled frame
            context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF2C2C32);
            context.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + panelH - 1, 0xFFC6C6C6);
            context.fill(panelX + 2, panelY + 2, panelX + panelW - 2, panelY + 14, 0xFF3F3F48);

            // Mixing Bay Header Text
            context.text(this.font, Component.literal("MIX BAY"), panelX + 13, panelY + 4, 0xFFFFCC00, true);

            // --- Tank 1 Gauge (Fed by Input 1) at panelX + 8, panelY + 18 (width 20, height 44) ---
            int t1X = panelX + 8;
            int t1Y = panelY + 16;
            int tankW = 21;
            int tankH = 46;
            // Tank 1 border & well
            context.fill(t1X, t1Y, t1X + tankW, t1Y + tankH, 0xFF373737);
            context.fill(t1X + 1, t1Y + 1, t1X + tankW - 1, t1Y + tankH - 1, 0xFF18181B);
            int t1Fill = this.menu.getScaledTank1(tankH - 2);
            if (t1Fill > 0) {
                int col1 = this.menu.getTank1Metal().getColor() | 0xFF000000;
                context.fill(t1X + 1, (t1Y + tankH - 1) - t1Fill, t1X + tankW - 1, t1Y + tankH - 1, col1);
            }

            // --- Tank 2 Gauge (Fed by Input 2) at panelX + 37, panelY + 18 (width 20, height 44) ---
            int t2X = panelX + 37;
            int t2Y = panelY + 16;
            // Tank 2 border & well
            context.fill(t2X, t2Y, t2X + tankW, t2Y + tankH, 0xFF373737);
            context.fill(t2X + 1, t2Y + 1, t2X + tankW - 1, t2Y + tankH - 1, 0xFF18181B);
            int t2Fill = this.menu.getScaledTank2(tankH - 2);
            if (t2Fill > 0) {
                int col2 = this.menu.getTank2Metal().getColor() | 0xFF000000;
                context.fill(t2X + 1, (t2Y + tankH - 1) - t2Fill, t2X + tankW - 1, t2Y + tankH - 1, col2);
            }

            // Tank badges
            context.text(this.font, Component.literal("§eT1"), t1X + 5, t1Y + tankH - 10, 0xFFFFFF, true);
            context.text(this.font, Component.literal("§eT2"), t2X + 5, t2Y + tankH - 10, 0xFFFFFF, true);

            // Subtext indicator
            context.text(this.font, Component.literal("§8Hold Only"), panelX + 8, panelY + 104, 0x555555, false);
            context.text(this.font, Component.literal("§8Mix->Tank3"), panelX + 6, panelY + 114, 0x555555, false);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.text(this.font, this.title, 8, 6, 4210752, false);
        context.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);


        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Lava Tooltip
        if (mouseX >= x + 9 && mouseX <= x + 23 && mouseY >= y + 17 && mouseY <= y + 69) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§c🔥 Thermal Lava Reservoir"),
                    Component.literal(String.format("§6%,d / %,d mB", this.menu.getLava(), this.menu.getMaxLava())),
                    Component.literal("§7Draw: 5 mB / smelt cycle")
            ), mouseX, mouseY);
        }

        // Energy Tooltip
        if (mouseX >= x + 25 && mouseX <= x + 39 && mouseY >= y + 17 && mouseY <= y + 69) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§6⚡ Induction Energy Coils"),
                    Component.literal(String.format("§e%,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy())),
                    Component.literal("§7Draw: 45 FE/t per active melting slot")
            ), mouseX, mouseY);
        }

        // Dual Smelting Progress Tooltip
        if (mouseX >= x + 75 && mouseX <= x + 101 && mouseY >= y + 41 && mouseY <= y + 60) {
            int p1 = this.menu.getScaledCookProgress1(100);
            int p2 = this.menu.getScaledCookProgress2(100);
            List<Component> pTooltip = new ArrayList<>();
            pTooltip.add(Component.literal("§e🔥 Induction Liquefaction"));
            pTooltip.add(Component.literal(String.format("§7Input Slot 1 (-> Tank 1): §f%s", p1 > 0 ? p1 + "%" : "Idle")));
            pTooltip.add(Component.literal(String.format("§7Input Slot 2 (-> Tank 2): §f%s", p2 > 0 ? p2 + "%" : "Idle")));
            pTooltip.add(Component.literal("§8Simultaneous dual melting supported."));
            context.setComponentTooltipForNextFrame(this.font, pTooltip, mouseX, mouseY);
        }

        // Molten Metal Chamber Tooltip (Internal Tank 3)
        if (mouseX >= x + 141 && mouseX <= x + 157 && mouseY >= y + 25 && mouseY <= y + 77) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("§d💧 Internal Tank 3 (Mix & Output Reservoir)"));
            int total = this.menu.getTotalMoltenVolume();
            tooltip.add(Component.literal(String.format("§fTotal Stored: §b%,d / 32,400 mB", total)));
            MoltenMetal top = this.menu.getMostAbundantFluid();
            if (top != MoltenMetal.NONE && total > 0) {
                tooltip.add(Component.literal(String.format("§7Top Fluid: §e%s", top.getDisplayName())));
            } else {
                tooltip.add(Component.literal("§8Chamber Empty"));
            }
            tooltip.add(Component.literal("§a✔ Auto-Emptying to Pipes: ACTIVE"));
            tooltip.add(Component.literal("§8Connect pipes or Casting Port to extract."));
            context.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
        }

        // Mixing Bay Tooltips (When Chip is Installed)
        if (this.menu.hasChip()) {
            int panelX = x + 176;
            int panelY = y + 16;
            int t1X = panelX + 8;
            int t1Y = panelY + 16;
            int t2X = panelX + 37;
            int t2Y = panelY + 16;
            int tankW = 21;
            int tankH = 46;

            // Holding Tank 1 Tooltip
            if (mouseX >= t1X && mouseX <= t1X + tankW && mouseY >= t1Y && mouseY <= t1Y + tankH) {
                MoltenMetal m1 = this.menu.getTank1Metal();
                int amt1 = this.menu.getTank1Amount();
                List<Component> t1Tip = new ArrayList<>();
                t1Tip.add(Component.literal("§6💧 Holding Tank 1 (Primary Feed)"));
                t1Tip.add(Component.literal(String.format("§7Fluid: §f%s", m1 != MoltenMetal.NONE ? m1.getDisplayName() : "Empty")));
                t1Tip.add(Component.literal(String.format("§7Stored: §b%,d / 10,800 mB", amt1)));
                t1Tip.add(Component.literal("§7Fed by: §eInput Slot 1"));
                t1Tip.add(Component.literal("§8Auto-Emptying: §cDISABLED (Protected for Mixing)"));
                t1Tip.add(Component.literal("§8Turn MIX ON to synthesize, or click [⏏] to eject."));
                context.setComponentTooltipForNextFrame(this.font, t1Tip, mouseX, mouseY);
            }

            // Holding Tank 2 Tooltip
            if (mouseX >= t2X && mouseX <= t2X + tankW && mouseY >= t2Y && mouseY <= t2Y + tankH) {
                MoltenMetal m2 = this.menu.getTank2Metal();
                int amt2 = this.menu.getTank2Amount();
                List<Component> t2Tip = new ArrayList<>();
                t2Tip.add(Component.literal("§6💧 Holding Tank 2 (Secondary Feed)"));
                t2Tip.add(Component.literal(String.format("§7Fluid: §f%s", m2 != MoltenMetal.NONE ? m2.getDisplayName() : "Empty")));
                t2Tip.add(Component.literal(String.format("§7Stored: §b%,d / 10,800 mB", amt2)));
                t2Tip.add(Component.literal("§7Fed by: §eInput Slot 2"));
                t2Tip.add(Component.literal("§8Auto-Emptying: §cDISABLED (Protected for Mixing)"));
                t2Tip.add(Component.literal("§8Turn MIX ON to synthesize, or click [⏏] to eject."));
                context.setComponentTooltipForNextFrame(this.font, t2Tip, mouseX, mouseY);
            }

            // Mix Toggle Tooltip
            if (mouseX >= panelX + 6 && mouseX <= panelX + 60 && mouseY >= panelY + 68 && mouseY <= panelY + 83) {
                boolean on = this.menu.isAlloyingEnabled();
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6⚡ Metallurgy Logic Controller"),
                        Component.literal(on ? "§aStatus: ACTIVE (Thermal Alloying ON)" : "§7Status: INACTIVE (Pure Metal Smelt)"),
                        Component.literal("§8When ON: Reacts Tank 1 & Tank 2 -> Outputs to Tank 3."),
                        Component.literal("§8When OFF: Melts two metals into holding tanks without mixing.")
                ), mouseX, mouseY);
            }

            // Dump Button Tooltip
            if (mouseX >= panelX + 6 && mouseX <= panelX + 32 && mouseY >= panelY + 86 && mouseY <= panelY + 101) {
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§c🗑 Dump Holding Tanks 1 & 2"),
                        Component.literal("§7Instantly and permanently destroys molten"),
                        Component.literal("§7metal contained inside Holding Tanks 1 & 2."),
                        Component.literal("§8Use if you melted the wrong metal type.")
                ), mouseX, mouseY);
            }

            // Eject Button Tooltip
            if (mouseX >= panelX + 34 && mouseX <= panelX + 60 && mouseY >= panelY + 86 && mouseY <= panelY + 101) {
                boolean ejecting = this.menu.isEjectingHoldingTanks();
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§b⏏ Eject Holding Tanks to Pipes"),
                        Component.literal("§7Actively pumps fluids from Holding Tanks 1 & 2"),
                        Component.literal("§7into connected pipes or external holding tanks."),
                        Component.literal(ejecting ? "§aStatus: PUMPING INTO PIPES..." : "§7Status: IDLE (Click to begin ejecting)"),
                        Component.literal("§8External tank must be connected to receive fluid.")
                ), mouseX, mouseY);
            }
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 6) {
            switch (this.hoveredSlot.index) {
                case 0 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§c🔥 Metal Liquefaction Chamber (Primary)"),
                        Component.literal("§7Accepts: §fOres, Raw Chunks, Ingots, Nuggets, Blocks,"),
                        Component.literal("§7         §fSwords, Tools, Armor, Horse Armor, Anvils, Chains"),
                        Component.literal("§a• 100% Zero-Loss Metal Value Reclaim:"),
                        Component.literal("§f  Nugget = 10 mB | Ingot = 90 mB | Block = 810 mB"),
                        Component.literal("§8Melts into Holding Tank 1 (when chip installed) or Tank 3.")
                ), mouseX, mouseY);
                case 1 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e🔥 Secondary Metal Feed / Alloying Flux"),
                        Component.literal("§7Accepts: §fSecondary metal stream for continuous melting"),
                        Component.literal("§7         §for stoichiometric alloying (e.g. Tin to pair with Copper)"),
                        Component.literal("§8Melts simultaneously into Holding Tank 2.")
                ), mouseX, mouseY);
                case 2 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6⚡ Metallurgy Controller Socket"),
                        Component.literal("§7Insert: §eMetallurgy Controller Chip"),
                        Component.literal("§f• Unlocks the §6Mixing Bay §fwith Holding Tanks 1 & 2"),
                        Component.literal("§f• Enables stoichiometric thermal alloying reactions:"),
                        Component.literal("§7  30 mB Cu + 10 mB Sn -> 40 mB Bronze"),
                        Component.literal("§7  10 mB Co + 10 mB Ar -> 20 mB Manyullyn"),
                        Component.literal("§8Holding tanks do not auto-empty, preventing mix loss.")
                ), mouseX, mouseY);
                case 3 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6⚙ Smelter Induction Overclock Socket"),
                        Component.literal("§7Accepts: §fCopper..Diamond Gears §7or §6Blaze Overclock Core"),
                        Component.literal("§8Overclocks thermal coils up to 4.0× smelting speed.")
                ), mouseX, mouseY);
                case 4 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§c🔥 Thermal Fuel Intake"),
                        Component.literal("§7Insert: §fLava Bucket §7(Iron, Copper, or Enchanted)"),
                        Component.literal("§7Fills internal 10,000 mB thermal lava reservoir."),
                        Component.literal("§8(Consumes 5 mB per smelting cycle)")
                ), mouseX, mouseY);
                case 5 -> context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§7🪣 Empty Fuel Bucket Return"),
                        Component.literal("§7Outputs emptied buckets after thermal refueling."),
                        Component.literal("§8Can be extracted automatically with pipes or hoppers.")
                ), mouseX, mouseY);
            }
        }
    }
}

