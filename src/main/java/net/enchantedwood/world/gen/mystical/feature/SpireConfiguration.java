package net.enchantedwood.world.gen.mystical.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

public record SpireConfiguration(
    BlockStateProvider core,
    BlockStateProvider shell,
    IntProvider height,
    IntProvider radius,
    boolean hanging
) implements FeatureConfig {
    public static final Codec<SpireConfiguration> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            BlockStateProvider.TYPE_CODEC.fieldOf("core").forGetter(SpireConfiguration::core),
            BlockStateProvider.TYPE_CODEC.fieldOf("shell").forGetter(SpireConfiguration::shell),
            IntProvider.createValidatingCodec(3, 128).fieldOf("height").forGetter(SpireConfiguration::height),
            IntProvider.createValidatingCodec(1, 10).fieldOf("radius").forGetter(SpireConfiguration::radius),
            Codec.BOOL.optionalFieldOf("hanging", false).forGetter(SpireConfiguration::hanging)
        ).apply(instance, SpireConfiguration::new)
    );
}
