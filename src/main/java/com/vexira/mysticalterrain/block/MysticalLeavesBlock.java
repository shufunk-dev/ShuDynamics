package com.vexira.mysticalterrain.block;

import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;

public class MysticalLeavesBlock extends LeavesBlock {
    public MysticalLeavesBlock(float leafParticleChance, Properties settings) {
        super(AmbientLeavesBlockSoundPlayer.noAmbientSound(), settings);
    }

    public MysticalLeavesBlock(Properties settings) {
        this(0.01f, settings);
    }
}
