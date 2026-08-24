package net.enchantedwood.world.dimension;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.enchantedwood.EnchantedWoodMod;

public class ModDimensions {
    public static final RegistryKey<DimensionOptions> MINING_DIMENSION_OPTIONS_KEY =
            RegistryKey.of(RegistryKeys.DIMENSION, Identifier.of(EnchantedWoodMod.MOD_ID, "mining_dimension"));
    public static final RegistryKey<World> MINING_DIMENSION_WORLD_KEY =
            RegistryKey.of(RegistryKeys.WORLD, Identifier.of(EnchantedWoodMod.MOD_ID, "mining_dimension"));
    public static final RegistryKey<DimensionType> MINING_DIMENSION_TYPE_KEY =
            RegistryKey.of(RegistryKeys.DIMENSION_TYPE, Identifier.of(EnchantedWoodMod.MOD_ID, "mining_dimension"));

    public static void registerDimensions() {
        EnchantedWoodMod.LOGGER.info("Registering Custom Dimensions for " + EnchantedWoodMod.MOD_ID);
    }
}
