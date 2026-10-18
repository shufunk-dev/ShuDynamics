package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record BoulderConfiguration(
    BlockStateProvider stateProvider,
    IntProvider radius
) {
    public static final Codec<BoulderConfiguration> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            BlockStateProvider.DIRECT_CODEC.fieldOf("state_provider").forGetter(BoulderConfiguration::stateProvider),
            IntProviders.codec(1, 10).fieldOf("radius").forGetter(BoulderConfiguration::radius)
        ).apply(instance, BoulderConfiguration::new)
    );
}
