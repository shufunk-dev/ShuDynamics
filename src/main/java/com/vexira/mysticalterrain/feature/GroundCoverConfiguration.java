package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

public record GroundCoverConfiguration(
   RegistryKey<Biome> biome,
   RegistryEntryList<Block> replaceable,
   BlockStateProvider top,
   BlockStateProvider under,
   int depth,
   Optional<BlockStateProvider> accent,
   float accentScale,
   float accentThreshold
) implements FeatureConfig {
   public static final Codec<GroundCoverConfiguration> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            RegistryKey.createCodec(RegistryKeys.BIOME).fieldOf("biome").forGetter(GroundCoverConfiguration::biome),
            RegistryCodecs.entryList(RegistryKeys.BLOCK).fieldOf("replaceable").forGetter(GroundCoverConfiguration::replaceable),
            BlockStateProvider.TYPE_CODEC.fieldOf("top").forGetter(GroundCoverConfiguration::top),
            BlockStateProvider.TYPE_CODEC.fieldOf("under").forGetter(GroundCoverConfiguration::under),
            Codec.intRange(0, 8).optionalFieldOf("depth", 3).forGetter(GroundCoverConfiguration::depth),
            BlockStateProvider.TYPE_CODEC.optionalFieldOf("accent").forGetter(GroundCoverConfiguration::accent),
            Codec.floatRange(1.0F, 256.0F).optionalFieldOf("accent_scale", 12.0F).forGetter(GroundCoverConfiguration::accentScale),
            Codec.floatRange(-1.0F, 1.0F).optionalFieldOf("accent_threshold", 0.3F).forGetter(GroundCoverConfiguration::accentThreshold)
         )
         .apply(instance, GroundCoverConfiguration::new)
   );
}
