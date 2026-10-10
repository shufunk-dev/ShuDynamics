package net.enchantedwood.block.custom;

import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;

public class RubberLeavesBlock extends LeavesBlock {
    public RubberLeavesBlock(Properties settings) {
        super(AmbientLeavesBlockSoundPlayer.noAmbientSound(), settings);
    }
}
