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

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class EnchantedDriveBayScreen extends AbstractContainerScreen<EnchantedDriveBayScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/enchanted_drive_bay_gui.png");

    private static final int[] SLOT_COLS = {36, 76, 116};
    private static final int[] SLOT_ROWS = {24, 48};

    public EnchantedDriveBayScreen(EnchantedDriveBayScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // Draw multi-color dynamic LED status indicators
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 3; c++) {
                int slotIndex = c + r * 3;
                int state = this.menu.getDriveState(slotIndex);
                int ledX = x + SLOT_COLS[c] - 9;
                int ledY = y + SLOT_ROWS[r] + 5;

                if (state == 0) {
                    // Green: Empty drive installed
                    context.fill(ledX, ledY, ledX + 4, ledY + 6, 0xFF00FF55);
                } else if (state == 1) {
                    // Yellow: In use (1+ items)
                    context.fill(ledX, ledY, ledX + 4, ledY + 6, 0xFFFFFF00);
                } else if (state == 2) {
                    // Purple: 80%+ full
                    context.fill(ledX, ledY, ledX + 4, ledY + 6, 0xFFD020FF);
                } else if (state == 3) {
                    // Red: 100% full
                    context.fill(ledX, ledY, ledX + 4, ledY + 6, 0xFFFF2222);
                } else {
                    // Dark / unlit socket
                    context.fill(ledX, ledY, ledX + 4, ledY + 6, 0xFF2A2A2A);
                }
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractLabels(context, mouseX, mouseY);

        int capacity = this.menu.getTotalCapacity();
        String capText = capacity > 0 ? String.format("%,d Items", capacity) : "No Storage";
        int color = capacity > 0 ? 0x55FF55 : 0xAAAAAA;

        // Draw Capacity string on top right of the Drive Chamber
        context.text(this.font, Component.literal("💾 " + capText), 80, 6, color, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);


        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Hover Tooltip for LED / Slot Sockets
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 3; c++) {
                int slotIndex = c + r * 3;
                int sx = x + SLOT_COLS[c];
                int sy = y + SLOT_ROWS[r];

                if (mouseX >= sx - 10 && mouseX < sx && mouseY >= sy && mouseY <= sy + 16) {
                    int state = this.menu.getDriveState(slotIndex);
                    boolean canTake = this.menu.canTakeDrive(slotIndex);
                    List<Component> tooltipList = new ArrayList<>();

                    if (state == 0) {
                        tooltipList.add(Component.literal("§a● Drive Bay " + (slotIndex + 1) + ": READY"));
                        tooltipList.add(Component.literal("§aStatus: Empty (0% Used)"));
                        tooltipList.add(Component.literal("§a🔓 UNLOCKED: Safe to remove or upgrade"));
                    } else if (state == 1) {
                        tooltipList.add(Component.literal("§e● Drive Bay " + (slotIndex + 1) + ": IN USE"));
                        tooltipList.add(Component.literal("§eStatus: Active (< 80% Full)"));
                        if (!canTake) {
                            tooltipList.add(Component.literal("§c🔒 LOCKED: Drive contains stored data"));
                            tooltipList.add(Component.literal("§7Empty items before uninstalling!"));
                        } else {
                            tooltipList.add(Component.literal("§a🔓 UNLOCKED: Remaining drives hold data"));
                        }
                    } else if (state == 2) {
                        tooltipList.add(Component.literal("§d● Drive Bay " + (slotIndex + 1) + ": 80%+ WARNING"));
                        tooltipList.add(Component.literal("§dStatus: Nearly Full (≥ 80% Full)"));
                        if (!canTake) {
                            tooltipList.add(Component.literal("§c🔒 LOCKED: Drive contains stored data"));
                            tooltipList.add(Component.literal("§7Empty items before uninstalling!"));
                        }
                    } else if (state == 3) {
                        tooltipList.add(Component.literal("§c● Drive Bay " + (slotIndex + 1) + ": FULL"));
                        tooltipList.add(Component.literal("§cStatus: 100% Full"));
                        if (!canTake) {
                            tooltipList.add(Component.literal("§c🔒 LOCKED: Drive contains stored data"));
                            tooltipList.add(Component.literal("§7Empty items before uninstalling!"));
                        }
                    } else {
                        tooltipList.add(Component.literal("§7○ Drive Bay " + (slotIndex + 1) + ": EMPTY SOCKET"));
                        tooltipList.add(Component.literal("§8Insert 1k, 4k, 16k, or 64k Storage Crystal"));
                    }

                    context.setComponentTooltipForNextFrame(this.font, tooltipList, mouseX, mouseY);
                }
            }
        }

        // Empty Drive Socket Tooltip
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 6) {
            context.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.literal("§b💾 Storage Crystal Socket (Bay " + (this.hoveredSlot.index + 1) + "/6)"),
                    Component.literal("§7Insert Digital Storage Crystals:"),
                    Component.literal("§f• 1K, 4K, 16K, or 64K Storage Crystal"),
                    Component.literal("§8Provides mass quantum item storage to connected network.")
            ), mouseX, mouseY);
        }
    }
}
