package net.enchantedwood.entity;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.entity.custom.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
    public static final ResourceKey<EntityType<?>> ATV_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "atv"));

    public static final EntityType<AtvEntity> ATV = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ATV_KEY,
            EntityType.Builder.<AtvEntity>of(AtvEntity::new, MobCategory.MISC)
                    .sized(1.4f, 1.1f)
                    .build(ATV_KEY)
    );

    // Convergence Mob Variants
    public static final ResourceKey<EntityType<?>> CONVERGENCE_ZOMBIE_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "convergence_zombie"));
    public static final EntityType<ConvergenceZombieEntity> CONVERGENCE_ZOMBIE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            CONVERGENCE_ZOMBIE_KEY,
            EntityType.Builder.<ConvergenceZombieEntity>of(ConvergenceZombieEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .build(CONVERGENCE_ZOMBIE_KEY)
    );

    public static final ResourceKey<EntityType<?>> CONVERGENCE_SKELETON_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "convergence_skeleton"));
    public static final EntityType<ConvergenceSkeletonEntity> CONVERGENCE_SKELETON = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            CONVERGENCE_SKELETON_KEY,
            EntityType.Builder.<ConvergenceSkeletonEntity>of(ConvergenceSkeletonEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.99f)
                    .build(CONVERGENCE_SKELETON_KEY)
    );

    public static final ResourceKey<EntityType<?>> CONVERGENCE_CREEPER_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "convergence_creeper"));
    public static final EntityType<ConvergenceCreeperEntity> CONVERGENCE_CREEPER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            CONVERGENCE_CREEPER_KEY,
            EntityType.Builder.<ConvergenceCreeperEntity>of(ConvergenceCreeperEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.7f)
                    .build(CONVERGENCE_CREEPER_KEY)
    );

    public static final ResourceKey<EntityType<?>> CONVERGENCE_SPIDER_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "convergence_spider"));
    public static final EntityType<ConvergenceSpiderEntity> CONVERGENCE_SPIDER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            CONVERGENCE_SPIDER_KEY,
            EntityType.Builder.<ConvergenceSpiderEntity>of(ConvergenceSpiderEntity::new, MobCategory.MONSTER)
                    .sized(1.4f, 0.9f)
                    .build(CONVERGENCE_SPIDER_KEY)
    );

    // Dimension Boss: The Resonance Colossus (Tier 1)
    public static final ResourceKey<EntityType<?>> RESONANCE_COLOSSUS_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "resonance_colossus"));
    public static final EntityType<ResonanceColossusEntity> RESONANCE_COLOSSUS = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            RESONANCE_COLOSSUS_KEY,
            EntityType.Builder.<ResonanceColossusEntity>of(ResonanceColossusEntity::new, MobCategory.MONSTER)
                    .sized(2.2f, 4.5f)
                    .build(RESONANCE_COLOSSUS_KEY)
    );

    // Resonance Pylon (Tier 1 Shield Anchor)
    public static final ResourceKey<EntityType<?>> RESONANCE_PYLON_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "resonance_pylon"));
    public static final EntityType<ResonancePylonEntity> RESONANCE_PYLON = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            RESONANCE_PYLON_KEY,
            EntityType.Builder.<ResonancePylonEntity>of(ResonancePylonEntity::new, MobCategory.MONSTER)
                    .sized(1.0f, 2.5f)
                    .build(RESONANCE_PYLON_KEY)
    );

    // Dimension Boss: The Ascendant Colossus (Tier 2)
    public static final ResourceKey<EntityType<?>> ASCENDANT_COLOSSUS_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "ascendant_colossus"));
    public static final EntityType<AscendantColossusEntity> ASCENDANT_COLOSSUS = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ASCENDANT_COLOSSUS_KEY,
            EntityType.Builder.<AscendantColossusEntity>of(AscendantColossusEntity::new, MobCategory.MONSTER)
                    .sized(2.4f, 5.0f)
                    .build(ASCENDANT_COLOSSUS_KEY)
    );

    // Dimension Boss: The Primordial Cataclysm (Tier 3 Mythic Final Boss)
    public static final ResourceKey<EntityType<?>> PRIMORDIAL_CATACLYSM_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(EnchantedWoodMod.MOD_ID, "primordial_cataclysm"));
    public static final EntityType<PrimordialCataclysmEntity> PRIMORDIAL_CATACLYSM = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            PRIMORDIAL_CATACLYSM_KEY,
            EntityType.Builder.<PrimordialCataclysmEntity>of(PrimordialCataclysmEntity::new, MobCategory.MONSTER)
                    .sized(2.6f, 5.5f)
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
