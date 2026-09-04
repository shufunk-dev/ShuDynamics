package net.enchantedwood.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.enchantedwood.EnchantedWoodMod;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class EnchantedDriveBayScreen extends HandledScreen<EnchantedDriveBayScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/enchanted_drive_bay_gui.png");

    private static final int[] SLOT_COLS = {36, 76, 116};
    private static final int[] SLOT_ROWS = {24, 48};

    public EnchantedDriveBayScreen(EnchantedDriveBayScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        this.titleY = 6;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // Draw multi-color dynamic LED status indicators
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 3; c++) {
                int slotIndex = c + r * 3;
                int state = this.handler.getDriveState(slotIndex);
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
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);

        int capacity = this.handler.getTotalCapacity();
        String capText = capacity > 0 ? String.format("%,d Items", capacity) : "No Storage";
        int color = capacity > 0 ? 0x55FF55 : 0xAAAAAA;

        // Draw Capacity string on top right of the Drive Chamber
        context.drawText(this.textRenderer, Text.literal("💾 " + capText), 80, 6, color, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Hover Tooltip for LED / Slot Sockets
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 3; c++) {
                int slotIndex = c + r * 3;
                int sx = x + SLOT_COLS[c];
                int sy = y + SLOT_ROWS[r];

                if (mouseX >= sx - 10 && mouseX < sx && mouseY >= sy && mouseY <= sy + 16) {
                    int state = this.handler.getDriveState(slotIndex);
                    boolean canTake = this.handler.canTakeDrive(slotIndex);
                    List<Text> tooltipList = new ArrayList<>();

                    if (state == 0) {
                        tooltipList.add(Text.literal("§a● Drive Bay " + (slotIndex + 1) + ": READY"));
                        tooltipList.add(Text.literal("§aStatus: Empty (0% Used)"));
                        tooltipList.add(Text.literal("§a🔓 UNLOCKED: Safe to remove or upgrade"));
                    } else if (state == 1) {
                        tooltipList.add(Text.literal("§e● Drive Bay " + (slotIndex + 1) + ": IN USE"));
                        tooltipList.add(Text.literal("§eStatus: Active (< 80% Full)"));
                        if (!canTake) {
                            tooltipList.add(Text.literal("§c🔒 LOCKED: Drive contains stored data"));
                            tooltipList.add(Text.literal("§7Empty items before uninstalling!"));
                        } else {
                            tooltipList.add(Text.literal("§a🔓 UNLOCKED: Remaining drives hold data"));
                        }
                    } else if (state == 2) {
                        tooltipList.add(Text.literal("§d● Drive Bay " + (slotIndex + 1) + ": 80%+ WARNING"));
                        tooltipList.add(Text.literal("§dStatus: Nearly Full (≥ 80% Full)"));
                        if (!canTake) {
                            tooltipList.add(Text.literal("§c🔒 LOCKED: Drive contains stored data"));
                            tooltipList.add(Text.literal("§7Empty items before uninstalling!"));
                        }
                    } else if (state == 3) {
                        tooltipList.add(Text.literal("§c● Drive Bay " + (slotIndex + 1) + ": FULL"));
                        tooltipList.add(Text.literal("§cStatus: 100% Full"));
                        if (!canTake) {
                            tooltipList.add(Text.literal("§c🔒 LOCKED: Drive contains stored data"));
                            tooltipList.add(Text.literal("§7Empty items before uninstalling!"));
                        }
                    } else {
                        tooltipList.add(Text.literal("§7○ Drive Bay " + (slotIndex + 1) + ": EMPTY SOCKET"));
                        tooltipList.add(Text.literal("§8Insert 1k, 4k, 16k, or 64k Storage Crystal"));
                    }

                    context.drawTooltip(this.textRenderer, tooltipList, mouseX, mouseY);
                }
            }
        }

        // Empty Drive Socket Tooltip
        if (this.focusedSlot != null && !this.focusedSlot.hasStack() && this.focusedSlot.id < 6) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("§b💾 Storage Crystal Socket (Bay " + (this.focusedSlot.id + 1) + "/6)"),
                    Text.literal("§7Insert Digital Storage Crystals:"),
                    Text.literal("§f• 1K, 4K, 16K, or 64K Storage Crystal"),
                    Text.literal("§8Provides mass quantum item storage to connected network.")
            ), mouseX, mouseY);
        }
    }
}
