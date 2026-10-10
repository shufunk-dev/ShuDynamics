package net.enchantedwood.world.dimension;

import net.enchantedwood.EnchantedWoodMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

public class ModDimensions {
    public static final ResourceKey<LevelStem> MINING_DIMENSION_OPTIONS_KEY =
            ResourceKey.create(Registries.LEVEL_STEM, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "mining_dimension"));
    public static final ResourceKey<Level> MINING_DIMENSION_WORLD_KEY =
            ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "mining_dimension"));
    public static final ResourceKey<DimensionType> MINING_DIMENSION_TYPE_KEY =
            ResourceKey.create(Registries.DIMENSION_TYPE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "mining_dimension"));

    public static final ResourceKey<LevelStem> CONVERGENCE_OPTIONS_KEY =
            ResourceKey.create(Registries.LEVEL_STEM, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "convergence"));
    public static final ResourceKey<Level> CONVERGENCE_WORLD_KEY =
            ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "convergence"));
    public static final ResourceKey<DimensionType> CONVERGENCE_TYPE_KEY =
            ResourceKey.create(Registries.DIMENSION_TYPE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "convergence"));

    public static void registerDimensions() {
        EnchantedWoodMod.LOGGER.info("Registering Custom Dimensions for " + EnchantedWoodMod.MOD_ID);
    }
}
