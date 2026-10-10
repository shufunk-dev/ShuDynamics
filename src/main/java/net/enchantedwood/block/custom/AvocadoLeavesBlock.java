package net.enchantedwood.block.custom;

import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;

public class AvocadoLeavesBlock extends LeavesBlock {
    public AvocadoLeavesBlock(Properties settings) {
        super(AmbientLeavesBlockSoundPlayer.noAmbientSound(), settings);
    }
}
