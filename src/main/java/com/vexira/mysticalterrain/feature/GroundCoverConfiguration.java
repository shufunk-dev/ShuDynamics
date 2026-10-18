package com.vexira.mysticalterrain.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record GroundCoverConfiguration(
   ResourceKey<Biome> biome,
   HolderSet<Block> replaceable,
   BlockStateProvider top,
   BlockStateProvider under,
   int depth,
   Optional<BlockStateProvider> accent,
   float accentScale,
   float accentThreshold
) {
   public static final Codec<GroundCoverConfiguration> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            ResourceKey.codec(Registries.BIOME).fieldOf("biome").forGetter(GroundCoverConfiguration::biome),
            RegistryCodecs.holderSet(Registries.BLOCK).fieldOf("replaceable").forGetter(GroundCoverConfiguration::replaceable),
            BlockStateProvider.DIRECT_CODEC.fieldOf("top").forGetter(GroundCoverConfiguration::top),
            BlockStateProvider.DIRECT_CODEC.fieldOf("under").forGetter(GroundCoverConfiguration::under),
            Codec.intRange(0, 8).optionalFieldOf("depth", 3).forGetter(GroundCoverConfiguration::depth),
            BlockStateProvider.DIRECT_CODEC.optionalFieldOf("accent").forGetter(GroundCoverConfiguration::accent),
            Codec.floatRange(1.0F, 256.0F).optionalFieldOf("accent_scale", 12.0F).forGetter(GroundCoverConfiguration::accentScale),
            Codec.floatRange(-1.0F, 1.0F).optionalFieldOf("accent_threshold", 0.3F).forGetter(GroundCoverConfiguration::accentThreshold)
         )
         .apply(instance, GroundCoverConfiguration::new)
   );
}
