package net.enchantedwood.block.custom;

import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;

public class StarfruitLeavesBlock extends LeavesBlock {
    public StarfruitLeavesBlock(Properties settings) {
        super(AmbientLeavesBlockSoundPlayer.noAmbientSound(), settings);
    }
}
