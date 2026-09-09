package net.enchantedwood.event;

import net.enchantedwood.entity.ModEntities;
import net.enchantedwood.world.dimension.ModDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.server.world.ServerWorld;

public class ConvergenceMobSpawnHandler {

    private static final String CONVERGENCE_CONVERTED_TAG = "convergence_spawn_checked";

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((Entity entity, ServerWorld world) -> {
            // Only process inside The Convergence dimension
            if (world.getRegistryKey() != ModDimensions.CONVERGENCE_WORLD_KEY) {
                return;
            }

            // If already evaluated or tagged, ignore
            if (entity.getCommandTags().contains(CONVERGENCE_CONVERTED_TAG)) {
                return;
            }
            entity.addCommandTag(CONVERGENCE_CONVERTED_TAG);

            // 40% chance to convert vanilla mob into its Convergence Variant (60% remain vanilla)
            if (world.getRandom().nextFloat() > 0.40f) {
                return;
            }

            EntityType<?> targetType = null;
            if (entity instanceof ZombieEntity && !(entity instanceof net.enchantedwood.entity.custom.ConvergenceZombieEntity)) {
                targetType = ModEntities.CONVERGENCE_ZOMBIE;
            } else if (entity instanceof SkeletonEntity && !(entity instanceof net.enchantedwood.entity.custom.ConvergenceSkeletonEntity)) {
                targetType = ModEntities.CONVERGENCE_SKELETON;
            } else if (entity instanceof CreeperEntity && !(entity instanceof net.enchantedwood.entity.custom.ConvergenceCreeperEntity)) {
                targetType = ModEntities.CONVERGENCE_CREEPER;
            } else if (entity instanceof SpiderEntity && !(entity instanceof net.enchantedwood.entity.custom.ConvergenceSpiderEntity)) {
                targetType = ModEntities.CONVERGENCE_SPIDER;
            }

            if (targetType != null) {
                Entity variant = targetType.create(world, SpawnReason.NATURAL);
                if (variant != null) {
                    variant.copyPositionAndRotation(entity);
                    variant.addCommandTag(CONVERGENCE_CONVERTED_TAG);
                    world.spawnEntity(variant);
                    entity.discard();
                }
            }
        });
    }
}
