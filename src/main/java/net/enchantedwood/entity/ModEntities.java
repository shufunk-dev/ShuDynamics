package net.enchantedwood.entity;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.entity.custom.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final RegistryKey<EntityType<?>> ATV_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(EnchantedWoodMod.MOD_ID, "atv"));

    public static final EntityType<AtvEntity> ATV = Registry.register(
            Registries.ENTITY_TYPE,
            ATV_KEY,
            EntityType.Builder.<AtvEntity>create(AtvEntity::new, SpawnGroup.MISC)
                    .dimensions(1.4f, 1.1f)
                    .build(ATV_KEY)
    );

    // Convergence Mob Variants
    public static final RegistryKey<EntityType<?>> CONVERGENCE_ZOMBIE_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(EnchantedWoodMod.MOD_ID, "convergence_zombie"));
    public static final EntityType<ConvergenceZombieEntity> CONVERGENCE_ZOMBIE = Registry.register(
            Registries.ENTITY_TYPE,
            CONVERGENCE_ZOMBIE_KEY,
            EntityType.Builder.<ConvergenceZombieEntity>create(ConvergenceZombieEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 1.95f)
                    .build(CONVERGENCE_ZOMBIE_KEY)
    );

    public static final RegistryKey<EntityType<?>> CONVERGENCE_SKELETON_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(EnchantedWoodMod.MOD_ID, "convergence_skeleton"));
    public static final EntityType<ConvergenceSkeletonEntity> CONVERGENCE_SKELETON = Registry.register(
            Registries.ENTITY_TYPE,
            CONVERGENCE_SKELETON_KEY,
            EntityType.Builder.<ConvergenceSkeletonEntity>create(ConvergenceSkeletonEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 1.99f)
                    .build(CONVERGENCE_SKELETON_KEY)
    );

    public static final RegistryKey<EntityType<?>> CONVERGENCE_CREEPER_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(EnchantedWoodMod.MOD_ID, "convergence_creeper"));
    public static final EntityType<ConvergenceCreeperEntity> CONVERGENCE_CREEPER = Registry.register(
            Registries.ENTITY_TYPE,
            CONVERGENCE_CREEPER_KEY,
            EntityType.Builder.<ConvergenceCreeperEntity>create(ConvergenceCreeperEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 1.7f)
                    .build(CONVERGENCE_CREEPER_KEY)
    );

    public static final RegistryKey<EntityType<?>> CONVERGENCE_SPIDER_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(EnchantedWoodMod.MOD_ID, "convergence_spider"));
    public static final EntityType<ConvergenceSpiderEntity> CONVERGENCE_SPIDER = Registry.register(
            Registries.ENTITY_TYPE,
            CONVERGENCE_SPIDER_KEY,
            EntityType.Builder.<ConvergenceSpiderEntity>create(ConvergenceSpiderEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.4f, 0.9f)
                    .build(CONVERGENCE_SPIDER_KEY)
    );

    // Dimension Boss: The Resonance Colossus (Tier 1)
    public static final RegistryKey<EntityType<?>> RESONANCE_COLOSSUS_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(EnchantedWoodMod.MOD_ID, "resonance_colossus"));
    public static final EntityType<ResonanceColossusEntity> RESONANCE_COLOSSUS = Registry.register(
            Registries.ENTITY_TYPE,
            RESONANCE_COLOSSUS_KEY,
            EntityType.Builder.<ResonanceColossusEntity>create(ResonanceColossusEntity::new, SpawnGroup.MONSTER)
                    .dimensions(2.2f, 4.5f)
                    .build(RESONANCE_COLOSSUS_KEY)
    );

    // Resonance Pylon (Tier 1 Shield Anchor)
    public static final RegistryKey<EntityType<?>> RESONANCE_PYLON_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(EnchantedWoodMod.MOD_ID, "resonance_pylon"));
    public static final EntityType<ResonancePylonEntity> RESONANCE_PYLON = Registry.register(
            Registries.ENTITY_TYPE,
            RESONANCE_PYLON_KEY,
            EntityType.Builder.<ResonancePylonEntity>create(ResonancePylonEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.0f, 2.5f)
                    .build(RESONANCE_PYLON_KEY)
    );

    // Dimension Boss: The Ascendant Colossus (Tier 2)
    public static final RegistryKey<EntityType<?>> ASCENDANT_COLOSSUS_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(EnchantedWoodMod.MOD_ID, "ascendant_colossus"));
    public static final EntityType<AscendantColossusEntity> ASCENDANT_COLOSSUS = Registry.register(
            Registries.ENTITY_TYPE,
            ASCENDANT_COLOSSUS_KEY,
            EntityType.Builder.<AscendantColossusEntity>create(AscendantColossusEntity::new, SpawnGroup.MONSTER)
                    .dimensions(2.4f, 5.0f)
                    .build(ASCENDANT_COLOSSUS_KEY)
    );

    // Dimension Boss: The Primordial Cataclysm (Tier 3 Mythic Final Boss)
    public static final RegistryKey<EntityType<?>> PRIMORDIAL_CATACLYSM_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(EnchantedWoodMod.MOD_ID, "primordial_cataclysm"));
    public static final EntityType<PrimordialCataclysmEntity> PRIMORDIAL_CATACLYSM = Registry.register(
            Registries.ENTITY_TYPE,
            PRIMORDIAL_CATACLYSM_KEY,
            EntityType.Builder.<PrimordialCataclysmEntity>create(PrimordialCataclysmEntity::new, SpawnGroup.MONSTER)
                    .dimensions(2.6f, 5.5f)
                    .build(PRIMORDIAL_CATACLYSM_KEY)
    );

    public static void registerModEntities() {
        EnchantedWoodMod.LOGGER.info("Registering Entities for " + EnchantedWoodMod.MOD_ID);

        FabricDefaultAttributeRegistry.register(CONVERGENCE_ZOMBIE, ConvergenceZombieEntity.createConvergenceZombieAttributes());
        FabricDefaultAttributeRegistry.register(CONVERGENCE_SKELETON, ConvergenceSkeletonEntity.createConvergenceSkeletonAttributes());
        FabricDefaultAttributeRegistry.register(CONVERGENCE_CREEPER, ConvergenceCreeperEntity.createConvergenceCreeperAttributes());
        FabricDefaultAttributeRegistry.register(CONVERGENCE_SPIDER, ConvergenceSpiderEntity.createConvergenceSpiderAttributes());
        FabricDefaultAttributeRegistry.register(RESONANCE_COLOSSUS, ResonanceColossusEntity.createResonanceColossusAttributes());
        FabricDefaultAttributeRegistry.register(RESONANCE_PYLON, ResonancePylonEntity.createPylonAttributes());
        FabricDefaultAttributeRegistry.register(ASCENDANT_COLOSSUS, AscendantColossusEntity.createAscendantColossusAttributes());
        FabricDefaultAttributeRegistry.register(PRIMORDIAL_CATACLYSM, PrimordialCataclysmEntity.createPrimordialCataclysmAttributes());
    }
}
