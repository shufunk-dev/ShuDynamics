package net.enchantedwood;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.enchantedwood.client.CustomHeartHudRenderer;
import net.enchantedwood.client.renderer.EnchantedChestBlockEntityRenderer;
import net.enchantedwood.screen.ModScreenHandlers;
import net.enchantedwood.screen.CrusherScreen;
import org.lwjgl.glfw.GLFW;

public class EnchantedWoodClient implements ClientModInitializer {
    private static KeyBinding openEquipmentKey;

    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.CRUSHER_SCREEN_HANDLER, CrusherScreen::new);
        HandledScreens.register(ModScreenHandlers.DUST_SMELTER_SCREEN_HANDLER, net.enchantedwood.screen.DustSmelterScreen::new);
        HandledScreens.register(ModScreenHandlers.ENCHANTED_LAVA_GENERATOR_SCREEN_HANDLER, net.enchantedwood.screen.EnchantedLavaGeneratorScreen::new);
        HandledScreens.register(ModScreenHandlers.ENCHANTED_CHEST_SCREEN_HANDLER, net.enchantedwood.screen.EnchantedChestScreen::new);
        HandledScreens.register(ModScreenHandlers.ENCHANTED_STORAGE_CONTROLLER_SCREEN_HANDLER, net.enchantedwood.screen.EnchantedStorageControllerScreen::new);
        HandledScreens.register(ModScreenHandlers.ENCHANTED_DRIVE_BAY_SCREEN_HANDLER, net.enchantedwood.screen.EnchantedDriveBayScreen::new);
        HandledScreens.register(ModScreenHandlers.ENCHANTED_STORAGE_TERMINAL_SCREEN_HANDLER, net.enchantedwood.screen.EnchantedStorageTerminalScreen::new);
        HandledScreens.register(ModScreenHandlers.EQUIPMENT_SCREEN_HANDLER, net.enchantedwood.screen.EquipmentScreen::new);
        HandledScreens.register(ModScreenHandlers.COPPER_GENERATOR_SCREEN_HANDLER, net.enchantedwood.screen.CopperGeneratorScreen::new);
        HandledScreens.register(ModScreenHandlers.COPPER_BATTERY_SCREEN_HANDLER, net.enchantedwood.screen.CopperBatteryScreen::new);
        HandledScreens.register(ModScreenHandlers.OXYGEN_GENERATOR_SCREEN_HANDLER, net.enchantedwood.screen.OxygenGeneratorScreen::new);
        HandledScreens.register(ModScreenHandlers.ALUMINUM_REFINER_SCREEN_HANDLER, net.enchantedwood.screen.AluminumRefinerScreen::new);
        HandledScreens.register(ModScreenHandlers.ALUMINUM_GENERATOR_SCREEN_HANDLER, net.enchantedwood.screen.AluminumGeneratorScreen::new);
        HandledScreens.register(ModScreenHandlers.ALUMINUM_BATTERY_SCREEN_HANDLER, net.enchantedwood.screen.AluminumBatteryScreen::new);
        HandledScreens.register(ModScreenHandlers.COKE_OVEN_SCREEN_HANDLER, net.enchantedwood.screen.CokeOvenScreen::new);
        HandledScreens.register(ModScreenHandlers.STEEL_BLAST_FURNACE_SCREEN_HANDLER, net.enchantedwood.screen.SteelBlastFurnaceScreen::new);
        HandledScreens.register(ModScreenHandlers.STEEL_GENERATOR_SCREEN_HANDLER, net.enchantedwood.screen.SteelGeneratorScreen::new);
        HandledScreens.register(ModScreenHandlers.STEEL_BATTERY_SCREEN_HANDLER, net.enchantedwood.screen.SteelBatteryScreen::new);

        BlockEntityRendererFactories.register(ModBlockEntities.ENCHANTED_CHEST_BLOCK_ENTITY, EnchantedChestBlockEntityRenderer::new);






        CustomHeartHudRenderer.register();

        // Register Keybinding 'C' to open Equipment GUI
        openEquipmentKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.enchantedwood.open_equipment",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                KeyBinding.Category.INVENTORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openEquipmentKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.player.networkHandler.sendChatCommand("equipment");
                }
            }
        });

        // Add Equipment Button directly to Player Inventory Screen (InventoryScreen)
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof InventoryScreen inventoryScreen) {
                int x = (scaledWidth - 176) / 2 + 65;
                int y = (scaledHeight - 166) / 2 + 9;
                Screens.getButtons(inventoryScreen).add(
                    ButtonWidget.builder(Text.literal("🎽"), button -> {
                        if (client.player != null) {
                            client.player.networkHandler.sendChatCommand("equipment");
                        }
                    }).dimensions(x, y, 14, 14).build()
                );
            }
        });
    }
}
