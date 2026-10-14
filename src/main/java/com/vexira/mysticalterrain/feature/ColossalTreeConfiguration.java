package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

public record ColossalTreeConfiguration(
   BlockStateProvider trunkProvider,
   BlockStateProvider foliageProvider,
   IntProvider height,
   IntProvider trunkRadius,
   IntProvider crownRadius,
   IntProvider branchCount
) implements FeatureConfig {
   public static final Codec<ColossalTreeConfiguration> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            BlockStateProvider.TYPE_CODEC.fieldOf("trunk_provider").forGetter(ColossalTreeConfiguration::trunkProvider),
            BlockStateProvider.TYPE_CODEC.fieldOf("foliage_provider").forGetter(ColossalTreeConfiguration::foliageProvider),
            IntProvider.createValidatingCodec(8, 120).fieldOf("height").forGetter(ColossalTreeConfiguration::height),
            IntProvider.createValidatingCodec(1, 3).fieldOf("trunk_radius").forGetter(ColossalTreeConfiguration::trunkRadius),
            IntProvider.createValidatingCodec(3, 13).fieldOf("crown_radius").forGetter(ColossalTreeConfiguration::crownRadius),
            IntProvider.createValidatingCodec(0, 16).fieldOf("branch_count").forGetter(ColossalTreeConfiguration::branchCount)
         )
         .apply(instance, ColossalTreeConfiguration::new)
   );
}
