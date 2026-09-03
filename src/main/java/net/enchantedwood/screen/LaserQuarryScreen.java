package net.enchantedwood.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class LaserQuarryScreen extends HandledScreen<LaserQuarryScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/laser_quarry_gui.png");

    private ButtonWidget modeButton;
    private ButtonWidget pauseButton;

    public LaserQuarryScreen(LaserQuarryScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        this.titleX = 8;
        this.titleY = 6;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        this.modeButton = ButtonWidget.builder(getModeText(), button -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 0);
            }
        }).dimensions(x + 20, y + 20, 54, 18).build();

        this.pauseButton = ButtonWidget.builder(getPauseText(), button -> {
            if (this.client != null && this.client.interactionManager != null) {
                this.client.interactionManager.clickButton(this.handler.syncId, 1);
            }
        }).dimensions(x + 20, y + 42, 54, 18).build();

        this.addDrawableChild(this.modeButton);
        this.addDrawableChild(this.pauseButton);
    }

    private Text getModeText() {
        return (this.handler.getMode() == 0) ? Text.literal("💎 Ores") : Text.literal("🕳️ Clear");
    }

    private Text getPauseText() {
        if (this.handler.isPaused()) {
            return (this.handler.getTotalMinedCount() == 0) ? Text.literal("▶ Start") : Text.literal("▶ Resume");
        }
        return Text.literal("⏸ Pause");
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        if (this.modeButton != null) {
            this.modeButton.setMessage(getModeText());
        }
        if (this.pauseButton != null) {
            this.pauseButton.setMessage(getPauseText());
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // Draw Energy Vertical Gauge (x + 8, y + 18, w: 8, h: 54)
        int energy = this.handler.getEnergy();
        int maxEnergy = this.handler.getMaxEnergy();
        if (maxEnergy > 0 && energy > 0) {
            int scaledH = Math.min(54, (int) ((long) energy * 54 / maxEnergy));
            int energyY = (y + 18) + (54 - scaledH);
            context.fill(x + 8, energyY, x + 16, y + 18 + 54, 0xFFE53935);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);

        // Telemetry LCD Display (x: 21..73, y: 64..75)
        String depthStr = "Y:" + this.handler.getScanY();
        int radius = this.handler.getRangeChunkRadius();
        String radiusStr = (radius == 0) ? "1x1" : (radius == 1 ? "3x3" : "5x5");
        String tele = depthStr + " " + radiusStr;
        context.drawText(this.textRenderer, Text.literal(tele).formatted(net.minecraft.util.Formatting.AQUA), 23, 66, 0x55FFFF, false);

        // Digital Storage Network status icon
        int netStatus = this.handler.getNetworkStatus();
        if (netStatus == 2) {
            context.drawText(this.textRenderer, Text.literal("●").formatted(net.minecraft.util.Formatting.AQUA), 162, 6, 0x55FFFF, false);
        } else if (netStatus == 1) {
            context.drawText(this.textRenderer, Text.literal("●").formatted(net.minecraft.util.Formatting.GREEN), 162, 6, 0x55FF55, false);
        } else {
            context.drawText(this.textRenderer, Text.literal("●").formatted(net.minecraft.util.Formatting.RED), 162, 6, 0xFF5555, false);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Energy Bar Hover Tooltip (x + 8 .. 16, y + 18 .. 72)
        if (mouseX >= x + 8 && mouseX <= x + 16 && mouseY >= y + 18 && mouseY <= y + 72) {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("§e⚡ Laser Quarry Energy"));
            lines.add(Text.literal(String.format("§f%,d / %,d FE", this.handler.getEnergy(), this.handler.getMaxEnergy())));
            lines.add(Text.literal("§7Consumes 150 FE per block extracted."));
            lines.add(Text.literal("§8Powered by cables or connected Storage Network."));
            context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        }

        // Speed Upgrade Slot Tooltip (x + 151 .. 169, y + 17 .. 35)
        if (mouseX >= x + 151 && mouseX <= x + 169 && mouseY >= y + 17 && mouseY <= y + 35) {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("§6⚡ Overclock & Speed Socket"));
            lines.add(Text.literal("§7Accepts: Gears (Copper..Diamond) or §6Blaze Overclock Core"));
            lines.add(Text.literal("§8Scales speed up to 20 blocks/second."));
            context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        }

        // Range Upgrade Slot Tooltip (x + 151 .. 169, y + 35 .. 53)
        if (mouseX >= x + 151 && mouseX <= x + 169 && mouseY >= y + 35 && mouseY <= y + 53) {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("§b📡 Range Expansion Socket"));
            lines.add(Text.literal("§7Accepts: §bTier 1 Core (3x3 Chunks) §7or §dTier 2 Core (5x5 Chunks)"));
            lines.add(Text.literal("§8Expands scanning and laser perimeter area."));
            context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        }

        // Utility / Extraction Socket Tooltip (x + 151 .. 169, y + 53 .. 71)
        if (mouseX >= x + 151 && mouseX <= x + 169 && mouseY >= y + 53 && mouseY <= y + 71) {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("§a🔮 Utility & Extraction Socket"));
            lines.add(Text.literal("§7Accepts: §6Fortune Core§7, §aSilk Touch Core§7,"));
            lines.add(Text.literal("§5Interdimensional Card§7, or §bChunk Loader Module"));
            lines.add(Text.literal("§8Provides drop multipliers, auto chunk-loading, or"));
            lines.add(Text.literal("§8direct cross-dimensional link bridging."));
            context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        }

        // Network Status Tooltip (x + 158 .. 170, y + 4 .. 16)
        if (mouseX >= x + 158 && mouseX <= x + 170 && mouseY >= y + 4 && mouseY <= y + 16) {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("§b🌐 Digital Storage Link"));
            int netStatus = this.handler.getNetworkStatus();
            if (netStatus == 2) {
                lines.add(Text.literal("§b● Linked: Quantum Interdimensional Link Active"));
                lines.add(Text.literal("§7Teleporting mined ores directly across dimensions!"));
                lines.add(Text.literal("§7Draws operating FE wirelessly from Base Grid."));
            } else if (netStatus == 1) {
                lines.add(Text.literal("§a● Online: Connected to Base Storage Network"));
                lines.add(Text.literal("§7Mined ores directly deposit into connected storage."));
            } else if (netStatus == 3) {
                lines.add(Text.literal("§c● Blocked: Missing Interdimensional Card"));
                lines.add(Text.literal("§eInstall an Interdimensional Card in either the"));
                lines.add(Text.literal("§eBase Storage Controller or this Quarry's Utility Socket!"));
            } else if (netStatus == 4) {
                lines.add(Text.literal("§c● Offline: Base Network Unreachable"));
                lines.add(Text.literal("§7Base chunk may be unloaded or Controller out of power."));
                lines.add(Text.literal("§eInstall an Interdimensional Card or Chunk Loader in Controller!"));
            } else {
                lines.add(Text.literal("§7○ Unbound: No Remote Network Linked"));
                lines.add(Text.literal("§7Mined items store in internal buffer or adjacent chests."));
                lines.add(Text.literal("§8Sneak + Right-Click Wrench on Base Controller, then Quarry to link."));
            }
            context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        }
    }
}
