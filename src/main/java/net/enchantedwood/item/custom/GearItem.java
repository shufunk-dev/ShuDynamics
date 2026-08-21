package net.enchantedwood.item.custom;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.enchantedwood.block.custom.GearTier;

public class GearItem extends Item {
    private final GearTier gearTier;
    private final boolean enchanted;

    public GearItem(GearTier gearTier, boolean enchanted, Settings settings) {
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
    public boolean hasGlint(ItemStack stack) {
        return enchanted || super.hasGlint(stack);
    }
}
