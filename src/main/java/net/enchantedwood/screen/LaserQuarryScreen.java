package net.enchantedwood.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.block.entity.LaserQuarryBlockEntity;
import static net.enchantedwood.block.entity.LaserQuarryBlockEntity.*;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class LaserQuarryScreen extends AbstractContainerScreen<LaserQuarryScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/laser_quarry_gui.png");

    private Button modeButton;
    private Button pauseButton;

    public LaserQuarryScreen(LaserQuarryScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        this.modeButton = Button.builder(getModeText(), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            }
        }).bounds(x + 20, y + 20, 54, 18).build();

        this.pauseButton = Button.builder(getPauseText(), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
            }
        }).bounds(x + 20, y + 42, 54, 18).build();

        this.addRenderableWidget(this.modeButton);
        this.addRenderableWidget(this.pauseButton);
    }

    private Component getModeText() {
        return (this.menu.getMode() == 0) ? Component.literal("💎 Ores") : Component.literal("🕳️ Clear");
    }

    private Component getPauseText() {
        if (this.menu.isPaused()) {
            return (this.menu.getTotalMinedCount() == 0) ? Component.literal("▶ Start") : Component.literal("▶ Resume");
        }
        return Component.literal("⏸ Pause");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.modeButton != null) {
            this.modeButton.setMessage(getModeText());
        }
        if (this.pauseButton != null) {
            this.pauseButton.setMessage(getPauseText());
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // Draw Energy Vertical Gauge (x + 8, y + 18, w: 8, h: 54)
        int energy = this.menu.getEnergy();
        int maxEnergy = this.menu.getMaxEnergy();
        if (maxEnergy > 0 && energy > 0) {
            int scaledH = Math.min(54, (int) ((long) energy * 54 / maxEnergy));
            int energyY = (y + 18) + (54 - scaledH);
            context.fill(x + 8, energyY, x + 17, y + 18 + 54, 0xFFE53935);
        }

        // Telemetry Screen Panel (x + 19, y + 62, w: 56, h: 19)
        int scanY = this.menu.getScanY();
        int startY = this.menu.getStartY();
        int minY = this.menu.getMinY();
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
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractLabels(context, mouseX, mouseY);

        int scanY = this.menu.getScanY();
        int startY = this.menu.getStartY();
        int minY = this.menu.getMinY();
        int totalDepth = Math.max(1, startY - minY + 1);
        int depthDone = Math.max(0, startY - scanY);
        int overallPct = Math.min(100, Math.max(0, (depthDone * 100) / totalDepth));

        // 1. Header Level & Progress Summary (x: 76, y: 6)
        if (scanY < minY) {
            context.text(this.font, Component.literal("§a✔ Bedrock (Done)"), 76, 6, 0xFF00AA44, false);
        } else {
            context.text(this.font, Component.literal("§8Lvl: §bY=" + scanY + " §8(§a" + overallPct + "%§8)"), 76, 6, 0xFF404040, false);
        }

        // 2. LCD Display Console (x: 22)
        // Line 1 (y: 64): Working Depth & Depth %
        if (scanY < minY) {
            context.text(this.font, Component.literal("§aBEDROCK"), 22, 64, 0xFF00FF66, false);
        } else {
            String lvlStr = "§bY:" + scanY + " §a" + overallPct + "%";
            context.text(this.font, Component.literal(lvlStr), 22, 64, 0xFF00E5FF, false);
        }

        // Line 2 (y: 71): ETA or Machine State
        int status = this.menu.getDetailedStatusCode();
        int etaSec = this.menu.getEtaSeconds();
        if (scanY < minY) {
            context.text(this.font, Component.literal("§a✔ COMPLETE"), 22, 71, 0xFF00FF66, false);
        } else if (status == STATUS_PAUSED) {
            context.text(this.font, Component.literal("§e⏸ PAUSED"), 22, 71, 0xFFFFCC00, false);
        } else if (status == STATUS_NO_ENERGY) {
            context.text(this.font, Component.literal("§c⚡ NO POWER"), 22, 71, 0xFFFF5555, false);
        } else if (status == STATUS_BUFFER_FULL) {
            context.text(this.font, Component.literal("§6📦 FULL"), 22, 71, 0xFFFF9900, false);
        } else {
            String etaStr = "§e⏱ " + formatEta(etaSec);
            context.text(this.font, Component.literal(etaStr), 22, 71, 0xFFFFD54F, false);
        }

        // 3. Digital Storage Network status icon (x: 162, y: 6) with full alpha
        int netStatus = this.menu.getNetworkStatus();
        if (netStatus == 2) {
            context.text(this.font, Component.literal("●"), 162, 6, 0xFF55FFFF, false);
        } else if (netStatus == 1) {
            context.text(this.font, Component.literal("●"), 162, 6, 0xFF55FF55, false);
        } else if (netStatus == 3 || netStatus == 4) {
            context.text(this.font, Component.literal("●"), 162, 6, 0xFFFF5555, false);
        } else {
            context.text(this.font, Component.literal("○"), 162, 6, 0xFFAAAAAA, false);
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
        mins = mins % 60;
        if (hours < 24) {
            return String.format("%dh %02dm", hours, mins);
        }
        int days = hours / 24;
        hours = hours % 24;
        return String.format("%dd %dh", days, hours);
    }

    private String getProgressBar(int percent) {
        int totalBars = 10;
        int filled = Math.max(0, Math.min(totalBars, (percent * totalBars) / 100));
        return "§a" + "█".repeat(filled) + "§8" + "░".repeat(totalBars - filled);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);


        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Energy Bar Hover Tooltip (x + 8 .. 16, y + 18 .. 72)
        if (mouseX >= x + 8 && mouseX <= x + 16 && mouseY >= y + 18 && mouseY <= y + 72) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§e⚡ Laser Quarry Energy"));
            lines.add(Component.literal(String.format("§f%,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy())));
            lines.add(Component.literal("§7Consumes 150 FE per block extracted."));
            lines.add(Component.literal("§8Powered by cables or connected Storage Network."));
            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }

        // Telemetry LCD Hover Tooltip (x + 19 .. 75, y + 62 .. 81)
        if (mouseX >= x + 19 && mouseX <= x + 75 && mouseY >= y + 62 && mouseY <= y + 81) {
            int scanY = this.menu.getScanY();
            int startY = this.menu.getStartY();
            int minY = this.menu.getMinY();
            int totalDepth = Math.max(1, startY - minY + 1);
            int depthDone = Math.max(0, startY - scanY);
            int overallPct = Math.min(100, Math.max(0, (depthDone * 100) / totalDepth));
            int status = this.menu.getDetailedStatusCode();
            int etaSec = this.menu.getEtaSeconds();

            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§b📡 Laser Quarry Telemetry"));
            lines.add(Component.literal("§8──────────────────────────────────"));
            lines.add(Component.literal("§7Current Working Level: §bY = " + scanY));
            lines.add(Component.literal("§7Starting Depth:        §8Y = " + startY));
            lines.add(Component.literal("§7Target Bedrock:        §8Y = " + minY));
            lines.add(Component.literal("§7Depth Completed:       §a" + overallPct + "% §8[" + getProgressBar(overallPct) + "§8]"));
            lines.add(Component.literal("§7Current Layer Progress:§3 " + this.menu.getLayerProgressPercent() + "%"));

            String etaDisplay = (scanY < minY) ? "§aCompleted (Bedrock)" : (status == STATUS_PAUSED ? "§ePaused §7(~" + formatEta(etaSec) + " remaining)" : "§e~" + formatEta(etaSec));
            lines.add(Component.literal("§7Estimated Finish Time: " + etaDisplay));

            int radius = this.menu.getRangeChunkRadius();
            String radiusStr = (radius == 0) ? "1x1 Chunk (16x16)" : (radius == 1 ? "3x3 Chunks (48x48)" : "5x5 Chunks (80x80)");
            lines.add(Component.literal("§7Mining Area:           §b" + radiusStr));
            lines.add(Component.literal(String.format("§7Total Blocks Mined:    §f%,d", this.menu.getTotalMinedCount())));

            String statusStr = switch (status) {
                case STATUS_COMPLETED -> "§a✔ Completed (Reached Bedrock)";
                case STATUS_PAUSED -> "§e⏸ Paused";
                case STATUS_NO_ENERGY -> "§c⚡ Out of Energy";
                case STATUS_BUFFER_FULL -> "§6📦 Output Buffer Full";
                default -> "§a● Actively Mining";
            };
            lines.add(Component.literal("§7Operational Status:    " + statusStr));

            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }

        // Speed Upgrade Slot Tooltip (x + 151 .. 169, y + 17 .. 35)
        if (mouseX >= x + 151 && mouseX <= x + 169 && mouseY >= y + 17 && mouseY <= y + 35) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§6⚡ Overclock & Speed Socket"));
            lines.add(Component.literal("§7Accepts: Gears (Copper..Diamond) or §6Blaze Overclock Core"));
            lines.add(Component.literal("§8Scales speed up to 20 blocks/second."));
            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }

        // Range Upgrade Slot Tooltip (x + 151 .. 169, y + 35 .. 53)
        if (mouseX >= x + 151 && mouseX <= x + 169 && mouseY >= y + 35 && mouseY <= y + 53) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§b📡 Range Expansion Socket"));
            lines.add(Component.literal("§7Accepts: §bTier 1 Core (3x3 Chunks) §7or §dTier 2 Core (5x5 Chunks)"));
            lines.add(Component.literal("§8Expands scanning and laser perimeter area."));
            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }

        // Utility / Extraction Socket Tooltip (x + 151 .. 169, y + 53 .. 71)
        if (mouseX >= x + 151 && mouseX <= x + 169 && mouseY >= y + 53 && mouseY <= y + 71) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§a🔮 Utility & Extraction Socket"));
            lines.add(Component.literal("§7Accepts: §6Fortune Core§7, §aSilk Touch Core§7,"));
            lines.add(Component.literal("§5Interdimensional Card§7, §bChunk Loader Module§7,"));
            lines.add(Component.literal("§eor §aWireless Storage Crystal"));
            lines.add(Component.literal("§8Provides drop multipliers, auto chunk-loading, or"));
            lines.add(Component.literal("§8quantum cross-dimensional wireless linking."));
            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }

        // Network Status Tooltip (x + 158 .. 170, y + 4 .. 16)
        if (mouseX >= x + 158 && mouseX <= x + 170 && mouseY >= y + 4 && mouseY <= y + 16) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§b🌐 Digital Storage Link"));
            int netStatus = this.menu.getNetworkStatus();
            if (netStatus == 2) {
                lines.add(Component.literal("§b● Linked: Quantum Interdimensional Link Active"));
                lines.add(Component.literal("§7Teleporting mined ores directly across dimensions!"));
                lines.add(Component.literal("§7Draws operating FE wirelessly from Base Grid."));
            } else if (netStatus == 1) {
                lines.add(Component.literal("§a● Online: Connected to Base Storage Network"));
                lines.add(Component.literal("§7Mined ores directly deposit into connected storage."));
            } else if (netStatus == 3) {
                lines.add(Component.literal("§c● Blocked: Missing Interdimensional Card"));
                lines.add(Component.literal("§eInstall an Interdimensional Card in either the"));
                lines.add(Component.literal("§eBase Storage Controller or this Quarry's Utility Socket!"));
            } else if (netStatus == 4) {
                lines.add(Component.literal("§c● Offline: Base Network Unreachable"));
                lines.add(Component.literal("§7Base chunk may be unloaded or Controller out of power."));
                lines.add(Component.literal("§eInstall an Interdimensional Card or Chunk Loader in Controller!"));
            } else {
                lines.add(Component.literal("§7○ Unbound: No Remote Network Linked"));
                lines.add(Component.literal("§7Mined items store in internal buffer or adjacent chests."));
                lines.add(Component.literal("§8Sneak + Right-Click Wrench on Base Controller, then Quarry to link."));
            }
            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }

        // Empty Extraction Buffer Slots (0..8)
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 9) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§a⛏️ Mined Resource Buffer"),
                    Component.literal("§7Extracted ores and quarried blocks appear here."),
                    Component.literal("§8(Automatically piped or teleported to linked storage)")
            ), mouseX, mouseY);
        }
    }
}
