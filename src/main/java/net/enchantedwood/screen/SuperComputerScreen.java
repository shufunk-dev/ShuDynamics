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
public class SuperComputerScreen extends AbstractContainerScreen<SuperComputerScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/super_computer_gui.png");

    private net.minecraft.client.gui.components.Button craftButton;

    private static String lastStatus = "";
    private static long lastStatusTime = 0;

    public static void setLastStatus(String msg) {
        lastStatus = msg;
        lastStatusTime = System.currentTimeMillis();
    }

    public SuperComputerScreen(SuperComputerScreenHandler handler, Inventory inventory, Component title) {
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

        this.craftButton = net.minecraft.client.gui.components.Button.builder(Component.literal("⚡ Craft"), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                com.mojang.blaze3d.platform.Window window = this.minecraft.getWindow();
                boolean shift = com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT) || com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_RSHIFT);
                int buttonId = shift ? 1 : 0;
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
            }
        }).bounds(x + 86, y + 50, 36, 18).build();

        this.addRenderableWidget(this.craftButton);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        // Draw Energy Vertical Gauge (x + 8, y + 17, w: 8, h: 31)
        int energy = this.menu.getEnergy();
        int maxEnergy = this.menu.getMaxEnergy();
        if (maxEnergy > 0 && energy > 0) {
            int scaledH = Math.min(31, (int) ((long) energy * 31 / maxEnergy));
            int energyY = (y + 17) + (31 - scaledH);
            context.fill(x + 8, energyY, x + 16, y + 17 + 31, 0xFFFF2222);
        }

        // Draw Computing / Crafting Progress Bar inside the beveled groove (x + 99, y + 39, w: 15, h: 4)
        int progress = this.menu.getCraftProgress();
        int maxProgress = this.menu.getMaxCraftProgress();
        if (maxProgress > 0 && progress > 0) {
            int progressW = Math.max(1, Math.min(15, (progress * 15) / maxProgress));
            context.fill(x + 99, y + 39, x + 99 + progressW, y + 43, 0xFF00FFCC);
        }

        // Draw Machine Links Subsystem Panel with rich pixel-art status icons
        this.drawLinkSystem(context, x, y, mouseX, mouseY);
    }

    private void drawLinkSystem(GuiGraphicsExtractor context, int x, int y, int mouseX, int mouseY) {
        int panelX = x + 85;
        int panelY = y + 4;
        int panelW = 87;
        int panelH = 14;

        // Beveled Sci-Fi Frame
        context.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, 0xFF141923);
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF00A0E14);

        long time = System.currentTimeMillis();

        for (int i = 0; i < 7; i++) {
            int bx = panelX + 2 + i * 12;
            int by = panelY + 1;
            int bw = 11;
            int bh = 12;

            boolean online = switch (i) {
                case 0 -> this.menu.isFurnaceOnline();
                case 1 -> this.menu.isPressOnline();
                case 2 -> this.menu.isFabricatorOnline();
                case 3 -> this.menu.isCasterOnline();
                case 4 -> this.menu.isWaterPumpOnline();
                case 5 -> this.menu.isLavaSourceOnline();
                default -> this.menu.isNetworkOnline();
            };

            boolean hovered = (mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh);

            // Bay slot background & border
            int slotBg = online ? 0xDD0D1520 : 0xAA0B0D12;
            context.fill(bx, by, bx + bw, by + bh, slotBg);
            int slotBorder = hovered ? 0xFF55FFFF : (online ? 0xFF2A3D55 : 0xFF1C222C);
            context.fill(bx, by, bx + bw, by + 1, slotBorder);
            context.fill(bx, by + bh - 1, bx + bw, by + bh, slotBorder);
            context.fill(bx, by, bx + 1, by + bh, slotBorder);
            context.fill(bx + bw - 1, by, bx + bw, by + bh, slotBorder);

            // Render specific icon pixel art inside
            switch (i) {
                case 0 -> { // 1. Smelter / Furnace
                    int stoneColor = online ? 0xFF525866 : 0xFF363B45;
                    context.fill(bx + 2, by + 1, bx + 9, by + 2, stoneColor);
                    context.fill(bx + 1, by + 2, bx + 3, by + 8, stoneColor);
                    context.fill(bx + 7, by + 2, bx + 9, by + 8, stoneColor);
                    context.fill(bx + 1, by + 8, bx + 9, by + 9, stoneColor);

                    if (online) {
                        long fireTick = (time / 140) % 3;
                        context.fill(bx + 3, by + 3, bx + 7, by + 8, 0xFFFF4400);
                        context.fill(bx + 3, by + (int)(4 + fireTick % 2), bx + 7, by + 8, 0xFFFF9900);
                        context.fill(bx + 4, by + 5, bx + 6, by + 7, fireTick == 1 ? 0xFFFFFF99 : 0xFFFFDD33);
                    } else {
                        context.fill(bx + 3, by + 3, bx + 7, by + 8, 0xFF16181E);
                        context.fill(bx + 4, by + 5, bx + 6, by + 7, 0xFF252830);
                    }
                }
                case 1 -> { // 2. Hydraulic Press
                    int ramColor = online ? 0xFF2980B9 : 0xFF2C3E50;
                    context.fill(bx + 1, by + 1, bx + 9, by + 3, ramColor);
                    context.fill(bx + 4, by + 3, bx + 6, by + 5, online ? 0xFFBDC3C7 : 0xFF4C5B6B);
                    context.fill(bx + 2, by + 5, bx + 8, by + 7, online ? 0xFF3498DB : 0xFF3D4C5C);
                    if (online) {
                        context.fill(bx + 3, by + 7, bx + 7, by + 8, 0xFF00FFEE);
                    }
                    context.fill(bx + 1, by + 8, bx + 9, by + 10, online ? 0xFF1C2833 : 0xFF17202A);
                }
                case 2 -> { // 3. Circuit Fabricator
                    context.fill(bx + 2, by + 2, bx + 8, by + 8, online ? 0xFF1E1430 : 0xFF1A1C22);
                    int pinColor = online ? 0xFFFFCC00 : 0xFF4D5360;
                    context.fill(bx, by + 3, bx + 2, by + 4, pinColor);
                    context.fill(bx, by + 6, bx + 2, by + 7, pinColor);
                    context.fill(bx + 8, by + 3, bx + 10, by + 4, pinColor);
                    context.fill(bx + 8, by + 6, bx + 10, by + 7, pinColor);
                    if (online) {
                        long pulse = (time / 200) % 2;
                        context.fill(bx + 3, by + 3, bx + 7, by + 7, pulse == 0 ? 0xFFD033FF : 0xFF9922EE);
                        context.fill(bx + 4, by + 4, bx + 6, by + 6, 0xFFFF88FF);
                    } else {
                        context.fill(bx + 3, by + 3, bx + 7, by + 7, 0xFF2D303A);
                    }
                }
                case 3 -> { // 4. Molten Metal Caster
                    context.fill(bx + 1, by + 1, bx + 5, by + 4, online ? 0xFF4D4035 : 0xFF2A2D34);
                    if (online) {
                        context.fill(bx + 2, by + 2, bx + 4, by + 3, 0xFFFFEE55);
                        context.fill(bx + 4, by + 3, bx + 6, by + 7, 0xFFFF8800);
                        context.fill(bx + 5, by + 4, bx + 6, by + 6, 0xFFFFDD33);
                        context.fill(bx + 2, by + 7, bx + 9, by + 9, 0xFF5D4037);
                        context.fill(bx + 3, by + 7, bx + 7, by + 8, 0xFFFF5500);
                    } else {
                        context.fill(bx + 4, by + 3, bx + 5, by + 6, 0xFF1E2128);
                        context.fill(bx + 2, by + 7, bx + 9, by + 9, 0xFF252830);
                    }
                }
                case 4 -> { // 5. Electric Water Pump
                    context.fill(bx + 3, by + 1, bx + 8, by + 3, online ? 0xFF3A6B88 : 0xFF25333D);
                    context.fill(bx + 1, by + 3, bx + 9, by + 9, online ? 0xFF1B3B52 : 0xFF141F28);
                    context.fill(bx + 2, by + 5, bx + 8, by + 8, online ? 0xFF0077CC : 0xFF10202E);
                    if (online) {
                        long wave = (time / 200) % 3;
                        context.fill(bx + 3, by + 4, bx + 8, by + 5, 0xFF33CCFF);
                        context.fill(bx + (int)(3 + wave), by + 5, bx + (int)(5 + wave), by + 7, 0xFFB0E2FF);
                    }
                    context.fill(bx + 3, by + 9, bx + 7, by + 11, online ? 0xFF294E66 : 0xFF172026);
                }
                case 5 -> { // 6. Thermal Lava Pump / Magma Crucible
                    context.fill(bx + 2, by + 2, bx + 8, by + 9, online ? 0xFF4A1A0B : 0xFF241814);
                    context.fill(bx + 3, by + 4, bx + 7, by + 8, online ? 0xFFFF4500 : 0xFF281510);
                    if (online) {
                        long glow = (time / 180) % 2;
                        context.fill(bx + 3, by + 5, bx + 7, by + 7, glow == 0 ? 0xFFFF9900 : 0xFFFFCC00);
                        context.fill(bx + 4, by + 2, bx + 6, by + 4, 0xFFFF3300);
                    } else {
                        context.fill(bx + 4, by + 5, bx + 6, by + 7, 0xFF1A1210);
                    }
                    context.fill(bx + 2, by + 9, bx + 4, by + 11, online ? 0xFF35201A : 0xFF181514);
                    context.fill(bx + 6, by + 9, bx + 8, by + 11, online ? 0xFF35201A : 0xFF181514);
                }
                default -> { // 7. Digital Storage Network
                    context.fill(bx + 1, by + 4, bx + 9, by + 9, online ? 0xFF0D233A : 0xFF1C222C);
                    if (online) {
                        context.fill(bx + 4, by + 2, bx + 6, by + 3, 0xFF00FFCC);
                        context.fill(bx + 3, by + 1, bx + 7, by + 2, 0xFF00DD99);
                        long blink = (time / 300) % 2;
                        context.fill(bx + 2, by + 5, bx + 7, by + 6, blink == 0 ? 0xFF00FF66 : 0xFF00AA44);
                        context.fill(bx + 2, by + 7, bx + 7, by + 8, 0xFF00EE88);
                    } else {
                        context.fill(bx + 4, by + 2, bx + 6, by + 3, 0xFF2C3440);
                        context.fill(bx + 2, by + 5, bx + 7, by + 6, 0xFF252C36);
                        context.fill(bx + 2, by + 7, bx + 7, by + 8, 0xFF252C36);
                    }
                }
            }

            // Status indicator pip (bottom right, 2x2 with dark outline)
            int pipX = bx + 8;
            int pipY = by + 9;
            context.fill(pipX - 1, pipY - 1, pipX + 3, pipY + 3, 0xFF000000);
            context.fill(pipX, pipY, pipX + 2, pipY + 2, online ? 0xFF00FF66 : 0xFFFF3333);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.text(this.font, this.title, 8, 6, 0xFF404040, false);
        context.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractTooltip(context, mouseX, mouseY);


        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Render On-Screen Status Notification Banner directly visible while GUI is open
        if (!lastStatus.isEmpty() && System.currentTimeMillis() - lastStatusTime < 14000) {
            Component statusText = Component.literal(lastStatus);
            int textW = this.font.width(statusText);
            int bannerX = Math.max(4, (this.width - textW) / 2);
            int bannerY = y - 16;

            // Draw dark background box
            context.fill(bannerX - 6, bannerY - 3, bannerX + textW + 6, bannerY + 11, 0xDD111111);
            context.fill(bannerX - 5, bannerY - 2, bannerX + textW + 5, bannerY + 10, 0xEE222222);
            context.text(this.font, statusText, bannerX, bannerY, 0xFFFFFF, true);
        }

        // Energy Bar Hover Tooltip (x + 8 .. 16, y + 17 .. 49)
        if (mouseX >= x + 8 && mouseX <= x + 16 && mouseY >= y + 17 && mouseY <= y + 49) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§e⚡ Super Computer Energy"));
            lines.add(Component.literal(String.format("§f%,d / %,d FE", this.menu.getEnergy(), this.menu.getMaxEnergy())));
            lines.add(Component.literal("§7Draws from Digital Network Controller or energy grid."));
            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }

        // Upgrade Slot Tooltip (x + 7 .. 25, y + 52 .. 70)
        if (mouseX >= x + 7 && mouseX <= x + 25 && mouseY >= y + 52 && mouseY <= y + 70) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§d🔥 Overclock Upgrade Socket"));
            lines.add(Component.literal("§7Accepts: Blaze Overclock Core"));
            lines.add(Component.literal("§8Boosts computing and synthesis speed!"));
            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }

        // Craft Button Hover Tooltip (x + 86 .. 122, y + 50 .. 68)
        if (mouseX >= x + 86 && mouseX <= x + 122 && mouseY >= y + 50 && mouseY <= y + 68) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§a⚡ Execute Craft"));
            lines.add(Component.literal("§7Click: Craft 1 batch"));
            lines.add(Component.literal("§7Shift-Click: Craft all possible"));
            lines.add(Component.literal("§8Uses materials from Digital Storage, Tanks & Inventory."));

            lines.add(Component.literal(""));
            lines.add(Component.literal("§eConnected Machines:"));
            lines.add(Component.literal(this.menu.isFurnaceOnline() ? " §a✔ Smelter / Furnace: §2ONLINE" : " §8✖ Smelter / Furnace: §cOFFLINE"));
            lines.add(Component.literal(this.menu.isPressOnline() ? " §a✔ Hydraulic Press: §2ONLINE" : " §8✖ Hydraulic Press: §cOFFLINE"));
            lines.add(Component.literal(this.menu.isFabricatorOnline() ? " §a✔ Circuit Fabricator: §2ONLINE" : " §8✖ Circuit Fabricator: §cOFFLINE"));
            lines.add(Component.literal(this.menu.isCasterOnline() ? " §a✔ Molten Metal Caster: §2ONLINE" : " §8✖ Molten Metal Caster: §cOFFLINE"));
            lines.add(Component.literal(this.menu.isWaterPumpOnline() ? " §a✔ Electric Water Pump: §2ONLINE" : " §8✖ Electric Water Pump: §cOFFLINE"));
            lines.add(Component.literal(this.menu.isLavaSourceOnline() ? " §a✔ Lava Pump / Crucible: §2ONLINE" : " §8✖ Lava Pump / Crucible: §cOFFLINE"));
            lines.add(Component.literal(this.menu.isNetworkOnline() ? " §a✔ Digital Storage: §2ONLINE" : " §c✖ Digital Storage: §cOFFLINE"));

            if (!lastStatus.isEmpty() && System.currentTimeMillis() - lastStatusTime < 14000) {
                lines.add(Component.literal(""));
                lines.add(Component.literal("§7Latest Status: " + lastStatus));
            }
            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }

        // 1. Furnace Link Status Tooltip (x + 87 .. 98, y + 4 .. 18)
        if (mouseX >= x + 87 && mouseX < x + 98 && mouseY >= y + 4 && mouseY <= y + 18) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§6♨ Automated Smelter Link"));
            if (this.menu.isFurnaceOnline()) {
                lines.add(Component.literal("§a● Status: ONLINE §7(Connected)"));
                lines.add(Component.literal("§7Enables automated smelting on-demand for glass,"));
                lines.add(Component.literal("§7smooth stone, charcoal, and processed ores."));
            } else {
                lines.add(Component.literal("§c● Status: OFFLINE §7(Disconnected)"));
                lines.add(Component.literal("§8Place an Enchanted Furnace within 48 blocks or link with Wrench."));
            }
            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }

        // 2. Press Link Status Tooltip (x + 99 .. 110, y + 4 .. 18)
        if (mouseX >= x + 99 && mouseX < x + 110 && mouseY >= y + 4 && mouseY <= y + 18) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§b◆ Hydraulic Press Link"));
            if (this.menu.isPressOnline()) {
                lines.add(Component.literal("§a● Status: ONLINE §7(Connected)"));
                lines.add(Component.literal("§7Enables automatic stamping on-demand for silicon wafers,"));
                lines.add(Component.literal("§7metal plates, reinforced casings, and stamped components."));
            } else {
                lines.add(Component.literal("§c● Status: OFFLINE §7(Disconnected)"));
                lines.add(Component.literal("§8Place a Hydraulic Press within 48 blocks or link with Wrench."));
            }
            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }

        // 3. Fabricator Link Status Tooltip (x + 111 .. 122, y + 4 .. 18)
        if (mouseX >= x + 111 && mouseX < x + 122 && mouseY >= y + 4 && mouseY <= y + 18) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§d✦ Circuit Fabricator Link"));
            if (this.menu.isFabricatorOnline()) {
                lines.add(Component.literal("§a● Status: ONLINE §7(Connected)"));
                lines.add(Component.literal("§7Enables automated precision assembly of Basic, Advanced,"));
                lines.add(Component.literal("§7Quantum, and Metallurgy Integrated Circuits."));
            } else {
                lines.add(Component.literal("§c● Status: OFFLINE §7(Disconnected)"));
                lines.add(Component.literal("§8Place a Circuit Fabricator within 48 blocks or link with Wrench."));
            }
            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }

        // 4. Caster Link Status Tooltip (x + 123 .. 134, y + 4 .. 18)
        if (mouseX >= x + 123 && mouseX < x + 134 && mouseY >= y + 4 && mouseY <= y + 18) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§e⚡ Molten Metal Caster Link"));
            if (this.menu.isCasterOnline()) {
                lines.add(Component.literal("§a● Status: ONLINE §7(Connected)"));
                lines.add(Component.literal("§7Enables direct on-demand casting from molten metal storage,"));
                lines.add(Component.literal("§7drastically saving crystalline storage space."));
            } else {
                lines.add(Component.literal("§c● Status: OFFLINE §7(Disconnected)"));
                lines.add(Component.literal("§8Place a Casting Port within 48 blocks to enable fluid casting."));
            }
            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }

        // 5. Water Pump Link Status Tooltip (x + 135 .. 146, y + 4 .. 18)
        if (mouseX >= x + 135 && mouseX < x + 146 && mouseY >= y + 4 && mouseY <= y + 18) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§9💧 Electric Water Pump Link"));
            if (this.menu.isWaterPumpOnline()) {
                lines.add(Component.literal("§a● Status: ONLINE §7(Connected)"));
                lines.add(Component.literal("§7Enables automated fluid pumping and water bucket auto-crafting."));
                lines.add(Component.literal("§7Draws water on-demand using empty buckets from Digital Storage."));
            } else {
                lines.add(Component.literal("§c● Status: OFFLINE §7(Disconnected)"));
                lines.add(Component.literal("§8Place an Electric Water Pump (or natural water) within 48 blocks."));
            }
            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }

        // 6. Lava Pump / Crucible Link Status Tooltip (x + 147 .. 158, y + 4 .. 18)
        if (mouseX >= x + 147 && mouseX < x + 158 && mouseY >= y + 4 && mouseY <= y + 18) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§6🔥 Thermal Lava Pump / Crucible Link"));
            if (this.menu.isLavaSourceOnline()) {
                lines.add(Component.literal("§a● Status: ONLINE §7(Connected)"));
                lines.add(Component.literal("§7Enables automated lava extraction and lava bucket auto-crafting."));
                lines.add(Component.literal("§7Draws lava on-demand using empty buckets from Digital Storage."));
            } else {
                lines.add(Component.literal("§c● Status: OFFLINE §7(Disconnected)"));
                lines.add(Component.literal("§8Place a Magma Crucible or Lava Pump within 48 blocks."));
            }
            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }

        // 7. Network Status Tooltip (x + 159 .. 170, y + 4 .. 18)
        if (mouseX >= x + 159 && mouseX < x + 170 && mouseY >= y + 4 && mouseY <= y + 18) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§a● Digital Storage Network Link"));
            if (this.menu.isNetworkOnline()) {
                lines.add(Component.literal("§a● Status: ONLINE §7(Connected)"));
                lines.add(Component.literal("§7Direct high-bandwidth bridge to Digital Storage Drives."));
                lines.add(Component.literal("§7Automatically pulls raw crafting materials from network crystals."));
            } else {
                lines.add(Component.literal("§c● Status: OFFLINE §7(Disconnected)"));
                lines.add(Component.literal("§8Place within 48 blocks of a Digital Controller or link with Wrench."));
            }
            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }

        // General Link Panel Summary Tooltip when hovering over panel margins
        if (mouseX >= x + 85 && mouseX <= x + 172 && mouseY >= y + 4 && mouseY <= y + 18
                && !(mouseX >= x + 87 && mouseX < x + 98)
                && !(mouseX >= x + 99 && mouseX < x + 110)
                && !(mouseX >= x + 111 && mouseX < x + 122)
                && !(mouseX >= x + 123 && mouseX < x + 134)
                && !(mouseX >= x + 135 && mouseX < x + 146)
                && !(mouseX >= x + 147 && mouseX < x + 158)
                && !(mouseX >= x + 159 && mouseX < x + 170)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("§b⚡ Automated Machine Links"));
            lines.add(Component.literal("§7Subsystem link status for integrated auto-crafting."));
            lines.add(Component.literal("§8Hover over individual icons for machine diagnostics."));
            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }

        // Empty Machine Slot Tooltips
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 15) {
            if (this.hoveredSlot.index < 9) {
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§e🧩 Auto-Crafting Recipe Grid (Slot " + (this.hoveredSlot.index + 1) + "/9)"),
                        Component.literal("§7Place recipe pattern items here to encode an automated craft.")
                ), mouseX, mouseY);
            } else if (this.hoveredSlot.index == 9) {
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§d🔥 Overclock Upgrade Socket"),
                        Component.literal("§7Accepts: §aBlaze Overclock Core"),
                        Component.literal("§8Boosts computation & rapid synthesis speed.")
                ), mouseX, mouseY);
            } else if (this.hoveredSlot.index >= 10 && this.hoveredSlot.index <= 13) {
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§a✨ Synthesized Output Buffer"),
                        Component.literal("§7Synthesized batch items appear here.")
                ), mouseX, mouseY);
            } else if (this.hoveredSlot.index == 14) {
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6🔍 Target Recipe Preview"),
                        Component.literal("§7Shows the result of the configured 3x3 pattern.")
                ), mouseX, mouseY);
            }
        }
    }
}
