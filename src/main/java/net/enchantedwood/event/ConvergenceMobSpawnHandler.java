package net.enchantedwood.event;

import net.enchantedwood.entity.ModEntities;
import net.enchantedwood.world.dimension.ModDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.enchantedwood.item.ModItems;

public class ConvergenceMobSpawnHandler {

    private static final String CONVERGENCE_CONVERTED_TAG = "convergence_spawn_checked";

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((Entity entity, ServerLevel world) -> {
            // Only process inside The Convergence dimension
            if (world.dimension() != ModDimensions.CONVERGENCE_WORLD_KEY) {
                return;
            }

            // If already evaluated or tagged, ignore
            if (entity.entityTags().contains(CONVERGENCE_CONVERTED_TAG)) {
                return;
            }
            entity.addTag(CONVERGENCE_CONVERTED_TAG);

            // Discard any hostile mobs attempting to spawn within the 48-block Sanctuary Outpost protection zone
            if (entity instanceof net.minecraft.world.entity.monster.Monster) {
                for (net.minecraft.core.BlockPos center : ConvergenceHazardHandler.SANCTUARY_CENTERS) {
                    if (center.closerThan(entity.blockPosition(), 48.0)) {
                        entity.discard();
                        return;
                    }
                }
            }

            // 40% chance to convert vanilla mob into its Convergence Variant (60% remain vanilla)
            if (world.getRandom().nextFloat() > 0.40f) {
                return;
            }

            EntityType<?> targetType = null;
            if (entity instanceof Zombie && !(entity instanceof net.enchantedwood.entity.custom.ConvergenceZombieEntity)) {
                targetType = ModEntities.CONVERGENCE_ZOMBIE;
            } else if (entity instanceof Skeleton && !(entity instanceof net.enchantedwood.entity.custom.ConvergenceSkeletonEntity)) {
                targetType = ModEntities.CONVERGENCE_SKELETON;
            } else if (entity instanceof Creeper && !(entity instanceof net.enchantedwood.entity.custom.ConvergenceCreeperEntity)) {
                targetType = ModEntities.CONVERGENCE_CREEPER;
            } else if (entity instanceof Spider && !(entity instanceof net.enchantedwood.entity.custom.ConvergenceSpiderEntity)) {
                targetType = ModEntities.CONVERGENCE_SPIDER;
            }

            if (targetType != null) {
                Entity variant = targetType.create(world, EntitySpawnReason.NATURAL);
                if (variant != null) {
                    variant.copyPosition(entity);
                    variant.addTag(CONVERGENCE_CONVERTED_TAG);
                    world.addFreshEntity(variant);
                    entity.discard();
                }
            }
        });

        // Rare drop: Anoxic Echoes from mutant Convergence mobs
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity.level() instanceof ServerLevel sw) {
                if (entity instanceof net.enchantedwood.entity.custom.ConvergenceCreeperEntity ||
                    entity instanceof net.enchantedwood.entity.custom.ConvergenceSkeletonEntity ||
                    entity instanceof net.enchantedwood.entity.custom.ConvergenceSpiderEntity ||
                    entity instanceof net.enchantedwood.entity.custom.ConvergenceZombieEntity) {
                    if (sw.getRandom().nextFloat() < 0.035f) {
                        entity.spawnAtLocation(sw, new ItemStack(ModItems.MUSIC_DISC_ANOXIC));
                    }
                }
            }
        });
    }
}

