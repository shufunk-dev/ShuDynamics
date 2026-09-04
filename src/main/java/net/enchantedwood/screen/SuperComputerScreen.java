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
public class SuperComputerScreen extends HandledScreen<SuperComputerScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.of(EnchantedWoodMod.MOD_ID, "textures/gui/container/super_computer_gui.png");

    private net.minecraft.client.gui.widget.ButtonWidget craftButton;

    private static String lastStatus = "";
    private static long lastStatusTime = 0;

    public static void setLastStatus(String msg) {
        lastStatus = msg;
        lastStatusTime = System.currentTimeMillis();
    }

    public SuperComputerScreen(SuperComputerScreenHandler handler, PlayerInventory inventory, Text title) {
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

        this.craftButton = net.minecraft.client.gui.widget.ButtonWidget.builder(Text.literal("⚡ Craft"), button -> {
            if (this.client != null && this.client.interactionManager != null) {
                net.minecraft.client.util.Window window = this.client.getWindow();
                boolean shift = net.minecraft.client.util.InputUtil.isKeyPressed(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
                        || net.minecraft.client.util.InputUtil.isKeyPressed(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
                int buttonId = shift ? 1 : 0;
                this.client.interactionManager.clickButton(this.handler.syncId, buttonId);
            }
        }).dimensions(x + 88, y + 50, 32, 18).build();

        this.addDrawableChild(this.craftButton);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // Draw Energy Vertical Gauge (x + 8, y + 17, w: 8, h: 31)
        int energy = this.handler.getEnergy();
        int maxEnergy = this.handler.getMaxEnergy();
        if (maxEnergy > 0 && energy > 0) {
            int scaledH = Math.min(31, (int) ((long) energy * 31 / maxEnergy));
            int energyY = (y + 17) + (31 - scaledH);
            context.fill(x + 8, energyY, x + 16, y + 17 + 31, 0xFFFF2222);
        }

        // Draw Computing / Crafting Progress Bar (x + 91, y + 39, w: 22, h: 4)
        int progress = this.handler.getCraftProgress();
        int maxProgress = this.handler.getMaxCraftProgress();
        if (maxProgress > 0 && progress > 0) {
            int progressW = Math.min(22, (progress * 22) / maxProgress);
            context.fill(x + 91, y + 39, x + 91 + progressW, y + 43, 0xFF00FFCC);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(8.0f, 6.0f);
        matrices.scale(0.75f, 0.75f);
        context.drawText(this.textRenderer, this.title, 0, 0, 0x404040, false);
        matrices.popMatrix();

        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0x404040, false);

        // Digital Storage Network status icon
        if (this.handler.isNetworkOnline()) {
            context.drawText(this.textRenderer, Text.literal("●").formatted(net.minecraft.util.Formatting.GREEN), 162, 6, 0x55FF55, false);
        } else {
            context.drawText(this.textRenderer, Text.literal("●").formatted(net.minecraft.util.Formatting.RED), 162, 6, 0xFF5555, false);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Render On-Screen Status Notification Banner directly visible while GUI is open
        if (!lastStatus.isEmpty() && System.currentTimeMillis() - lastStatusTime < 14000) {
            Text statusText = Text.literal(lastStatus);
            int textW = this.textRenderer.getWidth(statusText);
            int bannerX = Math.max(4, (this.width - textW) / 2);
            int bannerY = y - 16;

            // Draw dark background box
            context.fill(bannerX - 6, bannerY - 3, bannerX + textW + 6, bannerY + 11, 0xDD111111);
            context.fill(bannerX - 5, bannerY - 2, bannerX + textW + 5, bannerY + 10, 0xEE222222);
            context.drawText(this.textRenderer, statusText, bannerX, bannerY, 0xFFFFFF, true);
        }

        this.drawMouseoverTooltip(context, mouseX, mouseY);

        // Energy Bar Hover Tooltip (x + 8 .. 16, y + 17 .. 49)
        if (mouseX >= x + 8 && mouseX <= x + 16 && mouseY >= y + 17 && mouseY <= y + 49) {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("§e⚡ Super Computer Energy"));
            lines.add(Text.literal(String.format("§f%,d / %,d FE", this.handler.getEnergy(), this.handler.getMaxEnergy())));
            lines.add(Text.literal("§7Draws from Digital Network Controller or energy grid."));
            context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        }

        // Upgrade Slot Tooltip (x + 7 .. 25, y + 52 .. 70)
        if (mouseX >= x + 7 && mouseX <= x + 25 && mouseY >= y + 52 && mouseY <= y + 70) {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("§d🔥 Overclock Upgrade Socket"));
            lines.add(Text.literal("§7Accepts: Blaze Overclock Core"));
            lines.add(Text.literal("§8Boosts computing and synthesis speed!"));
            context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        }

        // Craft Button Hover Tooltip (x + 88 .. 120, y + 50 .. 68)
        if (mouseX >= x + 88 && mouseX <= x + 120 && mouseY >= y + 50 && mouseY <= y + 68) {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("§a⚡ Execute Craft"));
            lines.add(Text.literal("§7Click: Craft 1 batch"));
            lines.add(Text.literal("§7Shift-Click: Craft all possible with what you have"));
            lines.add(Text.literal("§8Uses materials from Digital Storage & Inventory."));
            if (!lastStatus.isEmpty() && System.currentTimeMillis() - lastStatusTime < 14000) {
                lines.add(Text.literal(""));
                lines.add(Text.literal("§7Latest Status: " + lastStatus));
            }
            context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        }

        // Network Status Tooltip (x + 158 .. 170, y + 4 .. 16)
        if (mouseX >= x + 158 && mouseX <= x + 170 && mouseY >= y + 4 && mouseY <= y + 16) {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("§b🌐 Digital Storage Link"));
            if (this.handler.isNetworkOnline()) {
                lines.add(Text.literal("§a● Online: Connected to Digital Storage Network"));
                lines.add(Text.literal("§7Automatically pulls ingredients from Drive Bay crystals."));
            } else {
                lines.add(Text.literal("§c● Offline: No active Storage Controller in range"));
                lines.add(Text.literal("§8Place within 16 blocks of an active Storage Controller."));
            }
            context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.focusedSlot != null && !this.focusedSlot.hasStack() && this.focusedSlot.id < 15) {
            if (this.focusedSlot.id < 9) {
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§e🧩 Auto-Crafting Recipe Grid (Slot " + (this.focusedSlot.id + 1) + "/9)"),
                        Text.literal("§7Place recipe pattern items here to encode an automated craft.")
                ), mouseX, mouseY);
            } else if (this.focusedSlot.id == 9) {
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§d🔥 Overclock Upgrade Socket"),
                        Text.literal("§7Accepts: §aBlaze Overclock Core"),
                        Text.literal("§8Boosts computation & rapid synthesis speed.")
                ), mouseX, mouseY);
            } else if (this.focusedSlot.id >= 10 && this.focusedSlot.id <= 13) {
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§a✨ Synthesized Output Buffer"),
                        Text.literal("§7Synthesized batch items appear here.")
                ), mouseX, mouseY);
            } else if (this.focusedSlot.id == 14) {
                context.drawTooltip(this.textRenderer, List.of(
                        Text.literal("§6🔍 Target Recipe Preview"),
                        Text.literal("§7Shows the result of the configured 3x3 pattern.")
                ), mouseX, mouseY);
            }
        }
    }
}
