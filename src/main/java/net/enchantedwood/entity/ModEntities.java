package net.enchantedwood.entity;

import net.enchantedwood.EnchantedWoodMod;
import net.enchantedwood.entity.custom.AtvEntity;
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

    public static void registerModEntities() {
        EnchantedWoodMod.LOGGER.info("Registering Entities for " + EnchantedWoodMod.MOD_ID);
    }
}
