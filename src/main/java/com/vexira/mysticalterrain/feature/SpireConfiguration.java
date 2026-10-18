package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record SpireConfiguration(
    BlockStateProvider core,
    BlockStateProvider shell,
    IntProvider height,
    IntProvider radius,
    boolean hanging
) {
    public static final Codec<SpireConfiguration> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            BlockStateProvider.DIRECT_CODEC.fieldOf("core").forGetter(SpireConfiguration::core),
            BlockStateProvider.DIRECT_CODEC.fieldOf("shell").forGetter(SpireConfiguration::shell),
            IntProviders.codec(3, 128).fieldOf("height").forGetter(SpireConfiguration::height),
            IntProviders.codec(1, 10).fieldOf("radius").forGetter(SpireConfiguration::radius),
            Codec.BOOL.optionalFieldOf("hanging", false).forGetter(SpireConfiguration::hanging)
        ).apply(instance, SpireConfiguration::new)
    );
}
