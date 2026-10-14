package net.enchantedwood.world.gen.mystical.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

public record BoulderConfiguration(
    BlockStateProvider stateProvider,
    IntProvider radius
) implements FeatureConfig {
    public static final Codec<BoulderConfiguration> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            BlockStateProvider.TYPE_CODEC.fieldOf("state_provider").forGetter(BoulderConfiguration::stateProvider),
            IntProvider.createValidatingCodec(1, 10).fieldOf("radius").forGetter(BoulderConfiguration::radius)
        ).apply(instance, BoulderConfiguration::new)
    );
}
