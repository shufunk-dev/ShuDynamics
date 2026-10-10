package net.enchantedwood.item.custom;

import net.enchantedwood.block.custom.GearTier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class GearItem extends Item {
    private final GearTier gearTier;
    private final boolean enchanted;

    public GearItem(GearTier gearTier, boolean enchanted, Properties settings) {
        super(settings);
        this.gearTier = gearTier;
        this.enchanted = enchanted;
    }

    public GearTier getGearTier() {
        return gearTier;
    }

    public boolean isEnchanted() {
        return enchanted;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return enchanted || super.isFoil(stack);
    }
}
