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

    public static final int STATUS_MINING = 0;
    public static final int STATUS_PAUSED = 1;
    public static final int STATUS_NO_ENERGY = 2;
    public static final int STATUS_BUFFER_FULL = 3;
    public static final int STATUS_COMPLETED = 4;

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
            context.fill(x + 8, energyY, x + 17, y + 18 + 54, 0xFFE53935);
        }

        // Telemetry Screen Panel (x + 19, y + 62, w: 56, h: 19)
        int scanY = this.handler.getScanY();
        int startY = this.handler.getStartY();
        int minY = this.handler.getMinY();
        int totalDepth = Math.max(1, startY - minY + 1);
        int depthDone = Math.max(0, startY - scanY);
        int overallPct = Math.min(100, Math.max(0, (depthDone * 100) / totalDepth));

        // High-tech sci-fi screen bezel and glass
        context.fill(x + 19, y + 62, x + 75, y + 81, 0xFF141923);
        context.fill(x + 20, y + 63, x + 74, y + 80, 0xFF080D12);

        // Micro Progress Bar groove along the bottom of the LCD (h: 2px)
        int barY = y + 78;
        context.fill(x + 20, barY, x + 74, barY + 2, 0xFF101922);
        int fillW = Math.max(0, Math.min(54, (overallPct * 54) / 100));
        if (fillW > 0) {
            int barColor = (scanY < minY) ? 0xFF00FF66 : 0xFF00E5FF;
            context.fill(x + 20, barY, x + 20 + fillW, barY + 2, barColor);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);

        int scanY = this.handler.getScanY();
        int startY = this.handler.getStartY();
        int minY = this.handler.getMinY();
        int totalDepth = Math.max(1, startY - minY + 1);
        int depthDone = Math.max(0, startY - scanY);
        int overallPct = Math.min(100, Math.max(0, (depthDone * 100) / totalDepth));

        // 1. Header Level & Progress Summary (x: 76, y: 6)
        if (scanY < minY) {
            context.drawText(this.textRenderer, Text.literal("§a✔ Bedrock (Done)"), 76, 6, 0xFF00AA44, false);
        } else {
            context.drawText(this.textRenderer, Text.literal("§8Lvl: §bY=" + scanY + " §8(§a" + overallPct + "%§8)"), 76, 6, 0xFF404040, false);
        }

        // 2. LCD Display Console (x: 22)
        // Line 1 (y: 64): Working Depth & Depth %
        if (scanY < minY) {
            context.drawText(this.textRenderer, Text.literal("§aBEDROCK"), 22, 64, 0xFF00FF66, false);
        } else {
            String lvlStr = "§bY:" + scanY + " §a" + overallPct + "%";
            context.drawText(this.textRenderer, Text.literal(lvlStr), 22, 64, 0xFF00E5FF, false);
        }

        // Line 2 (y: 71): ETA or Machine State
        int status = this.handler.getDetailedStatusCode();
        int etaSec = this.handler.getEtaSeconds();
        if (scanY < minY) {
            context.drawText(this.textRenderer, Text.literal("§a✔ COMPLETE"), 22, 71, 0xFF00FF66, false);
        } else if (status == STATUS_PAUSED) {
            context.drawText(this.textRenderer, Text.literal("§e⏸ PAUSED"), 22, 71, 0xFFFFCC00, false);
        } else if (status == STATUS_NO_ENERGY) {
            context.drawText(this.textRenderer, Text.literal("§c⚡ NO POWER"), 22, 71, 0xFFFF5555, false);
        } else if (status == STATUS_BUFFER_FULL) {
            context.drawText(this.textRenderer, Text.literal("§6📦 FULL"), 22, 71, 0xFFFF9900, false);
        } else {
            String etaStr = "§e⏱ " + formatEta(etaSec);
            context.drawText(this.textRenderer, Text.literal(etaStr), 22, 71, 0xFFFFD54F, false);
        }

        // 3. Digital Storage Network status icon (x: 162, y: 6) with full alpha
        int netStatus = this.handler.getNetworkStatus();
        if (netStatus == 2) {
            context.drawText(this.textRenderer, Text.literal("●"), 162, 6, 0xFF55FFFF, false);
        } else if (netStatus == 1) {
            context.drawText(this.textRenderer, Text.literal("●"), 162, 6, 0xFF55FF55, false);
        } else if (netStatus == 3 || netStatus == 4) {
            context.drawText(this.textRenderer, Text.literal("●"), 162, 6, 0xFFFF5555, false);
        } else {
            context.drawText(this.textRenderer, Text.literal("○"), 162, 6, 0xFFAAAAAA, false);
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

        // Telemetry LCD Hover Tooltip (x + 19 .. 75, y + 62 .. 81)
        if (mouseX >= x + 19 && mouseX <= x + 75 && mouseY >= y + 62 && mouseY <= y + 81) {
            int scanY = this.handler.getScanY();
            int startY = this.handler.getStartY();
            int minY = this.handler.getMinY();
            int totalDepth = Math.max(1, startY - minY + 1);
            int depthDone = Math.max(0, startY - scanY);
            int overallPct = Math.min(100, Math.max(0, (depthDone * 100) / totalDepth));
            int status = this.handler.getDetailedStatusCode();
            int etaSec = this.handler.getEtaSeconds();

            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("§b📡 Laser Quarry Telemetry"));
            lines.add(Text.literal("§8──────────────────────────────────"));
            lines.add(Text.literal("§7Current Working Level: §bY = " + scanY));
            lines.add(Text.literal("§7Starting Depth:        §8Y = " + startY));
            lines.add(Text.literal("§7Target Bedrock:        §8Y = " + minY));
            lines.add(Text.literal("§7Depth Completed:       §a" + overallPct + "% §8[" + getProgressBar(overallPct) + "§8]"));
            lines.add(Text.literal("§7Current Layer Progress:§3 " + this.handler.getLayerProgressPercent() + "%"));

            String etaDisplay = (scanY < minY) ? "§aCompleted (Bedrock)" : (status == STATUS_PAUSED ? "§ePaused §7(~" + formatEta(etaSec) + " remaining)" : "§e~" + formatEta(etaSec));
            lines.add(Text.literal("§7Estimated Finish Time: " + etaDisplay));

            int radius = this.handler.getRangeChunkRadius();
            String radiusStr = (radius == 0) ? "1x1 Chunk (16x16)" : (radius == 1 ? "3x3 Chunks (48x48)" : "5x5 Chunks (80x80)");
            lines.add(Text.literal("§7Mining Area:           §b" + radiusStr));
            lines.add(Text.literal(String.format("§7Total Blocks Mined:    §f%,d", this.handler.getTotalMinedCount())));

            String statusStr = switch (status) {
                case STATUS_COMPLETED -> "§a✔ Completed (Reached Bedrock)";
                case STATUS_PAUSED -> "§e⏸ Paused";
                case STATUS_NO_ENERGY -> "§c⚡ Out of Energy";
                case STATUS_BUFFER_FULL -> "§6📦 Output Buffer Full";
                default -> "§a● Actively Mining";
            };
            lines.add(Text.literal("§7Operational Status:    " + statusStr));

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
            lines.add(Text.literal("§5Interdimensional Card§7, §bChunk Loader Module§7,"));
            lines.add(Text.literal("§eor §aWireless Storage Crystal"));
            lines.add(Text.literal("§8Provides drop multipliers, auto chunk-loading, or"));
            lines.add(Text.literal("§8quantum cross-dimensional wireless linking."));
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

        // Empty Extraction Buffer Slots (0..8)
        if (this.focusedSlot != null && !this.focusedSlot.hasStack() && this.focusedSlot.id < 9) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§a⛏️ Mined Resource Buffer"),
                    Text.literal("§7Extracted ores and quarried blocks appear here."),
                    Text.literal("§8(Automatically piped or teleported to linked storage)")
            ), mouseX, mouseY);
        }
    }

    public static String formatEta(int seconds) {
        if (seconds <= 0) return "0s";
        if (seconds < 60) return seconds + "s";
        int mins = seconds / 60;
        int secs = seconds % 60;
        if (mins < 60) {
            return String.format("%dm %02ds", mins, secs);
        }
        int hours = mins / 60;
        int remMins = mins % 60;
        return String.format("%dh %02dm", hours, remMins);
    }

    private static String getProgressBar(int percent) {
        int bars = Math.max(0, Math.min(10, percent / 10));
        return "§a" + "■".repeat(bars) + "§8" + "■".repeat(10 - bars);
    }
}
