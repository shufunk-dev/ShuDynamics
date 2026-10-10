package net.enchantedwood.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class EnchantedRedstoneItem extends Item {
    public EnchantedRedstoneItem(Properties settings) {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
