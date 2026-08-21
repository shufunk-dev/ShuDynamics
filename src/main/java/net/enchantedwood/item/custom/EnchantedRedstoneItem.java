package net.enchantedwood.item.custom;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class EnchantedRedstoneItem extends Item {
    public EnchantedRedstoneItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
