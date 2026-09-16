package net.enchantedwood.screen;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.fluid.MoltenMetal;
import net.enchantedwood.network.InductionSmelterActionPayload;
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
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class InductionSmelterScreen extends HandledScreen<InductionSmelterScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/induction_smelter_gui.png");

    private ButtonWidget alloyButton;
    private ButtonWidget dumpButton;
    private ButtonWidget ejectButton;

    public InductionSmelterScreen(InductionSmelterScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    private BlockPos getTargetPos() {
        if (this.client != null && this.client.crosshairTarget instanceof BlockHitResult hitResult) {
            return hitResult.getBlockPos();
        }
        return BlockPos.ORIGIN;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        int panelX = x + 176;
        int panelY = y + 16;

        // 1. Alloy Toggle Button
        this.alloyButton = ButtonWidget.builder(getAlloyText(), button -> {
            ClientPlayNetworking.send(new InductionSmelterActionPayload(getTargetPos(), 0));
        }).dimensions(panelX + 6, panelY + 68, 54, 15).build();

        // 2. Dump / Purge Button (Destroys Tanks 1 & 2)
        this.dumpButton = ButtonWidget.builder(Text.literal("§c🗑 Dump"), button -> {
            ClientPlayNetworking.send(new InductionSmelterActionPayload(getTargetPos(), 1));
        }).dimensions(panelX + 6, panelY + 86, 26, 15).build();

        // 3. Eject Button (Pumps Tanks 1 & 2 into Pipes)
        this.ejectButton = ButtonWidget.builder(getEjectText(), button -> {
            ClientPlayNetworking.send(new InductionSmelterActionPayload(getTargetPos(), 2));
        }).dimensions(panelX + 34, panelY + 86, 26, 15).build();

        this.addDrawableChild(this.alloyButton);
        this.addDrawableChild(this.dumpButton);
        this.addDrawableChild(this.ejectButton);
    }

    private Text getAlloyText() {
        return this.handler.isAlloyingEnabled() ? Text.literal("§a⚡ MIX: ON") : Text.literal("§7○ MIX: OFF");
    }

    private Text getEjectText() {
        return this.handler.isEjectingHoldingTanks() ? Text.literal("§b⏏...") : Text.literal("§b⏏");
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        boolean hasChip = this.handler.hasChip();
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
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Base machine frame
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

        // 4. Molten Metal Reservoir (Internal Tank 3) at x + 142, y + 26 (width 14, height 50)
        int fluidHeight = this.handler.getScaledMoltenVolume(50);
        if (fluidHeight > 0) {
            MoltenMetal top = this.handler.getMostAbundantFluid();
            int color = top.getColor() | 0xFF000000;
            context.fill(x + 142, y + 76 - fluidHeight, x + 142 + 14, y + 76, color);
        }

        // 5. High-Tech "Mixing Bay" Side Panel (appears when Mixing Chip is installed)
        if (this.handler.hasChip()) {
            int panelX = x + 176;
            int panelY = y + 16;
            int panelW = 66;
            int panelH = 126;

            // Panel outer beveled frame
            context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF2C2C32);
            context.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + panelH - 1, 0xFFC6C6C6);
            context.fill(panelX + 2, panelY + 2, panelX + panelW - 2, panelY + 14, 0xFF3F3F48);

            // Mixing Bay Header Text
            context.drawText(this.textRenderer, Text.literal("MIX BAY"), panelX + 13, panelY + 4, 0xFFFFCC00, true);

            // --- Tank 1 Gauge (Fed by Input 1) at panelX + 8, panelY + 18 (width 20, height 44) ---
            int t1X = panelX + 8;
            int t1Y = panelY + 16;
            int tankW = 21;
            int tankH = 46;
            // Tank 1 border & well
            context.fill(t1X, t1Y, t1X + tankW, t1Y + tankH, 0xFF373737);
            context.fill(t1X + 1, t1Y + 1, t1X + tankW - 1, t1Y + tankH - 1, 0xFF18181B);
            int t1Fill = this.handler.getScaledTank1(tankH - 2);
            if (t1Fill > 0) {
                int col1 = this.handler.getTank1Metal().getColor() | 0xFF000000;
                context.fill(t1X + 1, (t1Y + tankH - 1) - t1Fill, t1X + tankW - 1, t1Y + tankH - 1, col1);
            }

            // --- Tank 2 Gauge (Fed by Input 2) at panelX + 37, panelY + 18 (width 20, height 44) ---
            int t2X = panelX + 37;
            int t2Y = panelY + 16;
            // Tank 2 border & well
            context.fill(t2X, t2Y, t2X + tankW, t2Y + tankH, 0xFF373737);
            context.fill(t2X + 1, t2Y + 1, t2X + tankW - 1, t2Y + tankH - 1, 0xFF18181B);
            int t2Fill = this.handler.getScaledTank2(tankH - 2);
            if (t2Fill > 0) {
                int col2 = this.handler.getTank2Metal().getColor() | 0xFF000000;
                context.fill(t2X + 1, (t2Y + tankH - 1) - t2Fill, t2X + tankW - 1, t2Y + tankH - 1, col2);
            }

            // Tank badges
            context.drawText(this.textRenderer, Text.literal("§eT1"), t1X + 5, t1Y + tankH - 10, 0xFFFFFF, true);
            context.drawText(this.textRenderer, Text.literal("§eT2"), t2X + 5, t2Y + tankH - 10, 0xFFFFFF, true);

            // Subtext indicator
            context.drawText(this.textRenderer, Text.literal("§8Hold Only"), panelX + 8, panelY + 104, 0x555555, false);
            context.drawText(this.textRenderer, Text.literal("§8Mix->Tank3"), panelX + 6, panelY + 114, 0x555555, false);
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
                    Text.literal("§7Draw: 45 FE/t per active melting slot")
            ), mouseX, mouseY);
        }

        // Dual Smelting Progress Tooltip
        if (mouseX >= x + 75 && mouseX <= x + 101 && mouseY >= y + 41 && mouseY <= y + 60) {
            int p1 = this.handler.getScaledCookProgress1(100);
            int p2 = this.handler.getScaledCookProgress2(100);
            List<Text> pTooltip = new ArrayList<>();
            pTooltip.add(Text.literal("§e🔥 Induction Liquefaction"));
            pTooltip.add(Text.literal(String.format("§7Input Slot 1 (-> Tank 1): §f%s", p1 > 0 ? p1 + "%" : "Idle")));
            pTooltip.add(Text.literal(String.format("§7Input Slot 2 (-> Tank 2): §f%s", p2 > 0 ? p2 + "%" : "Idle")));
            pTooltip.add(Text.literal("§8Simultaneous dual melting supported."));
            context.drawTooltip(this.textRenderer, pTooltip, mouseX, mouseY);
        }

        // Molten Metal Chamber Tooltip (Internal Tank 3)
        if (mouseX >= x + 141 && mouseX <= x + 157 && mouseY >= y + 25 && mouseY <= y + 77) {
            List<Text> tooltip = new ArrayList<>();
            tooltip.add(Text.literal("§d💧 Internal Tank 3 (Mix & Output Reservoir)"));
            int total = this.handler.getTotalMoltenVolume();
            tooltip.add(Text.literal(String.format("§fTotal Stored: §b%,d / 32,400 mB", total)));
            MoltenMetal top = this.handler.getMostAbundantFluid();
            if (top != MoltenMetal.NONE && total > 0) {
                tooltip.add(Text.literal(String.format("§7Top Fluid: §e%s", top.getDisplayName())));
            } else {
                tooltip.add(Text.literal("§8Chamber Empty"));
            }
            tooltip.add(Text.literal("§a✔ Auto-Emptying to Pipes: ACTIVE"));
            tooltip.add(Text.literal("§8Connect pipes or Casting Port to extract."));
            context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
        }

        // Mixing Bay Tooltips (When Chip is Installed)
        if (this.handler.hasChip()) {
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
                MoltenMetal m1 = this.handler.getTank1Metal();
                int amt1 = this.handler.getTank1Amount();
                List<Text> t1Tip = new ArrayList<>();
                t1Tip.add(Text.literal("§6💧 Holding Tank 1 (Primary Feed)"));
                t1Tip.add(Text.literal(String.format("§7Fluid: §f%s", m1 != MoltenMetal.NONE ? m1.getDisplayName() : "Empty")));
                t1Tip.add(Text.literal(String.format("§7Stored: §b%,d / 10,800 mB", amt1)));
                t1Tip.add(Text.literal("§7Fed by: §eInput Slot 1"));
                t1Tip.add(Text.literal("§8Auto-Emptying: §cDISABLED (Protected for Mixing)"));
                t1Tip.add(Text.literal("§8Turn MIX ON to synthesize, or click [⏏] to eject."));
                context.drawTooltip(this.textRenderer, t1Tip, mouseX, mouseY);
            }

            // Holding Tank 2 Tooltip
            if (mouseX >= t2X && mouseX <= t2X + tankW && mouseY >= t2Y && mouseY <= t2Y + tankH) {
                MoltenMetal m2 = this.handler.getTank2Metal();
                int amt2 = this.handler.getTank2Amount();
                List<Text> t2Tip = new ArrayList<>();
                t2Tip.add(Text.literal("§6💧 Holding Tank 2 (Secondary Feed)"));
                t2Tip.add(Text.literal(String.format("§7Fluid: §f%s", m2 != MoltenMetal.NONE ? m2.getDisplayName() : "Empty")));
                t2Tip.add(Text.literal(String.format("§7Stored: §b%,d / 10,800 mB", amt2)));
                t2Tip.add(Text.literal("§7Fed by: §eInput Slot 2"));
                t2Tip.add(Text.literal("§8Auto-Emptying: §cDISABLED (Protected for Mixing)"));
                t2Tip.add(Text.literal("§8Turn MIX ON to synthesize, or click [⏏] to eject."));
                context.drawTooltip(this.textRenderer, t2Tip, mouseX, mouseY);
            }

            // Mix Toggle Tooltip
            if (mouseX >= panelX + 6 && mouseX <= panelX + 60 && mouseY >= panelY + 68 && mouseY <= panelY + 83) {
                boolean on = this.handler.isAlloyingEnabled();
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§6⚡ Metallurgy Logic Controller"),
                        Text.literal(on ? "§aStatus: ACTIVE (Thermal Alloying ON)" : "§7Status: INACTIVE (Pure Metal Smelt)"),
                        Text.literal("§8When ON: Reacts Tank 1 & Tank 2 -> Outputs to Tank 3."),
                        Text.literal("§8When OFF: Melts two metals into holding tanks without mixing.")
                ), mouseX, mouseY);
            }

            // Dump Button Tooltip
            if (mouseX >= panelX + 6 && mouseX <= panelX + 32 && mouseY >= panelY + 86 && mouseY <= panelY + 101) {
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§c🗑 Dump Holding Tanks 1 & 2"),
                        Text.literal("§7Instantly and permanently destroys molten"),
                        Text.literal("§7metal contained inside Holding Tanks 1 & 2."),
                        Text.literal("§8Use if you melted the wrong metal type.")
                ), mouseX, mouseY);
            }

            // Eject Button Tooltip
            if (mouseX >= panelX + 34 && mouseX <= panelX + 60 && mouseY >= panelY + 86 && mouseY <= panelY + 101) {
                boolean ejecting = this.handler.isEjectingHoldingTanks();
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§b⏏ Eject Holding Tanks to Pipes"),
                        Text.literal("§7Actively pumps fluids from Holding Tanks 1 & 2"),
                        Text.literal("§7into connected pipes or external holding tanks."),
                        Text.literal(ejecting ? "§aStatus: PUMPING INTO PIPES..." : "§7Status: IDLE (Click to begin ejecting)"),
                        Text.literal("§8External tank must be connected to receive fluid.")
                ), mouseX, mouseY);
            }
        }

        // Empty Machine Slot Tooltips
        if (this.focusedSlot != null && !this.focusedSlot.hasStack() && this.focusedSlot.id < 6) {
            switch (this.focusedSlot.id) {
                case 0 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§c🔥 Metal Liquefaction Chamber (Primary)"),
                        Text.literal("§7Accepts: §fOres, Raw Chunks, Ingots, Nuggets, Blocks,"),
                        Text.literal("§7         §fSwords, Tools, Armor, Horse Armor, Anvils, Chains"),
                        Text.literal("§a• 100% Zero-Loss Metal Value Reclaim:"),
                        Text.literal("§f  Nugget = 10 mB | Ingot = 90 mB | Block = 810 mB"),
                        Text.literal("§8Melts into Holding Tank 1 (when chip installed) or Tank 3.")
                ), mouseX, mouseY);
                case 1 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§e🔥 Secondary Metal Feed / Alloying Flux"),
                        Text.literal("§7Accepts: §fSecondary metal stream for continuous melting"),
                        Text.literal("§7         §for stoichiometric alloying (e.g. Tin to pair with Copper)"),
                        Text.literal("§8Melts simultaneously into Holding Tank 2.")
                ), mouseX, mouseY);
                case 2 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§6⚡ Metallurgy Controller Socket"),
                        Text.literal("§7Insert: §eMetallurgy Controller Chip"),
                        Text.literal("§f• Unlocks the §6Mixing Bay §fwith Holding Tanks 1 & 2"),
                        Text.literal("§f• Enables stoichiometric thermal alloying reactions:"),
                        Text.literal("§7  30 mB Cu + 10 mB Sn -> 40 mB Bronze"),
                        Text.literal("§7  10 mB Co + 10 mB Ar -> 20 mB Manyullyn"),
                        Text.literal("§8Holding tanks do not auto-empty, preventing mix loss.")
                ), mouseX, mouseY);
                case 3 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§6⚙ Smelter Induction Overclock Socket"),
                        Text.literal("§7Accepts: §fCopper..Diamond Gears §7or §6Blaze Overclock Core"),
                        Text.literal("§8Overclocks thermal coils up to 4.0× smelting speed.")
                ), mouseX, mouseY);
                case 4 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§c🔥 Thermal Fuel Intake"),
                        Text.literal("§7Insert: §fLava Bucket §7(Iron, Copper, or Enchanted)"),
                        Text.literal("§7Fills internal 10,000 mB thermal lava reservoir."),
                        Text.literal("§8(Consumes 5 mB per smelting cycle)")
                ), mouseX, mouseY);
                case 5 -> context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§7🪣 Empty Fuel Bucket Return"),
                        Text.literal("§7Outputs emptied buckets after thermal refueling."),
                        Text.literal("§8Can be extracted automatically with pipes or hoppers.")
                ), mouseX, mouseY);
            }
        }
    }
}

