package net.enchantedwood.item.custom;

import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;

public class ConvergenceSpawnEggItem extends SpawnEggItem {
    private final EntityType<?> entityType;

    public ConvergenceSpawnEggItem(EntityType<?> entityType, Settings settings) {
        super(settings);
        this.entityType = entityType;
    }

    @Override
    public EntityType<?> getEntityType(ItemStack stack) {
        return this.entityType;
    }
}
