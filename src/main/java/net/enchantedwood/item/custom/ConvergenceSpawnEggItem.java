package net.enchantedwood.item.custom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.SpawnEggItem;

public class ConvergenceSpawnEggItem extends SpawnEggItem {
    public ConvergenceSpawnEggItem(EntityType<?> entityType, Properties settings) {
        super(settings.spawnEgg(entityType));
    }
}
