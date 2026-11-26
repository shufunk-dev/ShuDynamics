package net.enchantedwood.block.custom;

import net.enchantedwood.item.ModItems;

public class StarfruitLeavesBlock extends FruitLeavesBlock {
    public StarfruitLeavesBlock(Properties settings) {
        super(settings, () -> ModItems.STARFRUIT);
    }
}
