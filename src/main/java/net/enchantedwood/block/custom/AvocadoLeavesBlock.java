package net.enchantedwood.block.custom;

import net.enchantedwood.item.ModItems;

public class AvocadoLeavesBlock extends FruitLeavesBlock {
    public AvocadoLeavesBlock(Properties settings) {
        super(settings, () -> ModItems.AVOCADO);
    }
}
