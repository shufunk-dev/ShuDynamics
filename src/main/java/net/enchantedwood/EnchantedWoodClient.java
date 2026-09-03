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
        HandledScreens.register(ModScreenHandlers.HYDRAULIC_PRESS_SCREEN_HANDLER, net.enchantedwood.screen.HydraulicPressScreen::new);
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
        HandledScreens.register(ModScreenHandlers.FUEL_REFINERY_SCREEN_HANDLER, net.enchantedwood.screen.FuelRefineryScreen::new);
        HandledScreens.register(ModScreenHandlers.ROAD_PAVER_SCREEN_HANDLER, net.enchantedwood.screen.RoadPaverScreen::new);
        HandledScreens.register(ModScreenHandlers.ATV_SCREEN_HANDLER, net.enchantedwood.screen.AtvScreen::new);
        HandledScreens.register(ModScreenHandlers.VEHICLE_FABRICATOR_SCREEN_HANDLER, net.enchantedwood.screen.VehicleFabricatorScreen::new);

        // Phase 2: Nether Factory & Tier 4 Power Grid
        HandledScreens.register(ModScreenHandlers.TUNGSTEN_BATTERY_SCREEN_HANDLER, net.enchantedwood.screen.TungstenBatteryScreen::new);
        HandledScreens.register(ModScreenHandlers.GEOTHERMAL_GENERATOR_SCREEN_HANDLER, net.enchantedwood.screen.GeothermalGeneratorScreen::new);
        HandledScreens.register(ModScreenHandlers.ALLOY_FOUNDRY_SCREEN_HANDLER, net.enchantedwood.screen.AlloyFoundryScreen::new);
        HandledScreens.register(ModScreenHandlers.ITEM_SALVAGER_SCREEN_HANDLER, net.enchantedwood.screen.ItemSalvagerScreen::new);
        HandledScreens.register(ModScreenHandlers.MAGMA_CRUCIBLE_SCREEN_HANDLER, net.enchantedwood.screen.MagmaCrucibleScreen::new);
        HandledScreens.register(ModScreenHandlers.LAVA_PUMP_SCREEN_HANDLER, net.enchantedwood.screen.LavaPumpScreen::new);
        HandledScreens.register(ModScreenHandlers.CRUSHER_MK2_SCREEN_HANDLER, net.enchantedwood.screen.CrusherMk2Screen::new);
        HandledScreens.register(ModScreenHandlers.SOIL_INFUSER_SCREEN_HANDLER, net.enchantedwood.screen.SoilInfuserScreen::new);
        HandledScreens.register(ModScreenHandlers.TITANIUM_TANK_SCREEN_HANDLER, net.enchantedwood.screen.TitaniumTankScreen::new);
        HandledScreens.register(ModScreenHandlers.SUPER_COMPUTER_SCREEN_HANDLER, net.enchantedwood.screen.SuperComputerScreen::new);
        HandledScreens.register(ModScreenHandlers.LASER_QUARRY_SCREEN_HANDLER, net.enchantedwood.screen.LaserQuarryScreen::new);

        BlockEntityRendererFactories.register(ModBlockEntities.ENCHANTED_CHEST_BLOCK_ENTITY, EnchantedChestBlockEntityRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(net.enchantedwood.client.renderer.AtvEntityModel.MODEL_LAYER, net.enchantedwood.client.renderer.AtvEntityModel::getTexturedModelData);
        net.minecraft.client.render.entity.EntityRendererFactories.register(net.enchantedwood.entity.ModEntities.ATV, net.enchantedwood.client.renderer.AtvEntityRenderer::new);

        net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlock(net.enchantedwood.block.ModBlocks.CORN_CROP, net.minecraft.client.render.BlockRenderLayer.CUTOUT);
        net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlock(net.enchantedwood.block.ModBlocks.REINFORCED_TANK_GLASS, net.minecraft.client.render.BlockRenderLayer.CUTOUT);
        net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlock(net.enchantedwood.block.ModBlocks.VOLCANIC_GLASS, net.minecraft.client.render.BlockRenderLayer.TRANSLUCENT);

        CustomHeartHudRenderer.register();
        net.enchantedwood.network.ModMessages.registerClientReceivers();

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

            // Pressing inventory key while riding an ATV opens the ATV Dashboard GUI!
            if (client.player != null && client.player.getVehicle() instanceof net.enchantedwood.entity.custom.AtvEntity && client.currentScreen == null) {
                while (client.options.inventoryKey.wasPressed()) {
                    net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new net.enchantedwood.network.OpenAtvInventoryPayload());
                }
            }
        });

        // Add Equipment & ATV Dashboard Buttons directly to Player Inventory Screen (InventoryScreen)
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

                // If player is mounted on ATV, show Dashboard button
                if (client.player != null && client.player.getVehicle() instanceof net.enchantedwood.entity.custom.AtvEntity) {
                    Screens.getButtons(inventoryScreen).add(
                        ButtonWidget.builder(Text.literal("🏎️"), button -> {
                            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new net.enchantedwood.network.OpenAtvInventoryPayload());
                        }).dimensions(x + 16, y, 14, 14).build()
                    );
                }
            }
        });
    }
}
