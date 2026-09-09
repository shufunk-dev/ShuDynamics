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

    // Dimension Boss: The Resonance Colossus
    public static final RegistryKey<EntityType<?>> RESONANCE_COLOSSUS_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(EnchantedWoodMod.MOD_ID, "resonance_colossus"));
    public static final EntityType<ResonanceColossusEntity> RESONANCE_COLOSSUS = Registry.register(
            Registries.ENTITY_TYPE,
            RESONANCE_COLOSSUS_KEY,
            EntityType.Builder.<ResonanceColossusEntity>create(ResonanceColossusEntity::new, SpawnGroup.MONSTER)
                    .dimensions(2.2f, 4.5f)
                    .build(RESONANCE_COLOSSUS_KEY)
    );

    public static void registerModEntities() {
        EnchantedWoodMod.LOGGER.info("Registering Entities for " + EnchantedWoodMod.MOD_ID);

        FabricDefaultAttributeRegistry.register(CONVERGENCE_ZOMBIE, ConvergenceZombieEntity.createConvergenceZombieAttributes());
        FabricDefaultAttributeRegistry.register(CONVERGENCE_SKELETON, ConvergenceSkeletonEntity.createConvergenceSkeletonAttributes());
        FabricDefaultAttributeRegistry.register(CONVERGENCE_CREEPER, ConvergenceCreeperEntity.createConvergenceCreeperAttributes());
        FabricDefaultAttributeRegistry.register(CONVERGENCE_SPIDER, ConvergenceSpiderEntity.createConvergenceSpiderAttributes());
        FabricDefaultAttributeRegistry.register(RESONANCE_COLOSSUS, ResonanceColossusEntity.createResonanceColossusAttributes());
    }
}
