package net.enchantedwood.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.enchantedwood.EnchantedWoodMod;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public class EnchantedStorageTerminalScreen extends AbstractContainerScreen<EnchantedStorageTerminalScreenHandler> {
    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "textures/gui/container/enchanted_chest_gui.png");
    private EditBox searchBox;
    private Button prevButton;
    private Button nextButton;

    public EnchantedStorageTerminalScreen(EnchantedStorageTerminalScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 176, 222);
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

        this.searchBox = new EditBox(this.font, x + 98, y + 4, 70, 11, Component.literal("Search..."));
        this.searchBox.setMaxLength(30);
        this.searchBox.setBordered(true);
        this.searchBox.setCanLoseFocus(true);
        this.searchBox.setHint(Component.literal("Search...").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        this.searchBox.setValue(this.menu.getSearchQuery());
        this.searchBox.setResponder(query -> {
            this.menu.setSearchFilter(query);
            if (net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(net.enchantedwood.network.SetStorageTerminalSearchPayload.ID)) {
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new net.enchantedwood.network.SetStorageTerminalSearchPayload(query));
            }
        });
        this.addRenderableWidget(this.searchBox);

        // Previous Page Button
        this.prevButton = Button.builder(Component.literal("◀"), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            }
        }).bounds(x + 48, y + 3, 14, 12).build();

        // Next Page Button
        this.nextButton = Button.builder(Component.literal("▶"), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
            }
        }).bounds(x + 80, y + 3, 14, 12).build();

        this.addRenderableWidget(this.prevButton);
        this.addRenderableWidget(this.nextButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount > 0) {
            // Scroll Up -> Previous Page
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                return true;
            }
        } else if (verticalAmount < 0) {
            // Scroll Down -> Next Page
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent input) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            if (input.key() == 256) { // GLFW_KEY_ESCAPE
                this.onClose();
                return true;
            }
            if (this.searchBox.keyPressed(input)) {
                return true;
            }
            // Consume key press so inventory key (default 'E') never closes the screen
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent input) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            return this.searchBox.charTyped(input);
        }
        return super.charTyped(input);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        context.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractLabels(context, mouseX, mouseY);

        // Network telemetry in header
        int totalCap = this.menu.getTotalCapacity();
        boolean online = this.menu.isOnline();

        if (!online) {
            context.text(this.font, Component.literal("●").withStyle(net.minecraft.ChatFormatting.RED), 38, 6, 0xFF5555, false);
        } else if (totalCap <= 0) {
            context.text(this.font, Component.literal("●").withStyle(net.minecraft.ChatFormatting.GOLD), 38, 6, 0xFFAA00, false);
        } else {
            context.text(this.font, Component.literal("●").withStyle(net.minecraft.ChatFormatting.GREEN), 38, 6, 0x55FF55, false);
        }

        // Page Indicator between ◀ and ▶ buttons
        int curPage = this.menu.getCurrentPage() + 1;
        int totalPages = this.menu.getTotalPages();
        String pageStr = curPage + "/" + totalPages;
        int strWidth = this.font.width(pageStr);
        context.text(this.font, Component.literal(pageStr).withStyle(net.minecraft.ChatFormatting.DARK_GRAY), 71 - (strWidth / 2), 6, 0x3F3F3F, false);
    }

    public static String formatCount(int count) {
        if (count <= 1) return "";
        if (count < 10000) return String.valueOf(count); // Shows exact count (e.g. 1408, 9999)
        if (count < 1000000) return (count / 1000) + "k";
        if (count < 10000000) return String.format(Locale.ROOT, "%.1fM", count / 1000000.0);
        return (count / 1000000) + "M";
    }

    @Override
    protected void extractSlot(GuiGraphicsExtractor context, Slot slot, int mouseX, int mouseY) {
        if (slot.index < 54) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                int x = slot.x;
                int y = slot.y;
                context.item(stack, x, y);
                context.itemDecorations(this.font, stack, x, y, "");

                String countText = formatCount(stack.getCount());
                if (!countText.isEmpty()) {
                    float scale = 0.75f;
                    int textWidth = this.font.width(countText);
                    float posX = (x + 16.5f) - (textWidth * scale);
                    float posY = (y + 16.5f) - (8.5f * scale);

                    var matrices = context.pose();
                    matrices.pushMatrix();
                    matrices.translate(posX, posY);
                    matrices.scale(scale);
                    context.text(this.font, countText, 0, 0, 0xFFFFFF, true);
                    matrices.popMatrix();
                }
                return;
            }
        }
        super.extractSlot(context, slot, mouseX, mouseY);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem() && this.hoveredSlot.index < 54) {
            ItemStack stack = this.hoveredSlot.getItem();
            List<Component> tooltip = new ArrayList<>(getTooltipFromContainerItem(stack));
            tooltip.add(Component.literal("§6📦 Stored in Network: §e" + String.format(Locale.ROOT, "%,d", stack.getCount())));
            context.setTooltipForNextFrame(this.font, tooltip, stack.getTooltipImage(), mouseX, mouseY);
            return;
        }
        super.extractTooltip(context, mouseX, mouseY);


        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Hover Tooltip for Network Status (x + 34 .. 48, y + 4 .. 16)
        if (mouseX >= x + 34 && mouseX <= x + 48 && mouseY >= y + 4 && mouseY <= y + 16) {
            int totalCap = this.menu.getTotalCapacity();
            int stored = this.menu.getStoredCount();
            boolean online = this.menu.isOnline();

            if (!online) {
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§c⚡ Storage Network: OFFLINE"),
                        Component.literal("§7Connect power to the Enchanted Storage Controller.")
                ), mouseX, mouseY);
            } else if (totalCap <= 0) {
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6💾 Digital Storage Network"),
                        Component.literal("§eStatus: §6NO DRIVES INSTALLED"),
                        Component.literal("§7Install 1k, 4k, 16k, or 64k Storage Crystals"),
                        Component.literal("§7in a nearby Drive Bay to store items.")
                ), mouseX, mouseY);
            } else {
                context.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal("§6💾 Digital Storage Network"),
                        Component.literal("§eItems Stored: §f" + String.format(Locale.ROOT, "%,d / %,d", stored, totalCap)),
                        Component.literal("§aStatus: ONLINE"),
                        Component.literal("§7Use Mouse Wheel or ◀ ▶ buttons to cycle pages.")
                ), mouseX, mouseY);
            }
        }
    }
}
