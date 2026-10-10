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
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractLabels(context, mouseX, mouseY);

        // Telemetry LCD Display (x: 21..73, y: 64..75)
        String depthStr = "Y:" + this.menu.getScanY();
        int radius = this.menu.getRangeChunkRadius();
        String radiusStr = (radius == 0) ? "1x1" : (radius == 1 ? "3x3" : "5x5");
        String tele = depthStr + " " + radiusStr;
        context.text(this.font, Component.literal(tele).withStyle(net.minecraft.ChatFormatting.AQUA), 23, 66, 0x55FFFF, false);

        // Digital Storage Network status icon
        int netStatus = this.menu.getNetworkStatus();
        if (netStatus == 2) {
            context.text(this.font, Component.literal("●").withStyle(net.minecraft.ChatFormatting.AQUA), 162, 6, 0x55FFFF, false);
        } else if (netStatus == 1) {
            context.text(this.font, Component.literal("●").withStyle(net.minecraft.ChatFormatting.GREEN), 162, 6, 0x55FF55, false);
        } else if (netStatus == 3 || netStatus == 4) {
            context.text(this.font, Component.literal("●").withStyle(net.minecraft.ChatFormatting.RED), 162, 6, 0xFF5555, false);
        } else {
            context.text(this.font, Component.literal("○").withStyle(net.minecraft.ChatFormatting.GRAY), 162, 6, 0xAAAAAA, false);
        }
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
