package net.enchantedwood;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import net.enchantedwood.block.entity.ModBlockEntities;
import net.enchantedwood.client.CustomHeartHudRenderer;
import net.enchantedwood.client.renderer.EnchantedChestBlockEntityRenderer;
import net.enchantedwood.screen.ModScreenHandlers;
import net.enchantedwood.screen.CrusherScreen;

public class EnchantedWoodClient implements ClientModInitializer {
    private static KeyMapping openEquipmentKey;
    private static KeyMapping openSuitKey;

    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModScreenHandlers.CRUSHER_SCREEN_HANDLER, CrusherScreen::new);
        MenuScreens.register(ModScreenHandlers.DUST_SMELTER_SCREEN_HANDLER, net.enchantedwood.screen.DustSmelterScreen::new);
        MenuScreens.register(ModScreenHandlers.HYDRAULIC_PRESS_SCREEN_HANDLER, net.enchantedwood.screen.HydraulicPressScreen::new);
        MenuScreens.register(ModScreenHandlers.ENCHANTED_LAVA_GENERATOR_SCREEN_HANDLER, net.enchantedwood.screen.EnchantedLavaGeneratorScreen::new);
        MenuScreens.register(ModScreenHandlers.ENCHANTED_CHEST_SCREEN_HANDLER, net.enchantedwood.screen.EnchantedChestScreen::new);
        MenuScreens.register(ModScreenHandlers.ENCHANTED_STORAGE_CONTROLLER_SCREEN_HANDLER, net.enchantedwood.screen.EnchantedStorageControllerScreen::new);
        MenuScreens.register(ModScreenHandlers.ENCHANTED_DRIVE_BAY_SCREEN_HANDLER, net.enchantedwood.screen.EnchantedDriveBayScreen::new);
        MenuScreens.register(ModScreenHandlers.ENCHANTED_STORAGE_TERMINAL_SCREEN_HANDLER, net.enchantedwood.screen.EnchantedStorageTerminalScreen::new);
        MenuScreens.register(ModScreenHandlers.EQUIPMENT_SCREEN_HANDLER, net.enchantedwood.screen.EquipmentScreen::new);
        MenuScreens.register(ModScreenHandlers.COPPER_GENERATOR_SCREEN_HANDLER, net.enchantedwood.screen.CopperGeneratorScreen::new);
        MenuScreens.register(ModScreenHandlers.COPPER_BATTERY_SCREEN_HANDLER, net.enchantedwood.screen.CopperBatteryScreen::new);
        MenuScreens.register(ModScreenHandlers.OXYGEN_GENERATOR_SCREEN_HANDLER, net.enchantedwood.screen.OxygenGeneratorScreen::new);
        MenuScreens.register(ModScreenHandlers.ALUMINUM_REFINER_SCREEN_HANDLER, net.enchantedwood.screen.AluminumRefinerScreen::new);
        MenuScreens.register(ModScreenHandlers.ALUMINUM_GENERATOR_SCREEN_HANDLER, net.enchantedwood.screen.AluminumGeneratorScreen::new);
        MenuScreens.register(ModScreenHandlers.ALUMINUM_BATTERY_SCREEN_HANDLER, net.enchantedwood.screen.AluminumBatteryScreen::new);
        MenuScreens.register(ModScreenHandlers.COKE_OVEN_SCREEN_HANDLER, net.enchantedwood.screen.CokeOvenScreen::new);
        MenuScreens.register(ModScreenHandlers.STEEL_BLAST_FURNACE_SCREEN_HANDLER, net.enchantedwood.screen.SteelBlastFurnaceScreen::new);
        MenuScreens.register(ModScreenHandlers.STEEL_GENERATOR_SCREEN_HANDLER, net.enchantedwood.screen.SteelGeneratorScreen::new);
        MenuScreens.register(ModScreenHandlers.STEEL_BATTERY_SCREEN_HANDLER, net.enchantedwood.screen.SteelBatteryScreen::new);
        MenuScreens.register(ModScreenHandlers.FUEL_REFINERY_SCREEN_HANDLER, net.enchantedwood.screen.FuelRefineryScreen::new);
        MenuScreens.register(ModScreenHandlers.ROAD_PAVER_SCREEN_HANDLER, net.enchantedwood.screen.RoadPaverScreen::new);
        MenuScreens.register(ModScreenHandlers.ROAD_PAVER_MK2_SCREEN_HANDLER, net.enchantedwood.screen.RoadPaverMk2Screen::new);
        MenuScreens.register(ModScreenHandlers.ATV_SCREEN_HANDLER, net.enchantedwood.screen.AtvScreen::new);
        MenuScreens.register(ModScreenHandlers.VEHICLE_FABRICATOR_SCREEN_HANDLER, net.enchantedwood.screen.VehicleFabricatorScreen::new);

        // Phase 2: Nether Factory & Tier 4 Power Grid
        MenuScreens.register(ModScreenHandlers.TUNGSTEN_BATTERY_SCREEN_HANDLER, net.enchantedwood.screen.TungstenBatteryScreen::new);
        MenuScreens.register(ModScreenHandlers.GEOTHERMAL_GENERATOR_SCREEN_HANDLER, net.enchantedwood.screen.GeothermalGeneratorScreen::new);
        MenuScreens.register(ModScreenHandlers.ALLOY_FOUNDRY_SCREEN_HANDLER, net.enchantedwood.screen.AlloyFoundryScreen::new);
        MenuScreens.register(ModScreenHandlers.ITEM_SALVAGER_SCREEN_HANDLER, net.enchantedwood.screen.ItemSalvagerScreen::new);
        MenuScreens.register(ModScreenHandlers.MAGMA_CRUCIBLE_SCREEN_HANDLER, net.enchantedwood.screen.MagmaCrucibleScreen::new);
        MenuScreens.register(ModScreenHandlers.LAVA_PUMP_SCREEN_HANDLER, net.enchantedwood.screen.LavaPumpScreen::new);
        MenuScreens.register(ModScreenHandlers.WATER_PUMP_SCREEN_HANDLER, net.enchantedwood.screen.WaterPumpScreen::new);
        MenuScreens.register(ModScreenHandlers.CRYO_FREEZER_SCREEN_HANDLER, net.enchantedwood.screen.CryoFreezerScreen::new);
        MenuScreens.register(ModScreenHandlers.CRUSHER_MK2_SCREEN_HANDLER, net.enchantedwood.screen.CrusherMk2Screen::new);
        MenuScreens.register(ModScreenHandlers.DUST_SMELTER_MK2_SCREEN_HANDLER, net.enchantedwood.screen.DustSmelterMk2Screen::new);
        MenuScreens.register(ModScreenHandlers.SOIL_INFUSER_SCREEN_HANDLER, net.enchantedwood.screen.SoilInfuserScreen::new);
        MenuScreens.register(ModScreenHandlers.TITANIUM_TANK_SCREEN_HANDLER, net.enchantedwood.screen.TitaniumTankScreen::new);
        MenuScreens.register(ModScreenHandlers.SUPER_COMPUTER_SCREEN_HANDLER, net.enchantedwood.screen.SuperComputerScreen::new);
        MenuScreens.register(ModScreenHandlers.LASER_QUARRY_SCREEN_HANDLER, net.enchantedwood.screen.LaserQuarryScreen::new);
        MenuScreens.register(ModScreenHandlers.MODULAR_SUIT_SCREEN_HANDLER, net.enchantedwood.screen.ModularSuitScreen::new);
        MenuScreens.register(ModScreenHandlers.POWERED_ANVIL_SCREEN_HANDLER, net.enchantedwood.screen.PoweredAnvilScreen::new);
        MenuScreens.register(ModScreenHandlers.INDUSTRIAL_CENTRIFUGE_SCREEN_HANDLER, net.enchantedwood.screen.IndustrialCentrifugeScreen::new);
        MenuScreens.register(ModScreenHandlers.CHEMICAL_SYNTHESIZER_SCREEN_HANDLER, net.enchantedwood.screen.ChemicalSynthesizerScreen::new);
        MenuScreens.register(ModScreenHandlers.POLYMER_LOOM_SCREEN_HANDLER, net.enchantedwood.screen.PolymerLoomScreen::new);
        MenuScreens.register(ModScreenHandlers.STERILE_MEDICAL_CABINET_SCREEN_HANDLER, net.enchantedwood.screen.SterileMedicalCabinetScreen::new);
        MenuScreens.register(ModScreenHandlers.CIRCUIT_FABRICATOR_SCREEN_HANDLER, net.enchantedwood.screen.CircuitFabricatorScreen::new);
        MenuScreens.register(ModScreenHandlers.INDUCTION_SMELTER_SCREEN_HANDLER, net.enchantedwood.screen.InductionSmelterScreen::new);
        MenuScreens.register(ModScreenHandlers.CASTING_PORT_SCREEN_HANDLER, net.enchantedwood.screen.CastingPortScreen::new);
        MenuScreens.register(ModScreenHandlers.HARMONIC_RECORD_PRESS_SCREEN_HANDLER, net.enchantedwood.screen.HarmonicRecordPressScreen::new);
        MenuScreens.register(ModScreenHandlers.BRICK_OVEN_SCREEN_HANDLER, net.enchantedwood.screen.BrickOvenScreen::new);
        MenuScreens.register(ModScreenHandlers.ICE_CREAM_MACHINE_SCREEN_HANDLER, net.enchantedwood.screen.IceCreamMachineScreen::new);

        BlockEntityRenderers.register(ModBlockEntities.ENCHANTED_CHEST_BLOCK_ENTITY, EnchantedChestBlockEntityRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.TITANIUM_TANK_CONTROLLER_BLOCK_ENTITY, net.enchantedwood.client.renderer.TitaniumTankControllerBlockEntityRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(net.enchantedwood.client.renderer.AtvEntityModel.MODEL_LAYER, net.enchantedwood.client.renderer.AtvEntityModel::getTexturedModelData);
        net.minecraft.client.renderer.entity.EntityRenderers.register(net.enchantedwood.entity.ModEntities.ATV, net.enchantedwood.client.renderer.AtvEntityRenderer::new);
        net.minecraft.client.renderer.entity.EntityRenderers.register(net.enchantedwood.entity.ModEntities.CONVERGENCE_ZOMBIE, net.enchantedwood.client.renderer.ConvergenceZombieRenderer::new);
        net.minecraft.client.renderer.entity.EntityRenderers.register(net.enchantedwood.entity.ModEntities.CONVERGENCE_SKELETON, net.enchantedwood.client.renderer.ConvergenceSkeletonRenderer::new);
        net.minecraft.client.renderer.entity.EntityRenderers.register(net.enchantedwood.entity.ModEntities.CONVERGENCE_CREEPER, net.enchantedwood.client.renderer.ConvergenceCreeperRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(net.enchantedwood.client.renderer.ResonanceColossusModel.MODEL_LAYER, net.enchantedwood.client.renderer.ResonanceColossusModel::getTexturedModelData);
        net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(net.enchantedwood.client.renderer.ResonancePylonModel.MODEL_LAYER, net.enchantedwood.client.renderer.ResonancePylonModel::getTexturedModelData);
        net.minecraft.client.renderer.entity.EntityRenderers.register(net.enchantedwood.entity.ModEntities.CONVERGENCE_SPIDER, net.enchantedwood.client.renderer.ConvergenceSpiderRenderer::new);
        net.minecraft.client.renderer.entity.EntityRenderers.register(net.enchantedwood.entity.ModEntities.RESONANCE_COLOSSUS, net.enchantedwood.client.renderer.ResonanceColossusRenderer::new);
        net.minecraft.client.renderer.entity.EntityRenderers.register(net.enchantedwood.entity.ModEntities.RESONANCE_PYLON, net.enchantedwood.client.renderer.ResonancePylonRenderer::new);
        net.minecraft.client.renderer.entity.EntityRenderers.register(net.enchantedwood.entity.ModEntities.ASCENDANT_COLOSSUS, net.enchantedwood.client.renderer.AscendantColossusRenderer::new);
        net.minecraft.client.renderer.entity.EntityRenderers.register(net.enchantedwood.entity.ModEntities.PRIMORDIAL_CATACLYSM, net.enchantedwood.client.renderer.PrimordialCataclysmRenderer::new);




























        CustomHeartHudRenderer.register();
        net.enchantedwood.client.ModularSuitHudRenderer.register();
        net.enchantedwood.network.ModMessages.registerClientReceivers();

        // Register Keybinding 'C' to open Equipment GUI
        openEquipmentKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.enchantedwood.open_equipment",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_C,
                KeyMapping.Category.INVENTORY
        ));

        // Register Keybinding 'V' to open Modular Suit Access Panel
        openSuitKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.enchantedwood.modular_suit_panel",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_V,
                KeyMapping.Category.INVENTORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openEquipmentKey.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) {
                    client.player.connection.sendCommand("equipment");
                }
            }

            while (openSuitKey.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) {
                    net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new net.enchantedwood.network.OpenModularSuitPanelPayload());
                }
            }

            // Pressing inventory key while riding an ATV opens the ATV Dashboard GUI!
            if (client.player != null && client.player.getVehicle() instanceof net.enchantedwood.entity.custom.AtvEntity && client.gui.screen() == null) {
                while (client.options.keyInventory.consumeClick()) {
                    net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new net.enchantedwood.network.OpenAtvInventoryPayload());
                }
            }
        });

        // Add Equipment & Modular Suit & ATV Dashboard Buttons directly to Player Inventory Screen (InventoryScreen)
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof InventoryScreen inventoryScreen) {
                int x = (scaledWidth - 176) / 2 + 65;
                int y = (scaledHeight - 166) / 2 + 9;
                Screens.getWidgets(inventoryScreen).add(
                    Button.builder(Component.literal("🎽"), button -> {
                        if (client.player != null) {
                            client.player.connection.sendCommand("equipment");
                        }
                    }).bounds(x, y, 14, 14).build()
                );

                // Modular Suit Button
                Screens.getWidgets(inventoryScreen).add(
                    Button.builder(Component.literal("⚡"), button -> {
                        if (client.player != null) {
                            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new net.enchantedwood.network.OpenModularSuitPanelPayload());
                        }
                    }).bounds(x + 16, y, 14, 14).build()
                );

                // If player is mounted on ATV, show Dashboard button
                if (client.player != null && client.player.getVehicle() instanceof net.enchantedwood.entity.custom.AtvEntity) {
                    Screens.getWidgets(inventoryScreen).add(
                        Button.builder(Component.literal("🏎️"), button -> {
                            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new net.enchantedwood.network.OpenAtvInventoryPayload());
                        }).bounds(x + 32, y, 14, 14).build()
                    );
                }
            }
        });
    }
}
