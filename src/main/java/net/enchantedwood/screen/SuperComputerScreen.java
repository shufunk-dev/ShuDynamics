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
        // Status Indicators Background Pill (x + 100, y + 4, w: 70, h: 12)
        context.fill(x + 100, y + 4, x + 170, y + 16, 0xAA111822);
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

        // Enchanted Furnace / Smelting Link status icon (♨)
        if (this.handler.isFurnaceOnline()) {
            context.drawTextWithShadow(this.textRenderer, Text.literal("♨").formatted(net.minecraft.util.Formatting.GOLD), 104, 6, 0xFFAA00);
        } else {
            context.drawTextWithShadow(this.textRenderer, Text.literal("♨").formatted(net.minecraft.util.Formatting.DARK_GRAY), 104, 6, 0x555555);
        }

        // Hydraulic Press Link status icon (◆)
        if (this.handler.isPressOnline()) {
            context.drawTextWithShadow(this.textRenderer, Text.literal("◆").formatted(net.minecraft.util.Formatting.AQUA), 118, 6, 0x55FFFF);
        } else {
            context.drawTextWithShadow(this.textRenderer, Text.literal("◆").formatted(net.minecraft.util.Formatting.DARK_GRAY), 118, 6, 0x555555);
        }

        // Circuit Fabricator Link status icon (✦)
        if (this.handler.isFabricatorOnline()) {
            context.drawTextWithShadow(this.textRenderer, Text.literal("✦").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE), 132, 6, 0xFF55FF);
        } else {
            context.drawTextWithShadow(this.textRenderer, Text.literal("✦").formatted(net.minecraft.util.Formatting.DARK_GRAY), 132, 6, 0x555555);
        }

        // Casting Automation Link status icon (⚡)
        if (this.handler.isCasterOnline()) {
            context.drawTextWithShadow(this.textRenderer, Text.literal("⚡").formatted(net.minecraft.util.Formatting.YELLOW), 146, 6, 0xFFFF55);
        } else {
            context.drawTextWithShadow(this.textRenderer, Text.literal("⚡").formatted(net.minecraft.util.Formatting.DARK_GRAY), 146, 6, 0x555555);
        }

        // Digital Storage Network status icon (●)
        if (this.handler.isNetworkOnline()) {
            context.drawTextWithShadow(this.textRenderer, Text.literal("●").formatted(net.minecraft.util.Formatting.GREEN), 160, 6, 0x55FF55);
        } else {
            context.drawTextWithShadow(this.textRenderer, Text.literal("●").formatted(net.minecraft.util.Formatting.RED), 160, 6, 0xFF5555);
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
            lines.add(Text.literal("§7Shift-Click: Craft all possible"));
            lines.add(Text.literal("§8Uses materials from Digital Storage, Tanks & Inventory."));

            lines.add(Text.literal(""));
            lines.add(Text.literal("§eConnected Machines:"));
            lines.add(Text.literal(this.handler.isFurnaceOnline() ? " §a✔ Smelter / Furnace: §2ONLINE" : " §8✖ Smelter / Furnace: §cOFFLINE"));
            lines.add(Text.literal(this.handler.isPressOnline() ? " §a✔ Hydraulic Press: §2ONLINE" : " §8✖ Hydraulic Press: §cOFFLINE"));
            lines.add(Text.literal(this.handler.isFabricatorOnline() ? " §a✔ Circuit Fabricator: §2ONLINE" : " §8✖ Circuit Fabricator: §cOFFLINE"));
            lines.add(Text.literal(this.handler.isCasterOnline() ? " §a✔ Molten Metal Caster: §2ONLINE" : " §8✖ Molten Metal Caster: §cOFFLINE"));
            lines.add(Text.literal(this.handler.isNetworkOnline() ? " §a✔ Digital Storage: §2ONLINE" : " §c✖ Digital Storage: §cOFFLINE"));

            if (!lastStatus.isEmpty() && System.currentTimeMillis() - lastStatusTime < 14000) {
                lines.add(Text.literal(""));
                lines.add(Text.literal("§7Latest Status: " + lastStatus));
            }
            context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        }

        // Furnace Link Status Tooltip (x + 101 .. 114, y + 4 .. 16)
        if (mouseX >= x + 101 && mouseX <= x + 114 && mouseY >= y + 4 && mouseY <= y + 16) {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("§6♨ Automated Smelting Link"));
            if (this.handler.isFurnaceOnline()) {
                lines.add(Text.literal("§a● Online: Connected to Enchanted Furnace / Smelters"));
                lines.add(Text.literal("§7Smelts sand to glass, cobblestone to stone/smooth stone,"));
                lines.add(Text.literal("§7charcoal from logs, and ores on-demand."));
            } else {
                lines.add(Text.literal("§c● Offline: No Furnace detected"));
                lines.add(Text.literal("§8Place an Enchanted Furnace within 64 blocks or link with Wrench."));
            }
            context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        }

        // Press Link Status Tooltip (x + 115 .. 128, y + 4 .. 16)
        if (mouseX >= x + 115 && mouseX <= x + 128 && mouseY >= y + 4 && mouseY <= y + 16) {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("§b◆ Hydraulic Press Link"));
            if (this.handler.isPressOnline()) {
                lines.add(Text.literal("§a● Online: Connected to Hydraulic Press"));
                lines.add(Text.literal("§7Stamps silicon into wafers and ingots/blocks into plates on-demand."));
            } else {
                lines.add(Text.literal("§c● Offline: No Hydraulic Press detected"));
                lines.add(Text.literal("§8Place a Hydraulic Press within 64 blocks or link with Wrench."));
            }
            context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        }

        // Fabricator Link Status Tooltip (x + 129 .. 142, y + 4 .. 16)
        if (mouseX >= x + 129 && mouseX <= x + 142 && mouseY >= y + 4 && mouseY <= y + 16) {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("§d✦ Circuit Fabricator Link"));
            if (this.handler.isFabricatorOnline()) {
                lines.add(Text.literal("§a● Online: Connected to Precision Circuit Fabricator"));
                lines.add(Text.literal("§7Fabricates Basic, Advanced, Quantum, and Metallurgy chips on-demand."));
            } else {
                lines.add(Text.literal("§c● Offline: No Circuit Fabricator detected"));
                lines.add(Text.literal("§8Place a Circuit Fabricator within 64 blocks or link with Wrench."));
            }
            context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        }

        // Caster Link Status Tooltip (x + 143 .. 156, y + 4 .. 16)
        if (mouseX >= x + 143 && mouseX <= x + 156 && mouseY >= y + 4 && mouseY <= y + 16) {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("§e⚡ Molten Metal Caster Link"));
            if (this.handler.isCasterOnline()) {
                lines.add(Text.literal("§a● Online: Connected to Casting Port & Tanks"));
                lines.add(Text.literal("§7Draws from molten metal tanks for on-demand casting."));
                lines.add(Text.literal("§8(Keeps metals liquified to save digital storage crystals)"));
            } else {
                lines.add(Text.literal("§c● Offline: No Casting Port detected"));
                lines.add(Text.literal("§8Place a Casting Port within 64 blocks to enable metal casting."));
            }
            context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        }

        // Network Status Tooltip (x + 157 .. 170, y + 4 .. 16)
        if (mouseX >= x + 157 && mouseX <= x + 170 && mouseY >= y + 4 && mouseY <= y + 16) {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("§b● Digital Storage Link"));
            if (this.handler.isNetworkOnline()) {
                lines.add(Text.literal("§a● Online: Connected to Digital Storage Network"));
                lines.add(Text.literal("§7Automatically pulls ingredients from Drive Bay crystals."));
            } else {
                lines.add(Text.literal("§c● Offline: No active Storage Controller in range"));
                lines.add(Text.literal("§8Place within 64 blocks of a Storage Controller or link with Wrench."));
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
